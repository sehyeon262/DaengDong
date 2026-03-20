package com.frontend.domain.usecase

import com.frontend.data.repository.WalkRepository
import javax.inject.Inject

class StartFreeWalkUseCase @Inject constructor(
    private val repository: WalkRepository
) {
    suspend operator fun invoke(): Result<Long> {
        return repository.startFreeWalk()
    }
}
