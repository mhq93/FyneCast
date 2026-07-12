package com.mhq.fynecast.core.data.infra

import android.app.Application
import android.util.Base64
import android.util.Log
import com.facebook.FacebookSdk
import com.mhq.fynecast.core.domain.infra.AppInitializer
import java.security.MessageDigest

class AppInitializerImpl(
    private val application: Application
) : AppInitializer {

    override fun initializeSdkComponents() {
        // Handle standard global startup engines here if needed
        // e.g., FirebaseApp.initializeApp(application)
    }

    override fun initializeDebugFeatures() {
        FacebookSdk.setIsDebugEnabled(true)
        printFacebookKeyHash()
    }

    private fun printFacebookKeyHash() {
        try {
            val info = application.packageManager.getPackageInfo(
                application.packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures!!) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = Base64.encodeToString(md.digest(), Base64.DEFAULT)
            }
        } catch (e: Exception) {

        }
    }
}