package com.e108.be.domain.walk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NearbyDogResponse {
    private Long dogId;
    private String name;
    private String breed;
    private String profileImageUrl;
    private double latitude;
    private double longitude;
    private double distanceM;
    private Long walkRecordId;
}
