package com.frontend.domain.usecase

import com.frontend.data.repository.AuthRepository
import com.frontend.domain.model.LoginResponse
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(
        email: String,
        password: String
    ): LoginResponse {
        return repository.login(email, password)
    }
}
