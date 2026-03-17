package com.e108.be.domain.dog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UpdateTraitsResponse {
    private Long dogId;
    private List<String> traits;
}
