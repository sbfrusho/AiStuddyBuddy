package com.example.aistuddybuddy.data.bookmarks

import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.AnswerStyle
import com.example.aistuddybuddy.domain.model.Bookmark
import com.example.aistuddybuddy.domain.model.Question
import com.example.aistuddybuddy.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepositoryImpl(private val dao: BookmarkDao) : BookmarkRepository {

    override val bookmarks: Flow<List<Bookmark>> = dao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun save(bookmark: Bookmark): Long = dao.insert(
        BookmarkEntity(
            questionText = bookmark.question.text,
            options = bookmark.question.options,
            answer = bookmark.answer,
            provider = bookmark.provider.name,
            modelLabel = bookmark.modelLabel,
            style = bookmark.style.name,
            createdAt = bookmark.createdAt
        )
    )

    override suspend fun delete(id: Long) = dao.delete(id)

    private fun BookmarkEntity.toDomain(): Bookmark? {
        val provider = AiProvider.entries.firstOrNull { it.name == provider } ?: return null
        return Bookmark(
            id = id,
            question = Question(questionText, options),
            answer = answer,
            provider = provider,
            modelLabel = modelLabel,
            style = AnswerStyle.entries.firstOrNull { it.name == style } ?: AnswerStyle.STANDARD,
            createdAt = createdAt
        )
    }
}
