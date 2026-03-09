/**
 * [공통 Enum 패키지]
 *
 * 여러 도메인에서 공유하는 Enum을 모아두는 곳
 * (한 도메인에서만 쓰는 Enum은 해당 도메인 패키지에 둔다)
 *
 * 예시:
 *
 *   // 사용자 역할 (auth, admin 등 여러 도메인에서 사용)
 *   public enum Role {
 *       USER, ADMIN
 *   }
 *
 *   // 공통 상태값
 *   public enum Status {
 *       ACTIVE, INACTIVE, DELETED
 *   }
 *
 * 추가 기준:
 *   - 2개 이상 도메인에서 쓰면 → 여기에
 *   - 1개 도메인에서만 쓰면 → 해당 도메인 패키지에
 */
package com.e108.be.global.common.enums;
