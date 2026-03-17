package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.WalkApi
import com.frontend.domain.model.SaveLocationRequest
import com.frontend.domain.model.StartWalkRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class WalkRepository @Inject constructor(
    private val walkApi: WalkApi,
    private val tokenDataStore: TokenDataStore,
) {

    /**
     * 산책 시작 API 호출.
     * TokenDataStore에서 accessToken·dogId를 직접 조회하므로 외부 주입 불필요.
     * @return 서버가 발급한 walkId
     */
    suspend fun startWalk(): Long {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val dogId = tokenDataStore.getDogId().first()
            ?: throw Exception("강아지 정보가 없습니다")

        val response = walkApi.startWalk("Bearer $token", StartWalkRequest(dogId))
        return response.data?.walkId
            ?: throw Exception("산책 시작 실패: walkId가 없습니다")
    }

    /**
     * GPS 좌표 저장 API 호출.
     * 실패해도 산책을 중단하지 않으므로 Result로 반환.
     */
    suspend fun saveLocation(
        walkId: Long,
        latitude: Double,
        longitude: Double,
    ): Result<Unit> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")

        walkApi.saveLocation(
            authorization = "Bearer $token",
            walkId = walkId,
            request = SaveLocationRequest(latitude, longitude),
        )
    }
}
