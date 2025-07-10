package com.example.myapplication

import java.io.Serializable

data class Author(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val dateOfBirth: String = "",
    val phoneNumber: String = "",
    val country: String = "",
    val age: String = "",
    val createdAt: Long = 0,
    val quizCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isAuthor: Boolean = true, // Always true for authors
    val totalQuizViews: Long = 0,
    val totalQuizLikes: Long = 0,
    val featuredQuizzes: List<String> = emptyList()
) : Serializable