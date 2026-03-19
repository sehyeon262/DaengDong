package com.frontend.ui.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.data.local.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    sealed class AuthState {
        object Loading : AuthState()
        object LoggedIn : AuthState()
        object NotLoggedIn : AuthState()
    }

    val authState = flow {
        val token = tokenDataStore.getAccessToken().first()
        if (!token.isNullOrBlank()) emit(AuthState.LoggedIn)
        else emit(AuthState.NotLoggedIn)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AuthState.Loading)
}
