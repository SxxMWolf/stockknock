package com.sxxm.stockknock.auth.repository;

/**
 * 사용자 레포지토리
 * 
 * 역할:
 * - 사용자 엔티티의 데이터베이스 접근
 * - 이메일, 사용자명으로 사용자 조회
 * - 이메일, 사용자명 중복 체크
 */
import com.sxxm.stockknock.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}

