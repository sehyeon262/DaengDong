package com.e108.be.domain.auth.repository;

/**
 * [Repository 패키지]
 * - DB에 접근하는 계층 (SQL 대신 메서드로 DB 조회/저장/삭제)
 * - JpaRepository를 상속받으면 기본 CRUD(save, findById, delete 등)가 자동 생성됨
 * - 메서드 이름 규칙으로 쿼리 자동 생성: findByEmail → SELECT * FROM members WHERE email = ?
 */

import com.e108.be.domain.auth.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 이메일로 회원 조회 (로그인 시 사용)
    Optional<Member> findByEmail(String email);

    // 이메일 중복 체크 (회원가입 시 사용)
    boolean existsByEmail(String email);
}
