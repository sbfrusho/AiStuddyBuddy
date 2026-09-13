package com.example.aistuddybuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aistuddybuddy.domain.repository.DiVerificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class DiStatusUiState (
    val statusMessage: String = "Checking DI container...",
    val isSuccess: Boolean = false
)

class MainViewModel (
    private val repository: DiVerificationRepository
) : ViewModel(){

    private val _uiState = MutableStateFlow(DiStatusUiState())
    val uiState = _uiState

    init {
        checkDiStatus()
    }

    private fun checkDiStatus() {
        viewModelScope.launch {
            val result = repository.verifyDependencyChain()
            _uiState.value = DiStatusUiState(
                statusMessage = result,
                isSuccess = result == "Koin Initialized Successfully"
            )
        }
    }
}