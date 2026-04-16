package dev.jefrien.walkrush.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jefrien.walkrush.domain.model.auth.User
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val userId = authRepository.currentUserId()
            if (userId == null) {
                _events.emit(ProfileEvent.NavigateToAuth)
                return@launch
            }

            val user = authRepository.currentUser
                .let { flow ->
                    var value: User? = null
                    flow.collect { value = it }
                    value
                }

            val profile = userProfileRepository.getUserProfile(userId)

            _uiState.value = ProfileUiState(
                isLoading = false,
                user = user,
                profile = profile
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _events.emit(ProfileEvent.NavigateToAuth)
        }
    }

    data class ProfileUiState(
        val isLoading: Boolean = true,
        val user: User? = null,
        val profile: UserProfile? = null
    )

    sealed class ProfileEvent {
        object NavigateToAuth : ProfileEvent()
    }
}
