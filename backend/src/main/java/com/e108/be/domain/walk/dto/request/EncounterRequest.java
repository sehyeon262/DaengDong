package com.e108.be.domain.walk.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class EncounterRequest {
    private List<EncounterItem> encounters;

    @Getter
    @NoArgsConstructor
    public static class EncounterItem {
        private Long targetWalkRecordId;
    }
}
