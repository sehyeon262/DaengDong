package com.e108.be.domain.home.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeWalkInfo {

    private String walkStatus;
    private String walkMessage;
    private String characterType;
}
