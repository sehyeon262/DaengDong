package com.e108.be.domain.home.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeUserInfo {

    private String nickname;
    private String dogName;
    private String dogProfileImageUrl;
}
