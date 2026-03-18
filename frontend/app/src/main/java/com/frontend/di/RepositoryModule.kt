package com.frontend.di

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.AuthApi
import com.frontend.data.remote.DogApi
import com.frontend.data.remote.HomeApi
import com.frontend.data.remote.RecordApi
import com.frontend.data.repository.AuthRepository
import com.frontend.data.repository.DogRepository
import com.frontend.data.repository.HomeRepository
import com.frontend.data.repository.RecordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: AuthApi,
        tokenDataStore: TokenDataStore
    ): AuthRepository {
        return AuthRepository(api, tokenDataStore)
    }

    @Provides
    @Singleton
    fun provideHomeRepository(
        homeApi: HomeApi,
        tokenDataStore: TokenDataStore
    ): HomeRepository {
        return HomeRepository(homeApi, tokenDataStore)
    }

    @Provides
    @Singleton
    fun provideDogRepository(
        dogApi: DogApi,
        tokenDataStore: TokenDataStore
    ): DogRepository {
        return DogRepository(dogApi, tokenDataStore)
    }

    @Provides
    @Singleton
    fun provideRecordRepository(
        recordApi: RecordApi,
        tokenDataStore: TokenDataStore
    ): RecordRepository {
        return RecordRepository(recordApi, tokenDataStore)
    }
}