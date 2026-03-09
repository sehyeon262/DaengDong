package com.e108.be.domain.auth.entity;

/**
 * [Entity 패키지]
 * - DB 테이블과 1:1로 매핑되는 클래스를 모아두는 곳
 * - 이 클래스의 필드 = DB 컬럼
 * - 직접 외부에 노출하지 않고, DTO로 변환해서 사용한다
 */

import com.e108.be.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity                     // 이 클래스가 DB 테이블이라는 뜻
@Table(name = "members")    // 실제 테이블 이름
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자 (외부에서 new 못하게 막음)
public class Member extends BaseEntity { // BaseEntity 상속 → createdAt, updatedAt, isDeleted 자동 추가

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt로 암호화된 비밀번호가 저장됨

    @Column(nullable = false)
    private String nickname;

    @Builder // Member.builder().email("...").password("...").build() 이런 식으로 생성
    public Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
}
