package com.mhq.fynecast.core.navigation

import androidx.lifecycle.ViewModel
import com.mhq.fynecast.auth.domain.usecases.session.CheckUserLoggedInUseCase

class SessionViewModel(
    checkUserLoggedIn: CheckUserLoggedInUseCase
) : ViewModel() {
    val startDestination: String =
        if (checkUserLoggedIn())
            AppDestination.Home.route
        else
            AppDestination.Signup.route
}