package com.frontend.domain.usecase

import com.frontend.data.repository.WalkRepository
import javax.inject.Inject

class EndWalkUseCase @Inject constructor(
    private val repository: WalkRepository
) {
    suspend operator fun invoke(walkId: Long): Result<Unit> {
        return repository.endWalk(walkId)
    }
}
