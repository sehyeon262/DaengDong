package com.e108.be.domain.dog.controller;

import com.e108.be.domain.dog.dto.request.RegisterDogRequest;
import com.e108.be.domain.dog.dto.request.UpdateDogRequest;
import com.e108.be.domain.dog.dto.request.UpdateTraitsRequest;
import com.e108.be.domain.dog.dto.request.UpdateWeightRequest;
import com.e108.be.domain.dog.dto.response.DogProfileResponse;
import com.e108.be.domain.dog.dto.response.RegisterDogResponse;
import com.e108.be.domain.dog.dto.response.UpdateDogResponse;
import com.e108.be.domain.dog.dto.response.UpdateTraitsResponse;
import com.e108.be.domain.dog.dto.response.UpdateWeightResponse;
import com.e108.be.domain.dog.service.DogService;
import com.e108.be.global.common.template.ResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dogs")
@RequiredArgsConstructor
public class DogController {

    private final DogService dogService;

    // P1-01: 반려견 프로필 등록 - POST /api/v1/dogs
    @PostMapping
    public ResTemplate<RegisterDogResponse> registerDog(
            @AuthenticationPrincipal Long memberId,
            @RequestBody RegisterDogRequest request) {
        RegisterDogResponse response = dogService.registerDog(memberId, request);
        return ResTemplate.success(HttpStatus.CREATED, "반려견 프로필 등록 성공", response);
    }

    // P1-02: 반려견 프로필 조회 - GET /api/v1/dogs/{dogId}
    @GetMapping("/{dogId}")
    public ResTemplate<DogProfileResponse> getDog(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long dogId) {
        DogProfileResponse response = dogService.getDog(memberId, dogId);
        return ResTemplate.success(HttpStatus.OK, "반려견 프로필 조회 성공", response);
    }

    // S14P21E108-169: 반려견 공개 프로필 조회 - GET /api/v1/dogs/{dogId}/public
    @GetMapping("/{dogId}/public")
    public ResTemplate<DogProfileResponse> getPublicDog(
            @PathVariable Long dogId) {
        DogProfileResponse response = dogService.getPublicDog(dogId);
        return ResTemplate.success(HttpStatus.OK, "반려견 공개 프로필 조회 성공", response);
    }

    // P1-03: 반려견 프로필 수정 - PATCH /api/v1/dogs/{dogId}
    @PatchMapping("/{dogId}")
    public ResTemplate<UpdateDogResponse> updateDog(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long dogId,
            @RequestBody UpdateDogRequest request) {
        UpdateDogResponse response = dogService.updateDog(memberId, dogId, request);
        return ResTemplate.success(HttpStatus.OK, "반려견 프로필 수정 성공", response);
    }

    // P1-04: 반려견 체중 등록/수정 - PATCH /api/v1/dogs/{dogId}/weight
    @PatchMapping("/{dogId}/weight")
    public ResTemplate<UpdateWeightResponse> updateWeight(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long dogId,
            @RequestBody UpdateWeightRequest request) {
        UpdateWeightResponse response = dogService.updateWeight(memberId, dogId, request);
        return ResTemplate.success(HttpStatus.OK, "반려견 체중 업데이트 성공", response);
    }

    // P1-05: 반려견 성향 태그 등록/수정 - PATCH /api/v1/dogs/{dogId}/traits
    @PatchMapping("/{dogId}/traits")
    public ResTemplate<UpdateTraitsResponse> updateTraits(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long dogId,
            @RequestBody UpdateTraitsRequest request) {
        UpdateTraitsResponse response = dogService.updateTraits(memberId, dogId, request);
        return ResTemplate.success(HttpStatus.OK, "반려견 성향 태그 업데이트 성공", response);
    }
}
