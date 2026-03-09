package com.e108.be.global.common.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * [통일된 API 응답 형식]
 * 모든 API가 동일한 JSON 구조로 응답하도록 감싸주는 템플릿
 *
 * 성공 응답 예시:
 * { "code": 200, "message": "조회 성공", "data": { ... } }
 *
 * 에러 응답 예시 (data가 null이면 아예 안 보임):
 * { "code": 404, "message": "회원을 찾을 수 없습니다." }
 */
@Getter
@JsonPropertyOrder({"code", "message", "data"}) // JSON 필드 순서 고정
public class ResTemplate<T> {

    private final int code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL) // data가 null이면 JSON에서 제외
    private final T data;

    public ResTemplate(HttpStatus httpStatus, String message, T data) {
        this.code = httpStatus.value();
        this.message = message;
        this.data = data;
    }

    // 성공 응답 (데이터 + 메시지)
    public static <T> ResTemplate<T> success(HttpStatus status, String message, T data) {
        return new ResTemplate<>(status, message, data);
    }

    // 성공 응답 (데이터 없음)
    public static ResTemplate<Void> success(HttpStatus status, String message) {
        return new ResTemplate<>(status, message, null);
    }

    // 에러 응답
    public static <T> ResTemplate<T> error(HttpStatus status, String message) {
        return new ResTemplate<>(status, message, null);
    }
}
