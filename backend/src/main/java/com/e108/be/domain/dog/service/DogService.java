package com.e108.be.domain.dog.service;

import com.e108.be.domain.auth.entity.User;
import com.e108.be.domain.auth.repository.UserRepository;
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
import com.e108.be.domain.dog.entity.PersonalityTag;
import com.e108.be.domain.dog.repository.DogRepository;
import com.e108.be.domain.dog.repository.PersonalityTagRepository;
import com.e108.be.domain.dog.exception.DogNotFoundException;
import com.e108.be.domain.dog.exception.InvalidDogRequestException;
import com.e108.be.domain.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DogService {

    private final DogRepository dogRepository;
    private final PersonalityTagRepository personalityTagRepository;
    private final UserRepository userRepository;

    // P1-01: 반려견 프로필 등록
    @Transactional
    public RegisterDogResponse registerDog(Long memberId, RegisterDogRequest request) {
        if (request.getWeight() != null && request.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDogRequestException("체중은 0보다 커야 합니다.");
        }

        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new AuthException("사용자를 찾을 수 없습니다."));

        Dog dog = Dog.builder()
                .user(user)
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
        Dog dog = dogRepository.findByIdAndUser_Id(dogId, memberId)
                .orElseThrow(() -> new DogNotFoundException());
        return new DogProfileResponse(dog);
    }

    // P1-03: 반려견 프로필 수정
    @Transactional
    public UpdateDogResponse updateDog(Long memberId, Long dogId, UpdateDogRequest request) {
        Dog dog = dogRepository.findByIdAndUser_Id(dogId, memberId)
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
        Dog dog = dogRepository.findByIdAndUser_Id(dogId, memberId)
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
        Dog dog = dogRepository.findByIdAndUser_Id(dogId, memberId)
                .orElseThrow(() -> new DogNotFoundException());

        List<PersonalityTag> tags = request.getTraits().stream()
                .map(tagName -> personalityTagRepository.findByTagName(tagName)
                        .orElseGet(() -> personalityTagRepository.save(
                                PersonalityTag.builder().tagName(tagName).build())))
                .toList();

        dog.updateTraits(tags);
        return new UpdateTraitsResponse(dog.getId(), dog.getTraitNames());
    }
}
