/**
 * [유틸리티 패키지]
 *
 * 프로젝트 전반에서 재사용하는 유틸리티 클래스를 모아두는 곳
 * (특정 비즈니스 로직이 아닌, 범용적인 헬퍼 기능들)
 *
 * 예시:
 *
 *   // S3 URL 생성 유틸
 *   public class S3UrlUtil {
 *       public static String generateUrl(String bucket, String key) { ... }
 *   }
 *
 *   // 날짜 포맷 유틸
 *   public class DateUtil {
 *       public static String format(LocalDateTime dateTime) { ... }
 *   }
 *
 * 주의:
 *   - Service에 넣기엔 애매한 공통 기능만 여기에
 *   - 비즈니스 로직은 Service에 작성
 */
package com.e108.be.global.common.util;
