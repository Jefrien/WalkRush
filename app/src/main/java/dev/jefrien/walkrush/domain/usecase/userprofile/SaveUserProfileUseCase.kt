package dev.jefrien.walkrush.domain.usecase.userprofile

import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.domain.repository.UserProfileRepository

class SaveUserProfileUseCase(
    private val userProfileRepository: UserProfileRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(profile: UserProfile): Result<Unit> {
        val userId = authRepository.currentUserId()
            ?: return Result.failure(IllegalStateException("No authenticated user"))

        val profileWithId = profile.copy(id = userId)
        return userProfileRepository.saveUserProfile(profileWithId)
    }
}