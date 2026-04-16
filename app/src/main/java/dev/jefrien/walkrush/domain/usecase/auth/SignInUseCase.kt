package dev.jefrien.walkrush.domain.usecase.auth

import dev.jefrien.walkrush.domain.model.auth.AuthException
import dev.jefrien.walkrush.domain.model.auth.AuthResult
import dev.jefrien.walkrush.domain.model.auth.User
import dev.jefrien.walkrush.domain.repository.AuthRepository

/**
 * Sign in user with email and password
 */
class SignInUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AuthResult<User> {
        // Validation
        if (email.isBlank()) {
            return AuthResult.Error(
                AuthException.InvalidCredentials("Email requerido")
            )
        }
        if (password.isBlank()) {
            return AuthResult.Error(
                AuthException.InvalidCredentials("Contraseña requerida")
            )
        }
        if (!email.isValidEmail()) {
            return AuthResult.Error(
                AuthException.InvalidCredentials("Email inválido")
            )
        }

        return authRepository.signIn(email.trim(), password)
    }

    private fun String.isValidEmail(): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }
}