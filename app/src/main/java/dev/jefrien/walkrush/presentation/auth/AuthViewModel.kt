package dev.jefrien.walkrush.presentation.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jefrien.walkrush.domain.model.auth.AuthResult
import dev.jefrien.walkrush.domain.repository.UserProfileRepository
import dev.jefrien.walkrush.domain.usecase.auth.SignInUseCase
import dev.jefrien.walkrush.domain.usecase.auth.SignOutUseCase
import dev.jefrien.walkrush.domain.usecase.auth.SignUpUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication screens
 */
class AuthViewModel(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // One-time events (snackbar, navigation)
    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    // Input fields
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var confirmPassword by mutableStateOf("")
        private set
    var isSignUp by mutableStateOf(false)
        private set

    fun onEmailChange(value: String) {
        email = value
        clearError()
    }

    fun onPasswordChange(value: String) {
        password = value
        clearError()
    }

    fun onConfirmPasswordChange(value: String) {
        confirmPassword = value
        clearError()
    }

    fun toggleMode() {
        isSignUp = !isSignUp
        clearError()
    }

    fun submit() {
        if (isSignUp) {
            signUp()
        } else {
            signIn()
        }
    }

    private fun signIn() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = signInUseCase(email, password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)

                    // Verificar si tiene perfil
                    val userId = result.data.id
                    val hasProfile = userProfileRepository.hasCompletedOnboarding(userId)

                    if (hasProfile) {
                        _events.emit(AuthEvent.NavigateToHome)
                    } else {
                        _events.emit(AuthEvent.NavigateToOnboarding)
                    }
                }

                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exception.message
                    )
                }

                else -> {}
            }
        }
    }

    private fun signUp() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = signUpUseCase(email, password, confirmPassword)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(AuthEvent.ShowSuccess("Cuenta creada. Verifica tu email."))
                    _events.emit(AuthEvent.NavigateToOnboarding)
                }

                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exception.message
                    )
                }

                else -> {}
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _events.emit(AuthEvent.NavigateToAuth)
        }
    }

    private fun clearError() {
        if (_uiState.value.error != null) {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

    data class AuthUiState(
        val isLoading: Boolean = false,
        val error: String? = null
    )

    sealed class AuthEvent {
        data class ShowError(val message: String) : AuthEvent()
        data class ShowSuccess(val message: String) : AuthEvent()
        object NavigateToHome : AuthEvent()
        object NavigateToAuth : AuthEvent()
        object NavigateToOnboarding : AuthEvent()
    }
}