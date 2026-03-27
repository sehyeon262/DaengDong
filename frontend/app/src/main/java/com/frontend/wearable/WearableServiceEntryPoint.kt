package com.frontend.wearable

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * WearableListenerService는 Android가 직접 인스턴스화하므로
 * @AndroidEntryPoint 사용 불가 → EntryPointAccessors로 수동 DI.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WearableServiceEntryPoint
