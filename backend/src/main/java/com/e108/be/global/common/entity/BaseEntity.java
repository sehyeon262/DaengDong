package com.e108.be.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * [JPA 공통 엔티티]
 * 모든 Entity가 상속받는 베이스 클래스
 *
 * 상속하면 자동으로 아래 3개 컬럼이 추가됨:
 * - createdAt  : 생성일 (INSERT 시 자동 저장)
 * - updatedAt  : 수정일 (UPDATE 시 자동 갱신)
 * - isDeleted  : 삭제 여부 (Soft Delete용, 실제 DB에서 안 지움)
 *
 * 사용법:
 *   @Entity
 *   public class Member extends BaseEntity { ... }
 */
@Getter
@MappedSuperclass // 이 클래스 자체는 테이블이 아니고, 상속받는 엔티티에 컬럼을 물려줌
@EntityListeners(AuditingEntityListener.class) // createdAt, updatedAt 자동 관리
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false) // 생성일은 수정 불가
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean isDeleted = false;

    // Soft Delete: 실제로 DB에서 지우지 않고 isDeleted = true로 표시
    public void delete() {
        this.isDeleted = true;
    }
}
