package com.e108.be.domain.place.dto.response;

import com.e108.be.domain.place.entity.Place;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterPlaceResponse {

    private Long id;
    private String name;
    private String imageUrl;

    public static RegisterPlaceResponse from(Place place) {
        return RegisterPlaceResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .imageUrl(place.getImageUrl())
                .build();
    }
}
