package com.example.aistuddybuddy.data.bookmarks

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionText: String,
    /** JSON array of option strings, see [BookmarkConverters]. */
    val options: List<String>,
    val answer: String,
    /** [com.example.aistuddybuddy.domain.model.AiProvider] name. */
    val provider: String,
    val modelLabel: String,
    /** [com.example.aistuddybuddy.domain.model.AnswerStyle] name. */
    val style: String,
    val createdAt: Long
)
