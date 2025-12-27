/**
 * 사용자 계정 관리 비즈니스 로직. 회원가입, 로그인, 프로필 수정, JWT 토큰 생성.
 */
package com.sxxm.stockknock.auth.service;

import com.sxxm.stockknock.auth.dto.AuthRequest;
import com.sxxm.stockknock.auth.dto.AuthResponse;
import com.sxxm.stockknock.auth.dto.PasswordChangeRequest;
import com.sxxm.stockknock.auth.dto.UserDto;
import com.sxxm.stockknock.auth.dto.UserUpdateRequest;
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

    /**
     * 사용자 정보 수정 (아이디, 닉네임)
     */
    public UserDto updateUserProfile(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 아이디 변경
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String newUsername = request.getUsername().trim();
            // 현재 아이디와 다르고, 이미 사용 중인 아이디인지 확인
            if (!user.getUsername().equals(newUsername) && userRepository.existsByUsername(newUsername)) {
                throw new RuntimeException("이미 사용 중인 아이디입니다.");
            }
            user.setUsername(newUsername);
        }
        
        // 닉네임 변경
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname().trim());
        }
        
        user = userRepository.save(user);
        return UserDto.from(user);
    }

    /**
     * 비밀번호 변경
     */
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        // 새 비밀번호 검증
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new RuntimeException("새 비밀번호를 입력해주세요.");
        }
        
        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("비밀번호는 최소 6자 이상이어야 합니다.");
        }
        
        // 비밀번호 변경
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * 아이디 변경
     */
    public UserDto changeUsername(Long userId, String newUsername, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        
        // 새 아이디 검증
        if (newUsername == null || newUsername.trim().isEmpty()) {
            throw new RuntimeException("새 아이디를 입력해주세요.");
        }
        
        newUsername = newUsername.trim();
        
        // 현재 아이디와 같은지 확인
        if (user.getUsername().equals(newUsername)) {
            throw new RuntimeException("현재 아이디와 동일합니다.");
        }
        
        // 이미 사용 중인 아이디인지 확인
        if (userRepository.existsByUsername(newUsername)) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }
        
        user.setUsername(newUsername);
        user = userRepository.save(user);
        return UserDto.from(user);
    }
}

