package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.DogApi
import com.frontend.domain.model.DogProfileResponse
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DogRepository @Inject constructor(
    private val dogApi: DogApi,
    private val tokenDataStore: TokenDataStore
) {

    suspend fun getDogProfile(dogId: Long): DogProfileResponse {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val response = dogApi.getDogProfile("Bearer $token", dogId)
        return response.data ?: throw Exception("반려견 프로필 데이터가 없습니다")
    }
}
