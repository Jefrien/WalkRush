package dev.jefrien.walkrush.domain.usecase.auth

import dev.jefrien.walkrush.domain.model.auth.AuthResult
import dev.jefrien.walkrush.domain.repository.AuthRepository

/**
 * Sign out current user
 */
class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AuthResult<Unit> {
        return authRepository.signOut()
    }
}