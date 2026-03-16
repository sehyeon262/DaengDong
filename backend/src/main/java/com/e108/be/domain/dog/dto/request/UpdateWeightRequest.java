package com.e108.be.domain.dog.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class UpdateWeightRequest {
    private BigDecimal weight;
}
