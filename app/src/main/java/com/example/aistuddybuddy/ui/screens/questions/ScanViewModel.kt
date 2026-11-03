package com.example.aistuddybuddy.ui.screens.questions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aistuddybuddy.data.image.BitmapLoader
import com.example.aistuddybuddy.domain.model.AiAnswer
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.AiReadFailedException
import com.example.aistuddybuddy.domain.model.AnswerStyle
import com.example.aistuddybuddy.domain.model.Bookmark
import com.example.aistuddybuddy.domain.model.MissingApiKeyException
import com.example.aistuddybuddy.domain.model.Question
import com.example.aistuddybuddy.domain.model.ScannedQuestion
import com.example.aistuddybuddy.domain.repository.BookmarkRepository
import com.example.aistuddybuddy.domain.repository.SettingsRepository
import com.example.aistuddybuddy.domain.usecase.AskAiUseCase
import com.example.aistuddybuddy.domain.usecase.ScanQuestionsUseCase
import com.example.aistuddybuddy.util.cropNormalized
import com.example.aistuddybuddy.util.rotate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AnswerState {
    data object Loading : AnswerState

    /** [question] is the version of the question this answer was generated for. */
    data class Success(
        val answer: AiAnswer,
        val question: Question,
        val bookmarkId: Long? = null
    ) : AnswerState

    data class Error(val message: String, val needsApiKey: Boolean = false) : AnswerState
}

/** Answers are kept per AI and per follow-up style so switching between them is instant. */
data class AnswerKey(val provider: AiProvider, val style: AnswerStyle)

/** [image] is the photo sent along with the question because it refers to a figure. */
data class QuestionItem(
    val id: Long,
    val question: Question,
    val image: Bitmap? = null,
    val isEditing: Boolean = false,
    val selectedProvider: AiProvider? = null,
    val selectedStyle: AnswerStyle = AnswerStyle.STANDARD,
    val answers: Map<AnswerKey, AnswerState> = emptyMap()
) {
    val selectedKey: AnswerKey? get() = selectedProvider?.let { AnswerKey(it, selectedStyle) }
}

/** A scan still being read, shown as a card above the questions until it finishes or fails. */
data class PendingScan(
    val id: Long,
    val image: Bitmap,
    val status: PendingStatus
)

sealed interface PendingStatus {
    data class Reading(val message: String = INITIAL) : PendingStatus {
        companion object {
            const val INITIAL = "Reading the photo…"
        }
    }

    /** [ocrQuestions] lets the student use OCR's result anyway; empty when OCR found nothing. */
    data class Failed(
        val message: String,
        val ocrQuestions: List<ScannedQuestion> = emptyList(),
        val needsApiKey: Boolean = false
    ) : PendingStatus
}

data class ScanUiState(
    val image: Bitmap? = null,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val pendingScans: List<PendingScan> = emptyList(),
    val questions: List<QuestionItem> = emptyList()
)

/**
 * Shared across the camera, crop and questions screens: holds the page being scanned
 * and every question the user has collected so far, along with their AI answers.
 */
class ScanViewModel(
    private val scanQuestions: ScanQuestionsUseCase,
    private val askAi: AskAiUseCase,
    private val bookmarkRepository: BookmarkRepository,
    private val bitmapLoader: BitmapLoader,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    /**
     * AIs to offer on question cards: the ones with a saved key. With no keys at all, Gemini and
     * OpenRouter are shown so tapping one explains how to add a key.
     */
    val hasAnyKey: StateFlow<Boolean> = settingsRepository.configs
        .map { configs -> configs.values.any { it.apiKey.isNotBlank() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val providers: StateFlow<List<AiProvider>> = settingsRepository.configs
        .map { configs ->
            AiProvider.entries.filter { configs[it]?.apiKey?.isNotBlank() == true }.ifEmpty { NO_KEY_PROVIDERS }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, NO_KEY_PROVIDERS)

    private var nextId = 0L
    private val answerJobs = mutableMapOf<Pair<Long, AnswerKey>, Job>()
    private val scanJobs = mutableMapOf<Long, Job>()

    fun onImageCaptured(bitmap: Bitmap) {
        _uiState.update { it.copy(image = bitmap) }
    }

    fun loadImage(uri: Uri, onLoaded: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val bitmap = bitmapLoader.load(uri)
                _uiState.update { it.copy(image = bitmap, isBusy = false) }
                onLoaded()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError("Couldn't open that image.")
            }
        }
    }

    fun rotateImage() {
        val image = _uiState.value.image ?: return
        viewModelScope.launch {
            val rotated = withContext(Dispatchers.Default) { image.rotate(90) }
            _uiState.update { it.copy(image = rotated) }
        }
    }

    /**
     * Starts reading the selected part of the page in the background. A card for it appears on the
     * questions screen right away and turns into questions when reading finishes.
     */
    fun startScan(cropRect: Rect) {
        val image = _uiState.value.image ?: return
        val scanId = nextId++
        viewModelScope.launch {
            val cropped = withContext(Dispatchers.Default) { image.cropNormalized(cropRect) }
            _uiState.update {
                it.copy(pendingScans = listOf(PendingScan(scanId, cropped, PendingStatus.Reading())) + it.pendingScans)
            }
            runScan(scanId, cropped)
        }
    }

    fun retryScan(scanId: Long) {
        val scan = _uiState.value.pendingScans.firstOrNull { it.id == scanId } ?: return
        if (scan.status is PendingStatus.Reading) return
        setScanStatus(scanId, PendingStatus.Reading())
        viewModelScope.launch { runScan(scanId, scan.image) }
    }

    /** Adds what on-device OCR found, for when the student would rather not wait for the AI. */
    fun useOcrResult(scanId: Long) {
        val failed = _uiState.value.pendingScans.firstOrNull { it.id == scanId }?.status as? PendingStatus.Failed ?: return
        addScannedQuestions(scanId, image = null, questions = failed.ocrQuestions)
    }

    fun dismissScan(scanId: Long) {
        scanJobs.remove(scanId)?.cancel()
        removeScan(scanId)
    }

    private fun runScan(scanId: Long, image: Bitmap) {
        scanJobs[scanId] = viewModelScope.launch {
            scanQuestions(image, onReadingWithAi = { setScanStatus(scanId, PendingStatus.Reading("Reading with ${it.displayName}…")) })
                .onSuccess { addScannedQuestions(scanId, image, it.questions) }
                .onFailure { error ->
                    setScanStatus(
                        scanId,
                        PendingStatus.Failed(
                            message = error.message ?: "Couldn't read text from the image.",
                            ocrQuestions = (error as? AiReadFailedException)?.ocrQuestions.orEmpty(),
                            needsApiKey = (error as? AiReadFailedException)?.needsApiKey == true
                        )
                    )
                }
            scanJobs.remove(scanId)
        }
    }

    /** [image] is attached to questions that refer to a figure; null drops it. */
    private fun addScannedQuestions(scanId: Long, image: Bitmap?, questions: List<ScannedQuestion>) {
        val items = questions.map {
            QuestionItem(id = nextId++, question = it.question, image = if (it.needsImage) image else null)
        }
        _uiState.update { state ->
            state.copy(
                pendingScans = state.pendingScans.filterNot { it.id == scanId },
                questions = items + state.questions
            )
        }
    }

    private fun setScanStatus(scanId: Long, status: PendingStatus) {
        _uiState.update { state ->
            state.copy(pendingScans = state.pendingScans.map { if (it.id == scanId) it.copy(status = status) else it })
        }
    }

    private fun removeScan(scanId: Long) {
        _uiState.update { state -> state.copy(pendingScans = state.pendingScans.filterNot { it.id == scanId }) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun ask(id: Long, provider: AiProvider, forceRefresh: Boolean = false) {
        val item = findItem(id) ?: return
        updateItem(id) { it.copy(selectedProvider = provider) }
        request(id, AnswerKey(provider, item.selectedStyle), forceRefresh)
    }

    /** Switches to a follow-up version of the answer from the selected AI. */
    fun selectStyle(id: Long, style: AnswerStyle) {
        val item = findItem(id) ?: return
        updateItem(id) { it.copy(selectedStyle = style) }
        val provider = item.selectedProvider ?: return
        request(id, AnswerKey(provider, style), forceRefresh = false)
    }

    fun toggleBookmark(id: Long) {
        val item = findItem(id) ?: return
        val key = item.selectedKey ?: return
        val state = item.answers[key] as? AnswerState.Success ?: return
        viewModelScope.launch {
            val bookmarkId = state.bookmarkId
            if (bookmarkId != null) {
                bookmarkRepository.delete(bookmarkId)
                updateAnswer(id, key) { it.copy(bookmarkId = null) }
            } else {
                val newId = bookmarkRepository.save(
                    Bookmark(
                        question = state.question,
                        answer = state.answer.text,
                        provider = state.answer.provider,
                        modelLabel = state.answer.modelLabel,
                        style = key.style
                    )
                )
                updateAnswer(id, key) { it.copy(bookmarkId = newId) }
            }
        }
    }

    private fun request(id: Long, key: AnswerKey, forceRefresh: Boolean) {
        val item = findItem(id) ?: return
        val existing = item.answers[key]
        val upToDate = existing is AnswerState.Success && existing.question == item.question
        if (existing is AnswerState.Loading || (upToDate && !forceRefresh)) return

        val question = item.question
        if (question.text.isBlank()) {
            setAnswer(id, key, AnswerState.Error("The question is empty. Tap edit to type it in."))
            return
        }

        setAnswer(id, key, AnswerState.Loading)
        answerJobs[id to key] = viewModelScope.launch {
            val state = askAi(key.provider, question, key.style, item.image).fold(
                onSuccess = { AnswerState.Success(it, question) },
                onFailure = {
                    AnswerState.Error(
                        message = it.message ?: "Something went wrong. Please try again.",
                        needsApiKey = it is MissingApiKeyException
                    )
                }
            )
            setAnswer(id, key, state)
            answerJobs.remove(id to key)
        }
    }

    fun toggleEditing(id: Long) = updateItem(id) { item ->
        val question = item.question
        if (item.isEditing) {
            // Drop empty option rows when leaving edit mode.
            item.copy(isEditing = false, question = question.copy(options = question.options.filter { it.isNotBlank() }))
        } else {
            item.copy(isEditing = true)
        }
    }

    fun updateQuestionText(id: Long, text: String) = updateItem(id) {
        it.copy(question = it.question.copy(text = text))
    }

    fun updateOption(id: Long, index: Int, text: String) = updateItem(id) {
        val options = it.question.options.toMutableList().apply { if (index in indices) set(index, text) }
        it.copy(question = it.question.copy(options = options))
    }

    fun addOption(id: Long) = updateItem(id) {
        it.copy(question = it.question.copy(options = it.question.options + ""))
    }

    fun removeOption(id: Long, index: Int) = updateItem(id) {
        val options = it.question.options.toMutableList().apply { if (index in indices) removeAt(index) }
        it.copy(question = it.question.copy(options = options))
    }

    /** Stops sending the photo with this question, e.g. when the figure isn't actually needed. */
    fun removeImage(id: Long) = updateItem(id) { it.copy(image = null) }

    fun addBlankQuestion() {
        _uiState.update {
            it.copy(questions = listOf(QuestionItem(id = nextId++, question = Question(""), isEditing = true)) + it.questions)
        }
    }

    fun deleteQuestion(id: Long) {
        cancelJobs { it.first == id }
        _uiState.update { state -> state.copy(questions = state.questions.filterNot { it.id == id }) }
    }

    fun clearAll() {
        cancelJobs { true }
        scanJobs.values.forEach { it.cancel() }
        scanJobs.clear()
        _uiState.update { it.copy(questions = emptyList(), pendingScans = emptyList()) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(isBusy = false, errorMessage = message) }
    }

    private fun findItem(id: Long) = _uiState.value.questions.firstOrNull { it.id == id }

    private fun setAnswer(id: Long, key: AnswerKey, state: AnswerState) = updateItem(id) {
        it.copy(answers = it.answers + (key to state))
    }

    private fun updateAnswer(id: Long, key: AnswerKey, transform: (AnswerState.Success) -> AnswerState.Success) =
        updateItem(id) { item ->
            val state = item.answers[key] as? AnswerState.Success ?: return@updateItem item
            item.copy(answers = item.answers + (key to transform(state)))
        }

    private fun updateItem(id: Long, transform: (QuestionItem) -> QuestionItem) {
        _uiState.update { state ->
            state.copy(questions = state.questions.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun cancelJobs(predicate: (Pair<Long, AnswerKey>) -> Boolean) {
        answerJobs.keys.filter(predicate).forEach { answerJobs.remove(it)?.cancel() }
    }

    private companion object {
        val NO_KEY_PROVIDERS = listOf(AiProvider.GEMINI, AiProvider.OPENROUTER)
    }
}
