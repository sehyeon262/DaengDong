package com.frontend.wearable

import com.frontend.data.local.TokenDataStore
import com.frontend.data.repository.PlaceRepository
import com.frontend.data.repository.WalkRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * WearableListenerService는 Android가 직접 인스턴스화하므로
 * @AndroidEntryPoint 사용 불가 → EntryPointAccessors로 수동 DI.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WearableServiceEntryPoint {
    fun walkRepository(): WalkRepository
    fun placeRepository(): PlaceRepository
    fun tokenDataStore(): TokenDataStore
}
