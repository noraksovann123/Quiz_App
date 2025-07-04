package com.example.myapplication

import java.io.Serializable

data class QuizQuestion(
    val text: String = "",
    val imageUrl: String = "",
    val options: List<String> = listOf(),
    val correctOptionIndex: Int = 0
) : Serializable