package com.example.aistuddybuddy.ui.screens.questions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aistuddybuddy.data.image.BitmapLoader
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.MissingApiKeyException
import com.example.aistuddybuddy.domain.model.Question
import com.example.aistuddybuddy.domain.usecase.AskAiUseCase
import com.example.aistuddybuddy.domain.usecase.ExtractQuestionsUseCase
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
    data class Success(val text: String, val question: Question) : AnswerState

    data class Error(val message: String, val needsApiKey: Boolean = false) : AnswerState
}

data class QuestionItem(
    val id: Long,
    val question: Question,
    val isEditing: Boolean = false,
    val selectedProvider: AiProvider? = null,
    val answers: Map<AiProvider, AnswerState> = emptyMap()
)

data class ScanUiState(
    val image: Bitmap? = null,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val questions: List<QuestionItem> = emptyList()
)

/**
 * Shared across the camera, crop and questions screens: holds the page being scanned
 * and every question the user has collected so far, along with their AI answers.
 */
class ScanViewModel(
    private val extractQuestions: ExtractQuestionsUseCase,
    private val askAi: AskAiUseCase,
    private val bitmapLoader: BitmapLoader
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var nextId = 0L
    private val answerJobs = mutableMapOf<Pair<Long, AiProvider>, Job>()

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

    /** Runs OCR on the selected part of the page and adds the found questions to the top of the list. */
    fun extractQuestions(cropRect: Rect, onSuccess: () -> Unit) {
        val image = _uiState.value.image ?: return
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val cropped = withContext(Dispatchers.Default) { image.cropNormalized(cropRect) }
            extractQuestions(cropped)
                .onSuccess { questions ->
                    val items = questions.map { QuestionItem(id = nextId++, question = it) }
                    _uiState.update { it.copy(isBusy = false, questions = items + it.questions) }
                    onSuccess()
                }
                .onFailure { showError(it.message ?: "Couldn't read text from the image.") }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun ask(id: Long, provider: AiProvider, forceRefresh: Boolean = false) {
        val item = findItem(id) ?: return
        updateItem(id) { it.copy(selectedProvider = provider) }

        val existing = item.answers[provider]
        val upToDate = existing is AnswerState.Success && existing.question == item.question
        if (existing is AnswerState.Loading || (upToDate && !forceRefresh)) return

        val question = item.question
        if (question.text.isBlank()) {
            setAnswer(id, provider, AnswerState.Error("The question is empty. Tap edit to type it in."))
            return
        }

        setAnswer(id, provider, AnswerState.Loading)
        answerJobs[id to provider] = viewModelScope.launch {
            val state = askAi(provider, question).fold(
                onSuccess = { AnswerState.Success(it, question) },
                onFailure = {
                    AnswerState.Error(
                        message = it.message ?: "Something went wrong. Please try again.",
                        needsApiKey = it is MissingApiKeyException
                    )
                }
            )
            setAnswer(id, provider, state)
            answerJobs.remove(id to provider)
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
        _uiState.update { it.copy(isBusy = false, errorMessage = message) }
    }

    private fun findItem(id: Long) = _uiState.value.questions.firstOrNull { it.id == id }

    private fun setAnswer(id: Long, provider: AiProvider, state: AnswerState) = updateItem(id) {
        it.copy(answers = it.answers + (provider to state))
    }

    private fun updateItem(id: Long, transform: (QuestionItem) -> QuestionItem) {
        _uiState.update { state ->
            state.copy(questions = state.questions.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun cancelJobs(predicate: (Pair<Long, AiProvider>) -> Boolean) {
        answerJobs.keys.filter(predicate).forEach { answerJobs.remove(it)?.cancel() }
    }
}
