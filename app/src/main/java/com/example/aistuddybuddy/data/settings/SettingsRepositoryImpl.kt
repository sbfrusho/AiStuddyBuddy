package com.example.aistuddybuddy.data.settings

import android.content.Context
import com.example.aistuddybuddy.BuildConfig
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.AnswerLanguage
import com.example.aistuddybuddy.domain.model.AppPreferences
import com.example.aistuddybuddy.domain.model.ProviderConfig
import com.example.aistuddybuddy.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Stores per-provider API keys (encrypted with [KeyCipher]), model names and app preferences in
 * private SharedPreferences. Debug builds fall back to keys from local.properties (via BuildConfig).
 */
class SettingsRepositoryImpl(
    context: Context,
    private val keyCipher: KeyCipher
) : SettingsRepository {

    private val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)

    private val _configs = MutableStateFlow(AiProvider.entries.associateWith { load(it) })
    override val configs: StateFlow<Map<AiProvider, ProviderConfig>> = _configs.asStateFlow()

    private val _preferences = MutableStateFlow(loadPreferences())
    override val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    override fun config(provider: AiProvider): ProviderConfig =
        _configs.value[provider] ?: ProviderConfig(model = provider.defaultModel)

    override fun save(provider: AiProvider, config: ProviderConfig) {
        val cleaned = ProviderConfig(
            apiKey = config.apiKey.trim(),
            model = config.model.trim().ifBlank { provider.defaultModel }
        )
        prefs.edit()
            .putString(keyFor(provider), keyCipher.encrypt(cleaned.apiKey))
            .putString(modelFor(provider), cleaned.model)
            .apply()
        _configs.update { it + (provider to cleaned) }
    }

    override fun savePreferences(preferences: AppPreferences) {
        prefs.edit()
            .putString(ANSWER_LANGUAGE, preferences.answerLanguage.name)
            .putBoolean(READ_WITH_AI, preferences.readWithAi)
            .apply()
        _preferences.value = preferences
    }

    private fun load(provider: AiProvider) = ProviderConfig(
        apiKey = loadApiKey(provider).ifBlank { buildConfigKey(provider) },
        model = prefs.getString(modelFor(provider), null)?.takeIf { it.isNotBlank() } ?: provider.defaultModel
    )

    private fun loadApiKey(provider: AiProvider): String {
        val stored = prefs.getString(keyFor(provider), null).orEmpty()
        if (stored.isBlank()) return ""
        val key = keyCipher.decrypt(stored).orEmpty()
        // Keys saved by older versions are plain text; encrypt them in place.
        if (!keyCipher.isEncrypted(stored)) prefs.edit().putString(keyFor(provider), keyCipher.encrypt(key)).apply()
        return key
    }

    private fun loadPreferences() = AppPreferences(
        answerLanguage = prefs.getString(ANSWER_LANGUAGE, null)
            ?.let { name -> AnswerLanguage.entries.firstOrNull { it.name == name } }
            ?: AnswerLanguage.ENGLISH,
        readWithAi = prefs.getBoolean(READ_WITH_AI, true)
    )

    private fun buildConfigKey(provider: AiProvider): String = when (provider) {
        AiProvider.CHATGPT -> BuildConfig.OPENAI_API_KEY
        AiProvider.GEMINI -> BuildConfig.GEMINI_API_KEY
        AiProvider.DEEPSEEK -> BuildConfig.DEEPSEEK_API_KEY
        AiProvider.OPENROUTER -> BuildConfig.OPENROUTER_API_KEY
    }

    private fun keyFor(provider: AiProvider) = "api_key_${provider.name}"
    private fun modelFor(provider: AiProvider) = "model_${provider.name}"

    private companion object {
        const val ANSWER_LANGUAGE = "answer_language"
        const val READ_WITH_AI = "read_with_ai"
    }
}
