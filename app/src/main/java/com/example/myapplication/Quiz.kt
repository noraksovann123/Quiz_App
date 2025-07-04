package com.example.myapplication

import java.io.Serializable

data class Quiz(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorImageUrl: String = "",
    val questionCount: Int = 0,
    val playCount: Int = 0,
    val favoriteCount: Int = 0,
    val shareCount: Int = 0,
    val isFavorite: Boolean = false,
    val isFollowing: Boolean = false,
    val timestamp: Long = 0,
    val category: String = ""
) : Serializable
