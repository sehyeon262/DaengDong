package com.e108.be.domain.maps.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * M3-03 POST /api/v1/maps/stamps 요청 DTO
 * FE에서 사용자가 장소 근처(50m)에서 발자국 터치 시 호출
 */
@Getter
@NoArgsConstructor
public class StampRequest {

    @NotNull
    private Long walkId;

    @NotNull
    private Long dogId;

    @NotNull
    private Long placeId;
}
