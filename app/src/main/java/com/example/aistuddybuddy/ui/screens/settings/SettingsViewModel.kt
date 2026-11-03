package com.example.aistuddybuddy.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.AnswerLanguage
import com.example.aistuddybuddy.domain.model.AppPreferences
import com.example.aistuddybuddy.domain.model.ProviderConfig
import com.example.aistuddybuddy.domain.repository.AiRepository
import com.example.aistuddybuddy.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface KeyStatus {
    data object Idle : KeyStatus
    data object Testing : KeyStatus
    data object Works : KeyStatus
    data class Failed(val message: String) : KeyStatus
}

/**
 * One AI's card. [saved] is what's stored; [draftKey] is what's typed but not saved yet (blank
 * means "keep the saved key"), so the saved key is never shown back in full.
 */
data class ProviderUiState(
    val provider: AiProvider,
    val saved: ProviderConfig,
    val draftKey: String = "",
    val status: KeyStatus = KeyStatus.Idle
) {
    val isConnected: Boolean get() = saved.apiKey.isNotBlank()

    /** The last 4 characters of the saved key, enough to recognise it without revealing it. */
    val maskedKey: String get() = if (isConnected) "••••" + saved.apiKey.takeLast(4) else ""
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val drafts = MutableStateFlow<Map<AiProvider, String>>(emptyMap())
    private val statuses = MutableStateFlow<Map<AiProvider, KeyStatus>>(emptyMap())

    val providers: StateFlow<List<ProviderUiState>> =
        combine(settingsRepository.configs, drafts, statuses) { configs, drafts, statuses ->
            AiProvider.entries.map { provider ->
                ProviderUiState(
                    provider = provider,
                    saved = configs[provider] ?: ProviderConfig(model = provider.defaultModel),
                    draftKey = drafts[provider].orEmpty(),
                    status = statuses[provider] ?: KeyStatus.Idle
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val preferences: StateFlow<AppPreferences> = settingsRepository.preferences

    private val _anyConnected = MutableStateFlow(settingsRepository.configs.value.values.any { it.apiKey.isNotBlank() })
    val anyConnected: StateFlow<Boolean> = _anyConnected.asStateFlow()

    fun updateDraftKey(provider: AiProvider, key: String) {
        // Keys never contain whitespace; dropping it fixes the most common paste mistake.
        drafts.update { it + (provider to key.filterNot(Char::isWhitespace)) }
        statuses.update { it - provider }
    }

    /** Checks the typed key (or the saved one, if nothing new is typed) and saves it if it works. */
    fun testAndSave(provider: AiProvider) {
        val state = providers.value.firstOrNull { it.provider == provider } ?: return
        val key = state.draftKey.ifBlank { state.saved.apiKey }
        if (key.isBlank()) {
            statuses.update { it + (provider to KeyStatus.Failed("Paste your key first.")) }
            return
        }
        statuses.update { it + (provider to KeyStatus.Testing) }
        viewModelScope.launch {
            val model = state.saved.model.ifBlank { provider.defaultModel }
            aiRepository.verifyKey(provider, key, model)
                .onSuccess {
                    settingsRepository.save(provider, state.saved.copy(apiKey = key))
                    drafts.update { it - provider }
                    statuses.update { it + (provider to KeyStatus.Works) }
                    _anyConnected.value = true
                }
                .onFailure { error ->
                    statuses.update {
                        it + (provider to KeyStatus.Failed(error.message ?: "The key didn't work. Please check it."))
                    }
                }
        }
    }

    /** Saves the typed key without testing, for when the provider is busy but the key is right. */
    fun saveWithoutTesting(provider: AiProvider) {
        val state = providers.value.firstOrNull { it.provider == provider } ?: return
        if (state.draftKey.isBlank()) return
        settingsRepository.save(provider, state.saved.copy(apiKey = state.draftKey))
        drafts.update { it - provider }
        statuses.update { it - provider }
        _anyConnected.value = true
    }

    fun removeKey(provider: AiProvider) {
        settingsRepository.removeKey(provider)
        drafts.update { it - provider }
        statuses.update { it - provider }
        _anyConnected.value = settingsRepository.configs.value.values.any { it.apiKey.isNotBlank() }
    }

    fun updateModel(provider: AiProvider, model: String) {
        val saved = settingsRepository.config(provider)
        settingsRepository.save(provider, saved.copy(model = model))
    }

    fun updateAnswerLanguage(language: AnswerLanguage) =
        settingsRepository.savePreferences(preferences.value.copy(answerLanguage = language))

    fun updateReadWithAi(enabled: Boolean) =
        settingsRepository.savePreferences(preferences.value.copy(readWithAi = enabled))

    fun finishOnboarding() =
        settingsRepository.savePreferences(preferences.value.copy(onboardingDone = true))
}
