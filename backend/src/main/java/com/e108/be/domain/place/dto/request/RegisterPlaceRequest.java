package com.e108.be.domain.place.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 장소 등록 요청 DTO
 *
 * multipart/form-data로 전송되므로 @Setter 필요
 * (JSON @RequestBody와 달리 setter 기반 바인딩)
 */
@Getter
@Setter
public class RegisterPlaceRequest {

    @NotBlank(message = "장소 이름은 필수입니다.")
    private String name;

    private String categoryName;  // 카테고리 이름 (선택, 예: 카페, 동물병원)

    @NotNull(message = "위도는 필수입니다.")
    @Min(-90) @Max(90)
    private Double latitude;

    @NotNull(message = "경도는 필수입니다.")
    @Min(-180) @Max(180)
    private Double longitude;

    private String address;     // 주소 (선택)
    private String memo;        // 한줄 메모 (선택)
}
