package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.DogApi
import com.frontend.domain.model.DogProfileResponse
import com.frontend.domain.model.UpdateDogRequest
import com.frontend.domain.model.UpdateTraitsRequest
import com.frontend.domain.model.UpdateWeightRequest
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

    suspend fun updateDogProfile(dogId: Long, request: UpdateDogRequest): DogProfileResponse {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val response = dogApi.updateDogProfile("Bearer $token", dogId, request)
        return response.data ?: throw Exception("반려견 프로필 수정 실패")
    }

    suspend fun updateWeight(dogId: Long, weight: Double): DogProfileResponse {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val response = dogApi.updateWeight("Bearer $token", dogId, UpdateWeightRequest(weight))
        return response.data ?: throw Exception("체중 수정 실패")
    }

    suspend fun updateTraits(dogId: Long, traits: List<String>) {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        dogApi.updateTraits("Bearer $token", dogId, UpdateTraitsRequest(traits))
    }

    suspend fun fetchPublicDogProfile(dogId: Long): Result<DogProfileResponse> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val response = dogApi.getPublicDogProfile("Bearer $token", dogId)
        response.data ?: throw Exception("강아지 정보를 불러올 수 없습니다")
    }
}
