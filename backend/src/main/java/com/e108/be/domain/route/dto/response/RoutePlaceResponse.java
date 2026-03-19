package com.e108.be.domain.route.dto.response;

import com.e108.be.domain.place.repository.NearbyPlaceProjection;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoutePlaceResponse {

    private Long id;
    private String name;
    private String categoryName;
    private double latitude;
    private double longitude;
    private double distanceMeters;
    private String address;
    private String imageUrl;

    public static RoutePlaceResponse from(NearbyPlaceProjection p) {
        return RoutePlaceResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .categoryName(p.getCategoryName())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .distanceMeters(p.getDistanceMeters())
                .address(p.getAddress())
                .imageUrl(p.getImageUrl())
                .build();
    }
}
