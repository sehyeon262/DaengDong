package com.frontend.domain.usecase

import com.frontend.data.repository.WalkRepository
import com.frontend.domain.model.NewBadgeInfo
import javax.inject.Inject

class EndWalkUseCase @Inject constructor(
    private val repository: WalkRepository
) {
    suspend operator fun invoke(walkId: Long): Result<List<NewBadgeInfo>> {
        return repository.endWalk(walkId)
    }
}
