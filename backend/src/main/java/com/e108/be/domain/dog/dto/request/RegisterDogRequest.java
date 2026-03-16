package com.e108.be.domain.dog.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class RegisterDogRequest {
    private String name;
    private String breed;
    private LocalDate birthDate;
    private BigDecimal weight;
    private String gender;
}
