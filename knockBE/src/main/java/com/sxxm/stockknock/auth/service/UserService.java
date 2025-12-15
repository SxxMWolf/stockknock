package com.sxxm.stockknock.auth.service;

import com.sxxm.stockknock.auth.dto.AuthRequest;
import com.sxxm.stockknock.auth.dto.AuthResponse;
import com.sxxm.stockknock.auth.dto.UserDto;
import com.sxxm.stockknock.auth.entity.User;
import com.sxxm.stockknock.auth.repository.UserRepository;
import com.sxxm.stockknock.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailVerificationService emailVerificationService;

    public AuthResponse register(AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("아이디를 입력해주세요.");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("이메일을 입력해주세요.");
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 등록된 이메일입니다.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getNickname())
                .userId(user.getId())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("아이디를 입력해주세요.");
        }
        
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            throw new RuntimeException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        User user = userOpt.get();
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getNickname())
                .userId(user.getId())
                .build();
    }

    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return UserDto.from(user);
    }

    public UserDto updateUser(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        if (nickname != null) user.setNickname(nickname);
        
        user = userRepository.save(user);
        return UserDto.from(user);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    public User getUserEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 새 이메일로 인증 코드 전송
     */
    public void sendEmailChangeVerificationCode(String newEmail) {
        // 이미 사용 중인 이메일인지 확인
        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }
        
        // 인증 코드 전송
        emailVerificationService.sendVerificationCode(newEmail);
    }

    /**
     * 이메일 변경 (인증 코드 확인 및 비밀번호 확인 후)
     */
    public void changeEmail(Long userId, String newEmail, String verificationCode, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        
        // 이미 사용 중인 이메일인지 확인
        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }
        
        // 인증 코드 확인
        if (!emailVerificationService.verifyCode(newEmail, verificationCode)) {
            throw new RuntimeException("인증 코드가 일치하지 않거나 만료되었습니다.");
        }
        
        // 이메일 변경
        user.setEmail(newEmail);
        userRepository.save(user);
    }
}

