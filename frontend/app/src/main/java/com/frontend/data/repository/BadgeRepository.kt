package com.frontend.data.repository

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.BadgeApi
import com.frontend.domain.model.BadgeProgressResponse
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class BadgeRepository @Inject constructor(
    private val badgeApi: BadgeApi,
    private val tokenDataStore: TokenDataStore
) {

    suspend fun getBadgeProgress(): List<BadgeProgressResponse> {
        val token = tokenDataStore.getAccessToken().first()
            ?: throw Exception("로그인이 필요합니다")
        val response = badgeApi.getBadgeProgress("Bearer $token")
        return response.data ?: throw Exception("배지 데이터를 불러올 수 없습니다")
    }
}
