package com.frontend.data.repository

import android.content.Context
import android.net.Uri
import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.PlaceApi
import com.frontend.domain.model.Place
import com.frontend.domain.model.PlaceCategory
import com.frontend.domain.model.RegisterPlaceResponse
import com.frontend.domain.model.StampRequest
import com.frontend.domain.model.UnauthorizedException
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class PlaceRepository constructor(
    private val placeApi: PlaceApi,
    private val tokenDataStore: TokenDataStore
) {

    /** 현재 위치 기반 주변 장소 목록 조회 */
    suspend fun getPlacesNearby(
        latitude: Double,
        longitude: Double,
        radius: Double? = null,
        limit: Int? = null,
        category: String? = null
    ): Result<List<Place>> = runCatching {
        val token = tokenDataStore.getAccessToken().first() ?: error("인증 토큰 없음")
        placeApi.getPlacesNearby(
            authorization = "Bearer $token",
            lat = latitude,
            lon = longitude,
            radius = radius,
            limit = limit,
            category = category
        ).data ?: emptyList()
    }

    /** 장소 상세정보 조회 */
    suspend fun getPlaceDetail(placeId: Long): Result<Place> = runCatching {
        val token = tokenDataStore.getAccessToken().first() ?: error("인증 토큰 없음")
        placeApi.getPlaceDetail(
            authorization = "Bearer $token",
            placeId = placeId
        ).data ?: error("장소 데이터 없음")
    }

    /** 장소 카테고리 목록 조회 */
    suspend fun getPlaceCategories(): Result<List<PlaceCategory>> = runCatching {
        val token = tokenDataStore.getAccessToken().first() ?: error("인증 토큰 없음")
        placeApi.getPlaceCategories(
            authorization = "Bearer $token"
        ).data ?: emptyList()
    }

    /** 발자국 도장 찍기 */
    suspend fun sendStamp(walkId: Long, dogId: Long, placeId: Long): Result<Unit> {
        val token = tokenDataStore.getAccessToken().first()
            ?: return Result.failure(UnauthorizedException("인증 토큰 없음"))
        return runCatching {
            placeApi.sendStamp(
                authorization = "Bearer $token",
                request = StampRequest(walkId = walkId, dogId = dogId, placeId = placeId)
            )
            Unit
        }
    }

    /** 발자국 도장 찍은 장소 목록 조회 */
    suspend fun getFootprintPlaces(dogId: Long): Result<List<Place>> = runCatching {
        val token = tokenDataStore.getAccessToken().first() ?: error("인증 토큰 없음")
        placeApi.getFootprintPlaces(
            authorization = "Bearer $token",
            dogId = dogId
        ).data ?: emptyList()
    }

    /** 발자국 도장 찍기 — POST /maps/stamps */
    suspend fun stampPlace(walkId: Long, dogId: Long, placeId: Long): Result<Unit> = runCatching {
        val token = tokenDataStore.getAccessToken().first() ?: error("인증 토큰 없음")
        placeApi.stampPlace(
            authorization = "Bearer $token",
            request = StampRequest(walkId, dogId, placeId)
        )
    }

    /** 발자국 도장 취소 — DELETE /maps/stamps */
    suspend fun cancelStamp(dogId: Long, placeId: Long): Result<Unit> = runCatching {
        val token = tokenDataStore.getAccessToken().first() ?: error("인증 토큰 없음")
        placeApi.cancelStamp(
            authorization = "Bearer $token",
            dogId = dogId,
            placeId = placeId
        )
    }

    /** 신규 장소 등록 — POST /places (multipart/form-data)
     *  @param context  Uri → InputStream 변환에 필요
     *  @param imageUri 사용자가 선택한 이미지 Uri (null이면 이미지 없이 등록)
     */
    suspend fun registerPlace(
        context: Context,
        name: String,
        categoryName: String?,
        latitude: Double,
        longitude: Double,
        address: String?,
        memo: String?,
        imageUri: Uri?
    ): Result<RegisterPlaceResponse> = runCatching {
        val token = tokenDataStore.getAccessToken().first() ?: error("인증 토큰 없음")
        val textType = "text/plain".toMediaTypeOrNull()

        val imagePart = imageUri?.let { uri ->
            val stream = context.contentResolver.openInputStream(uri) ?: error("이미지를 열 수 없습니다")
            val bytes = stream.use { it.readBytes() }
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", "place_image.jpg", requestBody)
        }

        placeApi.registerPlace(
            authorization = "Bearer $token",
            name = name.toRequestBody(textType),
            latitude = latitude.toString().toRequestBody(textType),
            longitude = longitude.toString().toRequestBody(textType),
            categoryName = categoryName?.toRequestBody(textType),
            address = address?.toRequestBody(textType),
            memo = memo?.toRequestBody(textType),
            image = imagePart
        ).data ?: error("장소 등록 응답 데이터 없음")
    }
}
