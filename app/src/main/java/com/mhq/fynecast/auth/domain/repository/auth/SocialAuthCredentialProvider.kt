package com.mhq.fynecast.auth.domain.repository.auth

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity

interface SocialAuthCredentialProvider {
    suspend fun getGoogleIdToken(context: Context): String
    suspend fun getFacebookAccessToken(activity: ComponentActivity): String
}