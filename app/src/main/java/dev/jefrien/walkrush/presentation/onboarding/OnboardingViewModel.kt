package dev.jefrien.walkrush.presentation.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jefrien.walkrush.domain.model.userprofile.*
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.domain.usecase.userprofile.SaveUserProfileUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // Events
    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    // Current step
    var currentStep by mutableIntStateOf(0)
        private set

    // Form data - Basic Info
    var weight by mutableFloatStateOf(0f)
    var height by mutableFloatStateOf(0f)
    var age by mutableIntStateOf(0)
    var gender by mutableStateOf<Gender?>(null)

    // Form data - Goals
    var goalType by mutableStateOf(GoalType.WEIGHT_LOSS)
    var targetWeight by mutableFloatStateOf(0f)
    var timelineMonths by mutableIntStateOf(3)

    // Form data - Schedule
    var daysPerWeek by mutableIntStateOf(3)
    var intensityLevel by mutableStateOf(IntensityLevel.BEGINNER)

    // Form data - Treadmill (NEW)
    var hasIncline by mutableStateOf(true)
    var maxInclinePercent by mutableFloatStateOf(12f)
    var maxSpeedKmh by mutableFloatStateOf(12f)

    // Computed
    val isBasicInfoValid: Boolean
        get() = weight > 30 && weight < 200 &&
                height > 100 && height < 250 &&
                age > 10 && age < 100

    val isGoalsValid: Boolean
        get() = targetWeight > 0 && targetWeight < 200 &&
                timelineMonths in 1..24 &&
                targetWeight != weight

    val isScheduleValid: Boolean
        get() = daysPerWeek in 1..7

    val isTreadmillValid: Boolean
        get() = if (hasIncline) maxInclinePercent > 0 else true

    val canProceed: Boolean
        get() = when (currentStep) {
            0 -> true // Welcome
            1 -> isBasicInfoValid
            2 -> isGoalsValid
            3 -> isScheduleValid
            4 -> isTreadmillValid
            else -> false
        }

    fun nextStep() {
        if (currentStep < 5) {
            currentStep++
        }
    }

    fun previousStep() {
        if (currentStep > 0) {
            currentStep--
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val userId = authRepository.currentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No hay usuario autenticado"
                )
                return@launch
            }

            val profile = UserProfile(
                id = userId,
                weightKg = weight,
                heightCm = height,
                age = age,
                gender = gender,
                fitnessGoal = FitnessGoal(goalType, ""),
                targetWeightKg = targetWeight,
                timelineMonths = timelineMonths,
                daysPerWeek = daysPerWeek,
                intensityLevel = intensityLevel,
                treadmillCapabilities = TreadmillCapabilities(
                    hasIncline = hasIncline,
                    maxInclinePercent = if (hasIncline) maxInclinePercent else 0f,
                    maxSpeedKmh = maxSpeedKmh
                )
            )

            saveUserProfileUseCase(profile)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(OnboardingEvent.OnboardingComplete)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error guardando perfil"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // BMI calculation for display
    val bmi: Float
        get() = if (height > 0) weight / ((height / 100) * (height / 100)) else 0f

    data class OnboardingUiState(
        val isLoading: Boolean = false,
        val error: String? = null
    )

    sealed class OnboardingEvent {
        object OnboardingComplete : OnboardingEvent()
        data class ShowError(val message: String) : OnboardingEvent()
    }
}