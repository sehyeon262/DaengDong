package com.e108.be.domain.dog.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UpdateDogRequest {
    private String name;
    private String breed;
    private LocalDate birthDate;
    private String gender;
}
