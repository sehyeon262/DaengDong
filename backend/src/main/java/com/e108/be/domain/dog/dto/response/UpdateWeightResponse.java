package com.e108.be.domain.dog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class UpdateWeightResponse {
    private Long dogId;
    private BigDecimal weight;
}
