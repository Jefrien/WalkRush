package dev.jefrien.walkrush.domain.repository

import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>
    suspend fun getUserProfile(userId: String): UserProfile?
    fun observeUserProfile(userId: String): Flow<UserProfile?>
    suspend fun hasCompletedOnboarding(userId: String): Boolean
    suspend fun markOnboardingCompleted(userId: String)
}