package com.frontend.di

import com.frontend.data.local.TokenDataStore
import com.frontend.data.remote.AuthApi
import com.frontend.data.remote.HomeApi
import com.frontend.data.repository.AuthRepository
import com.frontend.data.repository.HomeRepository
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
        homeApi: HomeApi
    ): HomeRepository {
        return HomeRepository(homeApi)
    }
}
