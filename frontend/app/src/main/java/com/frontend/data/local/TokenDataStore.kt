package com.frontend.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TokenDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    // 토큰 저장
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    // 액세스 토큰 가져오기
    fun getAccessToken(): Flow<String?> {
        return dataStore.data.map { prefs -> prefs[ACCESS_TOKEN_KEY] }
    }

    // dogId 저장
    suspend fun saveDogId(dogId: Long) {
        dataStore.edit { prefs ->
            prefs[DOG_ID_KEY] = dogId
        }
    }

    // dogId 가져오기
    fun getDogId(): Flow<Long?> {
        return dataStore.data.map { prefs -> prefs[DOG_ID_KEY] }
    }

    // 토큰 삭제 (로그아웃때)
    suspend fun clearTokens() {
        dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
            prefs.remove(DOG_ID_KEY)
        }
    }

    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val DOG_ID_KEY = longPreferencesKey("dog_id")
    }
}
