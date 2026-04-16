package dev.jefrien.walkrush.domain.usecase.auth

import dev.jefrien.walkrush.domain.model.auth.AuthException
import dev.jefrien.walkrush.domain.model.auth.AuthResult
import dev.jefrien.walkrush.domain.model.auth.User
import dev.jefrien.walkrush.domain.repository.AuthRepository

/**
 * Sign up new user with email and password
 */
class SignUpUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String
    ): AuthResult<User> {
        // Validation
        if (email.isBlank()) {
            return AuthResult.Error(
                AuthException.InvalidCredentials("Email requerido")
            )
        }
        if (!email.isValidEmail()) {
            return AuthResult.Error(
                AuthException.InvalidCredentials("Email inválido")
            )
        }
        if (password.isBlank()) {
            return AuthResult.Error(
                AuthException.WeakPassword("Contraseña requerida")
            )
        }
        if (password.length < 6) {
            return AuthResult.Error(
                AuthException.WeakPassword("Mínimo 6 caracteres")
            )
        }
        if (password != confirmPassword) {
            return AuthResult.Error(
                AuthException.InvalidCredentials("Las contraseñas no coinciden")
            )
        }

        return authRepository.signUp(email.trim(), password)
    }

    private fun String.isValidEmail(): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }
}