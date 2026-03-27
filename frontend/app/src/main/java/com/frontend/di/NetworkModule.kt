package com.frontend.di

import com.frontend.BuildConfig
import com.frontend.data.local.TokenDataStore
import com.frontend.data.local.UnauthorizedEventBus
import com.frontend.data.remote.AuthApi
import com.frontend.data.remote.BadgeApi
import com.frontend.data.remote.ChatApi
import com.frontend.data.remote.DangerZoneApi
import com.frontend.data.remote.DogApi
import com.frontend.data.remote.HomeApi
import com.frontend.data.remote.KakaoLocalApi
import com.frontend.data.remote.PlaceApi
import com.frontend.data.remote.RecordApi
import com.frontend.data.remote.RouteApi
import com.frontend.data.remote.WalkApi
import com.frontend.util.Constants
import javax.inject.Qualifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** 카카오 로컬 API 전용 Retrofit 인스턴스를 구분하기 위한 Qualifier */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KakaoRetrofit

/** Auth 전용 OkHttpClient — TokenAuthenticator 미포함 (순환의존 방지) */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthOkHttp

/** Auth 전용 Retrofit — AuthApi 주입에 사용 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ─────────────────────────────────────────────────────────────────
    // Auth 전용 OkHttpClient / Retrofit
    // TokenAuthenticator 가 AuthApi 를 필요로 하기 때문에,
    // AuthApi 는 Authenticator 없는 별도 클라이언트로 만들어 순환의존을 방지
    // ─────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @AuthOkHttp
    fun provideAuthOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
                        else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(@AuthOkHttp okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(@AuthRetrofit retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    // ─────────────────────────────────────────────────────────────────
    // 메인 OkHttpClient / Retrofit
    // AuthInterceptor(토큰 자동 삽입) + TokenAuthenticator(401 처리) 포함
    // ─────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenDataStore: TokenDataStore,
        unauthorizedEventBus: UnauthorizedEventBus
    ): Interceptor {
        return Interceptor { chain ->
            val token = runBlocking { tokenDataStore.getAccessToken().first() }
            val request = if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            val response = chain.proceed(request)
            // 401: 인증 실패 (토큰 만료/무효) — 주된 처리 대상
            // 403: 백엔드 authenticationEntryPoint 미적용 시 방어용
            if (response.code == 401 || response.code == 403) {
                unauthorizedEventBus.emit()
            }
            response
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
                        else HttpLoggingInterceptor.Level.NONE
            })
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)  // 사진 업로드를 위해 넉넉하게
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideHomeApi(retrofit: Retrofit): HomeApi {
        return retrofit.create(HomeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDogApi(retrofit: Retrofit): DogApi {
        return retrofit.create(DogApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRecordApi(retrofit: Retrofit): RecordApi {
        return retrofit.create(RecordApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWalkApi(retrofit: Retrofit): WalkApi {
        return retrofit.create(WalkApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDangerZoneApi(retrofit: Retrofit): DangerZoneApi {
        return retrofit.create(DangerZoneApi::class.java)
    }

    @Provides
    @Singleton
    fun providePlaceApi(retrofit: Retrofit): PlaceApi {
        return retrofit.create(PlaceApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRouteApi(retrofit: Retrofit): RouteApi {
        return retrofit.create(RouteApi::class.java)
    }

    @Provides
    @Singleton
    fun provideBadgeApi(retrofit: Retrofit): BadgeApi {
        return retrofit.create(BadgeApi::class.java)
    }

    /** 카카오 로컬 API 전용 Retrofit (baseUrl: https://dapi.kakao.com/) */
    @Provides
    @Singleton
    @KakaoRetrofit
    fun provideKakaoRetrofit(@AuthOkHttp okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideKakaoLocalApi(@KakaoRetrofit retrofit: Retrofit): KakaoLocalApi {
        return retrofit.create(KakaoLocalApi::class.java)
    }

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi {
        return retrofit.create(ChatApi::class.java)
    }
}
