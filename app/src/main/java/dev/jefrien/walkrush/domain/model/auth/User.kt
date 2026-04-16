package dev.jefrien.walkrush.domain.model.auth


/**
 * Authenticated user model
 */
data class User(
    val id: String,
    val email: String,
    val isEmailVerified: Boolean,
    val createdAt: Long
)