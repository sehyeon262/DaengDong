package com.e108.be.domain.walk.dto.request;

import com.e108.be.domain.walk.entity.ProposalAction;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProposalRespondRequest {
    private ProposalAction action;  // ACCEPT | REJECT
    private Long myWalkRecordId;
}
