package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.WalkApi
import com.frontend.domain.model.FeedbackRequest
import com.frontend.domain.model.MetDogResponse
import com.frontend.domain.model.SaveLocationRequest
import com.frontend.domain.model.LocationBatchRequest
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
            ?: 1L
            //?: throw Exception("강아지 정보가 없습니다")
        //우회 : (?: 1L) // TODO: 로그인 우회 (원래: ?: throw Exception("강아지 정보가 없습니다"))

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

    /** 만난 친구 목록 조회 */
    suspend fun getMetDogs(dogId: Long): Result<List<MetDogResponse>> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val response = walkApi.getMetDogs("Bearer $token", dogId)
        response.data ?: throw Exception("만난 친구 목록 조회 실패")
    }

    /** 궁합 피드백 설정/수정 */
    suspend fun updateFeedback(request: FeedbackRequest): Result<Unit> = runCatching {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        walkApi.updateFeedback("Bearer $token", request)
    }
}
