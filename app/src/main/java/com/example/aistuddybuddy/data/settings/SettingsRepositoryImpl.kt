package com.example.aistuddybuddy.data.settings

import android.content.Context
import com.example.aistuddybuddy.BuildConfig
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.ProviderConfig
import com.example.aistuddybuddy.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Stores per-provider API keys and model names in private SharedPreferences.
 * Falls back to keys from local.properties (via BuildConfig) when none were entered.
 */
class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)

    private val _configs = MutableStateFlow(AiProvider.entries.associateWith { load(it) })
    override val configs: StateFlow<Map<AiProvider, ProviderConfig>> = _configs.asStateFlow()

    override fun config(provider: AiProvider): ProviderConfig =
        _configs.value[provider] ?: ProviderConfig(model = provider.defaultModel)

    override fun save(provider: AiProvider, config: ProviderConfig) {
        val cleaned = ProviderConfig(
            apiKey = config.apiKey.trim(),
            model = config.model.trim().ifBlank { provider.defaultModel }
        )
        prefs.edit()
            .putString(keyFor(provider), cleaned.apiKey)
            .putString(modelFor(provider), cleaned.model)
            .apply()
        _configs.update { it + (provider to cleaned) }
    }

    private fun load(provider: AiProvider) = ProviderConfig(
        apiKey = prefs.getString(keyFor(provider), null)?.takeIf { it.isNotBlank() } ?: buildConfigKey(provider),
        model = prefs.getString(modelFor(provider), null)?.takeIf { it.isNotBlank() } ?: provider.defaultModel
    )

    private fun buildConfigKey(provider: AiProvider): String = when (provider) {
        AiProvider.CHATGPT -> BuildConfig.OPENAI_API_KEY
        AiProvider.GEMINI -> BuildConfig.GEMINI_API_KEY
        AiProvider.DEEPSEEK -> BuildConfig.DEEPSEEK_API_KEY
        AiProvider.OPENROUTER -> BuildConfig.OPENROUTER_API_KEY
    }

    private fun keyFor(provider: AiProvider) = "api_key_${provider.name}"
    private fun modelFor(provider: AiProvider) = "model_${provider.name}"
}
