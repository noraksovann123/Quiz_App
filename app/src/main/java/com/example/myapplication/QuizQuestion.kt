package com.example.myapplication

data class QuizOption(
    val text: String = "",
    val isCorrect: Boolean = false
)

data class QuizQuestion(
    val id: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val options: Map<String, QuizOption> = mapOf()
)