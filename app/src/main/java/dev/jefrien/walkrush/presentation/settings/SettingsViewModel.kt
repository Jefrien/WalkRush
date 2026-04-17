package dev.jefrien.walkrush.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jefrien.walkrush.domain.model.userprofile.IntensityLevel
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.domain.repository.UserProfileRepository
import dev.jefrien.walkrush.domain.usecase.userprofile.SaveUserProfileUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val saveUserProfileUseCase: SaveUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    var weight by mutableFloatStateOf(0f)
    var targetWeight by mutableFloatStateOf(0f)
    var timelineMonths by mutableFloatStateOf(3f)
    var trainingDays by mutableStateOf<List<Int>>(emptyList())
    var intensityLevel by mutableStateOf(IntensityLevel.BEGINNER)

    private var originalProfile: UserProfile? = null

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val userId = authRepository.currentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Usuario no autenticado")
                return@launch
            }

            val profile = userProfileRepository.getUserProfile(userId)
            if (profile == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "No se encontró el perfil")
                return@launch
            }

            originalProfile = profile
            weight = profile.weightKg
            targetWeight = profile.targetWeightKg
            timelineMonths = profile.timelineMonths.toFloat()
            trainingDays = profile.trainingDays
            intensityLevel = profile.intensityLevel

            _uiState.value = SettingsUiState(isLoading = false)
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            val profile = originalProfile
            if (profile == null) {
                _uiState.value = _uiState.value.copy(error = "Perfil no cargado")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            val updated = profile.copy(
                weightKg = weight,
                targetWeightKg = targetWeight,
                timelineMonths = timelineMonths.toInt(),
                trainingDays = trainingDays.sorted(),
                daysPerWeek = trainingDays.size,
                intensityLevel = intensityLevel
            )

            saveUserProfileUseCase(updated)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _events.emit(SettingsEvent.Saved)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Error guardando cambios"
                    )
                }
        }
    }

    data class SettingsUiState(
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val error: String? = null
    )

    sealed class SettingsEvent {
        object Saved : SettingsEvent()
    }
}
