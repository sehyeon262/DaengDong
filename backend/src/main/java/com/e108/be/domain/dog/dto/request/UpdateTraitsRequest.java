package com.e108.be.domain.dog.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UpdateTraitsRequest {
    private List<String> traits;
}
