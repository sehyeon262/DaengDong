package com.frontend.domain.model

import com.google.gson.annotations.SerializedName

/** 카카오 로컬 키워드 검색 API 응답 */
data class KakaoSearchResponse(
    val documents: List<KakaoPlace>,
    val meta: KakaoMeta
)

data class KakaoPlace(
    @SerializedName("place_name") val placeName: String,
    @SerializedName("address_name") val addressName: String,
    @SerializedName("road_address_name") val roadAddressName: String,
    val x: String,  // 경도 (longitude)
    val y: String   // 위도 (latitude)
)

data class KakaoMeta(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("pageable_count") val pageableCount: Int,
    @SerializedName("is_end") val isEnd: Boolean
)

/** 카카오 좌표→주소 변환(역지오코딩) API 응답 */
data class KakaoCoord2AddressResponse(
    val documents: List<KakaoAddressDocument>
)

data class KakaoAddressDocument(
    val address: KakaoAddressItem?,
    @SerializedName("road_address") val roadAddress: KakaoRoadAddressItem?
)

data class KakaoAddressItem(
    @SerializedName("address_name") val addressName: String
)

data class KakaoRoadAddressItem(
    @SerializedName("address_name") val addressName: String
)
