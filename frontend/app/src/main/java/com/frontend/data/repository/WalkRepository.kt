package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.WalkApi
import com.frontend.domain.model.SaveLocationRequest
import com.frontend.domain.model.StartWalkRequest
import com.frontend.domain.model.WalkDetailResponse
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class WalkRepository @Inject constructor(
    private val walkApi: WalkApi,
    private val tokenDataStore: TokenDataStore,
) {

    /**
     * 산책 시작 API 호출.
     * TokenDataStore에서 accessToken·dogId를 직접 조회하므로 외부 주입 불필요.
     * @return Result<Long> — 성공 시 서버가 발급한 walkId, 실패 시 예외를 래핑
     */
    suspend fun startWalk(): Result<Long> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val dogId = tokenDataStore.getDogId().first()
            ?: throw Exception("강아지 정보가 없습니다")

        val response = walkApi.startWalk("Bearer $token", StartWalkRequest(dogId))
        response.data?.walkId
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

    /** 산책 상세 조회 (일기 + 사진 포함) */
    suspend fun getWalkDetail(walkId: Long): Result<WalkDetailResponse> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val response = walkApi.getWalkDetail("Bearer $token", walkId)
        response.data ?: throw Exception("산책 상세 조회 실패")
    }

    /** 사진 업로드 */
    suspend fun uploadPhotos(
        walkId: Long,
        parts: List<okhttp3.MultipartBody.Part>
    ): Result<List<String>> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val response = walkApi.uploadPhotos("Bearer $token", walkId, parts)
        response.data ?: throw Exception("사진 업로드 실패")
    }

    /** 사진 삭제 */
    suspend fun deletePhoto(walkId: Long, photoUrl: String): Result<List<String>> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val response = walkApi.deletePhoto("Bearer $token", walkId, mapOf("photoUrl" to photoUrl))
        response.data ?: throw Exception("사진 삭제 실패")
    }
}
