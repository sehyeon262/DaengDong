package com.frontend.di

import com.frontend.data.remote.AuthApi
import com.frontend.data.remote.BadgeApi
import com.frontend.data.remote.DangerZoneApi
import com.frontend.data.remote.DogApi
import com.frontend.data.remote.HomeApi
import com.frontend.data.remote.PlaceApi
import com.frontend.data.remote.RecordApi
import com.frontend.data.remote.RouteApi
import com.frontend.data.remote.WalkApi
import com.frontend.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY  // 통신 내용 로그 출력
            })
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
}