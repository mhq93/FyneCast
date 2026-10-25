package com.mhq.fynecast.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mhq.fynecast.auth.domain.usecases.profile.RefreshUserSessionUseCase
import com.mhq.fynecast.auth.domain.usecases.session.CheckUserLoggedInUseCase
import com.mhq.fynecast.core.navigation.AppDestination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SessionViewModel(
    checkUserLoggedIn: CheckUserLoggedInUseCase,
    private val refreshUserSession: RefreshUserSessionUseCase
) : ViewModel() {

    val startDestination: String =
        if (checkUserLoggedIn())
            AppDestination.Home.route
        else
            AppDestination.Signup.route

    private val _sessionExpired = Channel<Unit>(Channel.BUFFERED)
    val sessionExpired = _sessionExpired.receiveAsFlow()

    /** Checks with Firebase that the account is still valid, e.g. after an email change. */
    fun onAppResumed() {
        viewModelScope.launch {
            if (!refreshUserSession()) _sessionExpired.send(Unit)
        }
    }
}