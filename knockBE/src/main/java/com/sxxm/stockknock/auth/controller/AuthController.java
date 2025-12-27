/**
 * 사용자 인증 및 계정 관리 API. 회원가입, 로그인, 프로필 수정 등.
 */
package com.sxxm.stockknock.auth.controller;

import com.sxxm.stockknock.auth.dto.AuthRequest;
import com.sxxm.stockknock.auth.dto.AuthResponse;
import com.sxxm.stockknock.auth.dto.EmailChangeRequest;
import com.sxxm.stockknock.auth.dto.PasswordChangeRequest;
import com.sxxm.stockknock.auth.dto.UserDto;
import com.sxxm.stockknock.auth.dto.UserUpdateRequest;
import com.sxxm.stockknock.auth.service.UserService;
import com.sxxm.stockknock.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = userService.register(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 이메일 변경을 위한 인증 코드 전송
     */
    @PostMapping("/email/verification-code")
    public ResponseEntity<Map<String, String>> sendEmailVerificationCode(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).build();
            }
            
            token = token.substring(7);
            jwtUtil.getUserIdFromToken(token); // 토큰 유효성 검증
            
            String newEmail = request.get("newEmail");
            if (newEmail == null || newEmail.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            userService.sendEmailChangeVerificationCode(newEmail);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "인증 코드가 전송되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 이메일 변경
     */
    @PutMapping("/email")
    public ResponseEntity<Map<String, String>> changeEmail(
            @RequestBody EmailChangeRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).build();
            }
            
            token = token.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            userService.changeEmail(
                    userId,
                    request.getNewEmail(),
                    request.getVerificationCode(),
                    request.getPassword()
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "이메일이 변경되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 사용자 정보 조회
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).build();
            }
            
            token = token.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            UserDto userDto = userService.getUserById(userId);
            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 사용자 정보 수정 (아이디, 닉네임)
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody UserUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).build();
            }
            
            token = token.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            UserDto userDto = userService.updateUserProfile(userId, request);
            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 비밀번호 변경
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).build();
            }
            
            token = token.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            userService.changePassword(userId, request);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "비밀번호가 변경되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 아이디 변경
     */
    @PutMapping("/username")
    public ResponseEntity<?> changeUsername(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(401).build();
            }
            
            token = token.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            String newUsername = request.get("newUsername");
            String password = request.get("password");
            
            if (newUsername == null || password == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "새 아이디와 비밀번호를 입력해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            UserDto userDto = userService.changeUsername(userId, newUsername, password);
            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

