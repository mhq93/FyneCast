package com.mhq.fynecast.core.data.infra

import android.app.Application
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
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
            for (signature in getSigningSignatures()) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Log.d("AppInitializer", "Facebook key hash: $hashKey")
            }
        } catch (e: Exception) {
            Log.w("AppInitializer", "Could not read the signing certificate", e)
        }
    }

    /**
     * Reads the app's signing certificates using the non-deprecated API for
     * the running OS version, falling back to the old signature API only on
     * versions where no replacement exists.
     */
    private fun getSigningSignatures(): Array<Signature> {
        val packageName = application.packageName
        val packageManager = application.packageManager

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val flags = PackageManager.PackageInfoFlags.of(
                    PackageManager.GET_SIGNING_CERTIFICATES.toLong()
                )
                val signingInfo = packageManager.getPackageInfo(packageName, flags).signingInfo
                signingInfo?.let {
                    if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory
                } ?: emptyArray()
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                @Suppress("DEPRECATION")
                val signingInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo
                signingInfo?.let {
                    if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory
                } ?: emptyArray()
            }

            else -> {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                ).signatures ?: emptyArray()
            }
        }
    }
}