package com.e108.be.domain.dog.dto.response;

import com.e108.be.domain.dog.entity.Dog;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class DogProfileResponse {
    private Long dogId;
    private String name;
    private String breed;
    private LocalDate birthDate;
    private BigDecimal weight;
    private String gender;

    public DogProfileResponse(Dog dog) {
        this.dogId = dog.getId();
        this.name = dog.getName();
        this.breed = dog.getBreed();
        this.birthDate = dog.getBirthDate();
        this.weight = dog.getWeight();
        this.gender = dog.getGender();
    }
}
