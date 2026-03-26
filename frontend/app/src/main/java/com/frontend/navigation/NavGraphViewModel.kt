package com.frontend.navigation

import androidx.lifecycle.ViewModel
import com.frontend.data.local.UnauthorizedEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

/**
 * NavGraph 에서 UnauthorizedEventBus 를 observe 하기 위한 ViewModel.
 * Hilt 를 통해 싱글톤 UnauthorizedEventBus 를 주입받아 이벤트를 노출함.
 */
@HiltViewModel
class NavGraphViewModel @Inject constructor(
    unauthorizedEventBus: UnauthorizedEventBus
) : ViewModel() {
    val unauthorizedEvent: SharedFlow<Unit> = unauthorizedEventBus.unauthorizedEvent
}
