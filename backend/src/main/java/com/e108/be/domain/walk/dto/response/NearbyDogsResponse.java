package com.e108.be.domain.walk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NearbyDogsResponse {
    private List<NearbyDogResponse> nearbyDogs;
    private List<PendingProposalResponse> pendingProposals;
}
