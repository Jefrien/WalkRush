package dev.jefrien.walkrush.domain.usecase.userprofile

import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.domain.repository.UserProfileRepository

class GetUserProfileUseCase(
    private val userProfileRepository: UserProfileRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): UserProfile? {
        val userId = authRepository.currentUserId() ?: return null
        return userProfileRepository.getUserProfile(userId)
    }
}