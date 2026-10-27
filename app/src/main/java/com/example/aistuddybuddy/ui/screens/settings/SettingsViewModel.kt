package com.example.aistuddybuddy.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.ProviderConfig
import com.example.aistuddybuddy.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _drafts = MutableStateFlow(settingsRepository.configs.value)
    val drafts: StateFlow<Map<AiProvider, ProviderConfig>> = _drafts.asStateFlow()

    fun updateApiKey(provider: AiProvider, apiKey: String) = updateDraft(provider) { it.copy(apiKey = apiKey) }

    fun updateModel(provider: AiProvider, model: String) = updateDraft(provider) { it.copy(model = model) }

    fun save() {
        _drafts.value.forEach { (provider, config) -> settingsRepository.save(provider, config) }
        _drafts.value = settingsRepository.configs.value
    }

    private fun updateDraft(provider: AiProvider, transform: (ProviderConfig) -> ProviderConfig) {
        _drafts.update { drafts ->
            drafts + (provider to transform(drafts[provider] ?: ProviderConfig()))
        }
    }
}
