package com.mhq.fynecast.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mhq.fynecast.auth.domain.usecases.session.CheckUserLoggedInUseCase
import com.mhq.fynecast.core.di.FyneCastApplication

class SessionViewModel(
    checkUserLoggedIn: CheckUserLoggedInUseCase
) : ViewModel() {
    val startDestination: String =
        if (checkUserLoggedIn())
            BottomNavigationItem.Home.route
        else
            BottomNavigationItem.Signup.route

    companion object {
        val factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)
                SessionViewModel(
                    checkUserLoggedIn = CheckUserLoggedInUseCase(app.container.authRepository)
                )
            }
        }
    }
}