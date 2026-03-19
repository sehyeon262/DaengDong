package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.WalkApi
import com.frontend.domain.model.LocationBatchRequest
import com.frontend.domain.model.StartWalkRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class WalkRepository @Inject constructor(
    private val walkApi: WalkApi,
    private val tokenDataStore: TokenDataStore,
) {

    /**
     * W1-01: 산책 시작 API 호출 (추천 코스용)
     * @return Result<Long> — 성공 시 walkId
     */
    suspend fun startWalk(): Result<Long> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val dogId = tokenDataStore.getDogId().first()
            ?: 1L // TODO: 로그인 우회 (원래: ?: throw Exception("강아지 정보가 없습니다"))

        val response = walkApi.startWalk("Bearer $token", StartWalkRequest(dogId))
        response.data?.walkId
            ?: throw Exception("산책 시작 실패: walkId가 없습니다")
    }

    /**
     * R1-03: 자유 산책 시작 API 호출
     * @return Result<Long> — 성공 시 walkId
     */
    suspend fun startFreeWalk(): Result<Long> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val dogId = tokenDataStore.getDogId().first()
        // TODO: 로그인 우회 (원래: ?: throw Exception("강아지 정보가 없습니다"))
            ?: throw Exception("강아지 정보가 없습니다")

        val response = walkApi.startFreeWalk("Bearer $token", StartWalkRequest(dogId))
        response.data?.walkId
            ?: throw Exception("자유 산책 시작 실패: walkId가 없습니다")
    }

    /**
     * W1-02: GPS 좌표 배치 저장 API 호출
     * 실패해도 산책을 중단하지 않으므로 Result로 반환
     */
    suspend fun saveLocations(
        walkId: Long,
        points: List<LocationBatchRequest.LocationPoint>,
    ): Result<Unit> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")

        walkApi.saveLocations(
            authorization = "Bearer $token",
            walkId = walkId,
            request = LocationBatchRequest(points),
        )
    }

    /**
     * W1-06: 산책 종료 API 호출
     * @return Result<Unit>
     */
    suspend fun endWalk(walkId: Long): Result<Unit> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")

        walkApi.endWalk(
            authorization = "Bearer $token",
            walkId = walkId,
        )
    }
}
