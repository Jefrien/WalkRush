package dev.jefrien.walkrush.data.remote.supabase

import dev.jefrien.walkrush.domain.model.auth.AuthException
import dev.jefrien.walkrush.domain.model.auth.AuthResult
import dev.jefrien.walkrush.domain.model.auth.User
import dev.jefrien.walkrush.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Supabase implementation of AuthRepository
 */
class SupabaseAuthRepository(
    private val supabase: SupabaseClient
) : AuthRepository {

    private val auth: Auth
        get() = supabase.auth

    override val currentUser: Flow<User?> = auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                status.session.user?.toDomain()
            }

            else -> null
        }
    }

    override suspend fun awaitSessionInitialization() {
        auth.awaitInitialization()
    }

    override suspend fun isAuthenticated(): Boolean {
        return auth.currentSessionOrNull() != null
    }

    override fun currentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    override suspend fun signIn(email: String, password: String): AuthResult<User> {
        return try {
            val result = auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val user = auth.currentUserOrNull()?.toDomain()
                ?: return AuthResult.Error(AuthException.UnknownError("No se pudo obtener usuario"))

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(mapException(e))
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult<User> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val user = auth.currentUserOrNull()?.toDomain()
                ?: return AuthResult.Error(AuthException.UnknownError("No se pudo crear usuario"))

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(mapException(e))
        }
    }

    override suspend fun signOut(): AuthResult<Unit> {
        return try {
            auth.signOut()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(AuthException.NetworkError(e.message ?: "Error al cerrar sesión"))
        }
    }

    override suspend fun resetPassword(email: String): AuthResult<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(mapException(e))
        }
    }

    override suspend fun handleDeepLink(url: String): AuthResult<User> {
        // Handle OAuth callback deep links
        return try {
            // Parse and handle the deep link if needed
            // For PKCE flow, Supabase handles this automatically
            val user = auth.currentUserOrNull()?.toDomain()
                ?: return AuthResult.Error(AuthException.UnknownError("No se pudo completar autenticación"))

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(mapException(e))
        }
    }

    private fun UserInfo.toDomain(): User {
        return User(
            id = this.id,
            email = this.email ?: "",
            isEmailVerified = this.emailConfirmedAt != null,
            createdAt = this.createdAt?.toEpochMilliseconds() ?: System.currentTimeMillis()
        )
    }

    private fun mapException(e: Exception): AuthException {
        val message = e.message ?: "Error desconocido"
        return when {
            message.contains("Invalid login credentials", ignoreCase = true) ->
                AuthException.InvalidCredentials()

            message.contains("User not found", ignoreCase = true) ->
                AuthException.UserNotFound()

            message.contains("Email rate limit exceeded", ignoreCase = true) ||
                    message.contains("already registered", ignoreCase = true) ->
                AuthException.EmailAlreadyExists()

            message.contains("Password should be at least", ignoreCase = true) ->
                AuthException.WeakPassword()

            message.contains("network", ignoreCase = true) ||
                    message.contains("connection", ignoreCase = true) ->
                AuthException.NetworkError()

            else -> AuthException.UnknownError(message)
        }
    }
}