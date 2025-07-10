package com.example.myapplication

import java.io.Serializable

data class Quiz(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorImageUrl: String = "",
    val imageUrl: String = "",
    val collection: String = "",
    val category: String = "",  // Added for compatibility with existing code
    val questionCount: Int = 0,
    val keywords: List<String> = listOf(),
    val theme: String = "classic",
    val visibility: String = "public",
    val questionVisibility: String = "reveal",
    val timestamp: Long = 0,
    val favoriteCount: Int = 0,
    val shareCount: Int = 0,
    val playCount: Int = 0
) : Serializable