package com.example.aistuddybuddy.domain.repository

import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.AppPreferences
import com.example.aistuddybuddy.domain.model.ProviderConfig
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val configs: StateFlow<Map<AiProvider, ProviderConfig>>
    val preferences: StateFlow<AppPreferences>

    fun config(provider: AiProvider): ProviderConfig

    fun save(provider: AiProvider, config: ProviderConfig)

    fun savePreferences(preferences: AppPreferences)
}
