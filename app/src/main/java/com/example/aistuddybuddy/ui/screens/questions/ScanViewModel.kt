package com.example.aistuddybuddy.ui.screens.questions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aistuddybuddy.data.image.BitmapLoader
import com.example.aistuddybuddy.domain.model.AiAnswer
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.AnswerStyle
import com.example.aistuddybuddy.domain.model.Bookmark
import com.example.aistuddybuddy.domain.model.MissingApiKeyException
import com.example.aistuddybuddy.domain.model.Question
import com.example.aistuddybuddy.domain.repository.BookmarkRepository
import com.example.aistuddybuddy.domain.usecase.AskAiUseCase
import com.example.aistuddybuddy.domain.usecase.ScanQuestionsUseCase
import com.example.aistuddybuddy.util.cropNormalized
import com.example.aistuddybuddy.util.rotate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/** [busyMessage] describes the work in progress; [notice] is shown once on the questions screen. */
data class ScanUiState(
    val image: Bitmap? = null,
    val isBusy: Boolean = false,
    val busyMessage: String? = null,
    val errorMessage: String? = null,
    val notice: String? = null,
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
    private val bitmapLoader: BitmapLoader
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var nextId = 0L
    private val answerJobs = mutableMapOf<Pair<Long, AnswerKey>, Job>()

    fun onImageCaptured(bitmap: Bitmap) {
        _uiState.update { it.copy(image = bitmap) }
    }

    fun loadImage(uri: Uri, onLoaded: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, busyMessage = null) }
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

    /** Reads the selected part of the page and adds the found questions to the top of the list. */
    fun extractQuestions(cropRect: Rect, onSuccess: () -> Unit) {
        val image = _uiState.value.image ?: return
        if (_uiState.value.isBusy) return
        val reader = scanQuestions.reader()
        viewModelScope.launch {
            _uiState.update {
                it.copy(isBusy = true, busyMessage = reader?.let { r -> "Reading with ${r.displayName}…" } ?: "Reading text…")
            }
            val cropped = withContext(Dispatchers.Default) { image.cropNormalized(cropRect) }
            scanQuestions(cropped)
                .onSuccess { result ->
                    val items = result.questions.map {
                        QuestionItem(id = nextId++, question = it.question, image = if (it.needsImage) cropped else null)
                    }
                    _uiState.update {
                        it.copy(isBusy = false, busyMessage = null, notice = result.notice, questions = items + it.questions)
                    }
                    onSuccess()
                }
                .onFailure { showError(it.message ?: "Couldn't read text from the image.") }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
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
        _uiState.update { it.copy(questions = emptyList()) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(isBusy = false, busyMessage = null, errorMessage = message) }
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
}
