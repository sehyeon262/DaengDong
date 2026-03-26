package com.e108.be.domain.walk.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProposalRequest {
    private Long fromWalkRecordId;
    private Long toWalkRecordId;
}
