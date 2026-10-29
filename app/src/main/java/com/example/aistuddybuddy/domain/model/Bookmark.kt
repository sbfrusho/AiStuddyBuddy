package com.example.aistuddybuddy.domain.model

data class Bookmark(
    val id: Long = 0,
    val question: Question,
    val answer: String,
    val provider: AiProvider,
    val modelLabel: String,
    val style: AnswerStyle,
    val createdAt: Long = System.currentTimeMillis()
)
