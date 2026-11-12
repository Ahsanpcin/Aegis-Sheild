package com.example.di

import android.content.Context
import com.example.data.local.AegisDatabase
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
import com.example.data.repository.SecurityHistoryRepository

class AegisContainer(context: Context) {
    val database: AegisDatabase by lazy {
        AegisDatabase.getInstance(context)
    }

    val preferences: AegisPreferences by lazy {
        AegisPreferences(context)
    }

    val protectionRepository: ProtectionRepository by lazy {
        ProtectionRepository(context)
    }

    val securityHistoryRepository: SecurityHistoryRepository by lazy {
        SecurityHistoryRepository(database.incidentDao())
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
