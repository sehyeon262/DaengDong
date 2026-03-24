package com.e108.be.domain.walk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AcceptedProposalResponse {
    private String proposalId;
    private Long dogId;
    private String name;
    private String breed;
    private String profileImageUrl;
    private Long chatRoomId;
}
