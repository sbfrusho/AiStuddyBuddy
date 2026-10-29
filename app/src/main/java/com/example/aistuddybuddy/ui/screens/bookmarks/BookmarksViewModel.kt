package com.example.aistuddybuddy.ui.screens.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aistuddybuddy.domain.model.Bookmark
import com.example.aistuddybuddy.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarksViewModel(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    /** Null until the first load finishes, so the screen doesn't flash its empty state. */
    val bookmarks: StateFlow<List<Bookmark>?> = bookmarkRepository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(id: Long) {
        viewModelScope.launch { bookmarkRepository.delete(id) }
    }
}
