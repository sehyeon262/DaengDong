package com.e108.be.domain.dog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterDogResponse {
    private Long dogId;
    private String name;
}
