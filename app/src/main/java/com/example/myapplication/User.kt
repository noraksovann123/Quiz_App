package com.example.myapplication

data class User(
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
    val isAuthor: Boolean = false // Differentiates between guest and registered user
)