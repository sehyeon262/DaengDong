package com.e108.be.domain.walk.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProposalRespondRequest {
    private String action;       // "ACCEPT" | "REJECT"
    private Long myWalkRecordId;
}
