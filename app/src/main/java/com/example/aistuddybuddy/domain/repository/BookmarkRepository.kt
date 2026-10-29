package com.example.aistuddybuddy.domain.repository

import com.example.aistuddybuddy.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    val bookmarks: Flow<List<Bookmark>>

    /** Returns the new bookmark's id. */
    suspend fun save(bookmark: Bookmark): Long

    suspend fun delete(id: Long)
}
