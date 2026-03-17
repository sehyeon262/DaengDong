package com.e108.be.domain.dog.service;

import com.e108.be.domain.dog.dto.request.RegisterDogRequest;
import com.e108.be.domain.dog.dto.request.UpdateDogRequest;
import com.e108.be.domain.dog.dto.request.UpdateTraitsRequest;
import com.e108.be.domain.dog.dto.request.UpdateWeightRequest;
import com.e108.be.domain.dog.dto.response.DogProfileResponse;
import com.e108.be.domain.dog.dto.response.RegisterDogResponse;
import com.e108.be.domain.dog.dto.response.UpdateDogResponse;
import com.e108.be.domain.dog.dto.response.UpdateTraitsResponse;
import com.e108.be.domain.dog.dto.response.UpdateWeightResponse;
import com.e108.be.domain.dog.entity.Dog;
import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.domain.dog.exception.DogNotFoundException;
import com.e108.be.domain.dog.exception.InvalidDogRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DogService {

    private final DogRepository dogRepository;

    // P1-01: 반려견 프로필 등록
    @Transactional
    public RegisterDogResponse registerDog(Long memberId, RegisterDogRequest request) {
        if (request.getWeight() != null && request.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDogRequestException("체중은 0보다 커야 합니다.");
        }

        Dog dog = Dog.builder()
                .userId(memberId)
                .name(request.getName())
                .breed(request.getBreed())
                .birthDate(request.getBirthDate())
                .weight(request.getWeight())
                .gender(request.getGender())
                .build();

        Dog saved = dogRepository.save(dog);
        return new RegisterDogResponse(saved.getId(), saved.getName());
    }

    // P1-02: 반려견 프로필 조회
    @Transactional(readOnly = true)
    public DogProfileResponse getDog(Long memberId, Long dogId) {
        Dog dog = dogRepository.findByIdAndUserId(dogId, memberId)
                .orElseThrow(() -> new DogNotFoundException());
        return new DogProfileResponse(dog);
    }

    // P1-03: 반려견 프로필 수정
    @Transactional
    public UpdateDogResponse updateDog(Long memberId, Long dogId, UpdateDogRequest request) {
        Dog dog = dogRepository.findByIdAndUserId(dogId, memberId)
                .orElseThrow(() -> new DogNotFoundException());
        dog.updateProfile(request.getName(), request.getBreed(), request.getBirthDate(), request.getGender());
        return new UpdateDogResponse(dog.getId());
    }

    // P1-04: 반려견 체중 등록/수정
    @Transactional
    public UpdateWeightResponse updateWeight(Long memberId, Long dogId, UpdateWeightRequest request) {
        if (request.getWeight() == null || request.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDogRequestException("체중은 0보다 커야 합니다.");
        }
        Dog dog = dogRepository.findByIdAndUserId(dogId, memberId)
                .orElseThrow(() -> new DogNotFoundException());
        dog.updateWeight(request.getWeight());
        return new UpdateWeightResponse(dog.getId(), dog.getWeight());
    }

    // P1-05: 반려견 성향 태그 등록/수정
    @Transactional
    public UpdateTraitsResponse updateTraits(Long memberId, Long dogId, UpdateTraitsRequest request) {
        if (request.getTraits() == null || request.getTraits().isEmpty()) {
            throw new InvalidDogRequestException("성향 태그는 하나 이상 입력해야 합니다.");
        }
        Dog dog = dogRepository.findByIdAndUserId(dogId, memberId)
                .orElseThrow(() -> new DogNotFoundException());
        dog.updateTraits(request.getTraits());
        return new UpdateTraitsResponse(dog.getId(), dog.getTraits());
    }
}
