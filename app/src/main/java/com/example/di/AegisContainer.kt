package com.example.di

import android.content.Context
import com.example.blocking.overlay.SystemOverlayController
import com.example.data.local.AegisDatabase
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.data.repository.SecurityHistoryRepository
import com.example.detection.classifier.ModelManager

class AegisContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AegisDatabase by lazy {
        AegisDatabase.getInstance(appContext)
    }

    val preferences: AegisPreferences by lazy {
        AegisPreferences(appContext)
    }

    val modelManager: ModelManager by lazy {
        ModelManager(appContext, preferences)
    }

    val protectionRepository: ProtectionRepository by lazy {
        ProtectionRepository(appContext).apply {
            updateModelInfo(modelManager.activeModelInfo.value)
        }
    }

    val securityHistoryRepository: SecurityHistoryRepository by lazy {
        SecurityHistoryRepository(database.incidentDao())
    }

    val overlayController: SystemOverlayController by lazy {
        SystemOverlayController(appContext)
    }

    fun refreshModelTelemetry() {
        val info = modelManager.refreshStatus()
        protectionRepository.updateModelInfo(info)
    }

    companion object {
        @Volatile
        private var INSTANCE: AegisContainer? = null

        fun getInstance(context: Context): AegisContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AegisContainer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
