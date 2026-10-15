package com.mhq.fynecast.auth.data.repository.auth

import android.content.Context
import androidx.activity.ComponentActivity

interface SocialAuthCredentialProvider {
    suspend fun getGoogleIdToken(context: Context): String
    suspend fun getFacebookAccessToken(activity: ComponentActivity): String
}