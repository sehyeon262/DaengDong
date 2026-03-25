package com.frontend.di

import com.frontend.BuildConfig
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

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
                        else HttpLoggingInterceptor.Level.NONE
            })
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
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
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
    fun provideKakaoRetrofit(okHttpClient: OkHttpClient): Retrofit {
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