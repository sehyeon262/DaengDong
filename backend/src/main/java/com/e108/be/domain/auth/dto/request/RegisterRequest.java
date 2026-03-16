package com.e108.be.domain.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterRequest {

    private String email;
    private String password;
    private String nickname;
    private String phone;
}
