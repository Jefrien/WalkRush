package dev.jefrien.walkrush.domain.repository

import dev.jefrien.walkrush.domain.model.auth.AuthResult
import dev.jefrien.walkrush.domain.model.auth.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository for authentication operations
 */
interface AuthRepository {

    /**
     * Current authenticated user flow
     */
    val currentUser: Flow<User?>

    /**
     * Check if user is currently authenticated
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * Get current user ID or null
     */
    fun currentUserId(): String?

    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): AuthResult<User>

    /**
     * Sign up with email and password
     */
    suspend fun signUp(email: String, password: String): AuthResult<User>

    /**
     * Sign out current user
     */
    suspend fun signOut(): AuthResult<Unit>

    /**
     * Reset password for email
     */
    suspend fun resetPassword(email: String): AuthResult<Unit>

    /**
     * Handle OAuth deep link callback
     */
    suspend fun handleDeepLink(url: String): AuthResult<User>
}