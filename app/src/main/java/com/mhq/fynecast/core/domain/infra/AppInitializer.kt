package com.mhq.fynecast.core.domain.infra

interface AppInitializer {
    fun initializeSdkComponents()
    fun initializeDebugFeatures()
}