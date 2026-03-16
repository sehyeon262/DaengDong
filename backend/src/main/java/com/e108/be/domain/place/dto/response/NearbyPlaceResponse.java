package com.e108.be.domain.place.dto.response;

import com.e108.be.domain.place.repository.NearbyPlaceProjection;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NearbyPlaceResponse {

    private Long id;
    private String name;
    private String categoryName;
    private double latitude;
    private double longitude;
    private double distanceMeters;
    private String address;
    private String contact;
    private String imageUrl;

    public static NearbyPlaceResponse from(NearbyPlaceProjection p) {
        return NearbyPlaceResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .categoryName(p.getCategoryName())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .distanceMeters(p.getDistanceMeters())
                .address(p.getAddress())
                .contact(p.getContact())
                .imageUrl(p.getImageUrl())
                .build();
    }
}
