package dev.jefrien.walkrush.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jefrien.walkrush.data.manager.HealthDataManager
import dev.jefrien.walkrush.domain.model.auth.User
import dev.jefrien.walkrush.domain.model.routine.Routine
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.domain.repository.RoutineRepository
import dev.jefrien.walkrush.domain.repository.UserProfileRepository
import dev.jefrien.walkrush.presentation.common.DailyWorkoutItem
import dev.jefrien.walkrush.presentation.common.buildDailyWorkouts
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val routineRepository: RoutineRepository,
    private val healthDataManager: HealthDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _events.emit(HomeEvent.NavigateToAuth)
                    return@launch
                }

                val user = authRepository.currentUser.first()
                val profile = userProfileRepository.getUserProfile(userId)
                val activeRoutine = routineRepository.getActiveRoutine(userId)
                val hcAvailable = healthDataManager.isAvailable()
                val hcConnected = hcAvailable && healthDataManager.hasPermissions()

                _uiState.value = HomeUiState(
                    isLoading = false,
                    user = user,
                    profile = profile,
                    activeRoutine = activeRoutine,
                    dailyWorkouts = buildDailyWorkouts(activeRoutine, profile),
                    healthConnectConnected = hcConnected,
                    activeDataSource = healthDataManager.activeSourceType.displayName
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun refresh() {
        loadUserData()
    }

    fun generateRoutine() {
        viewModelScope.launch {
            val profile = _uiState.value.profile
            if (profile == null) {
                _uiState.value = _uiState.value.copy(generationError = "No hay perfil disponible")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isGeneratingRoutine = true, generationError = null)

            routineRepository.generateAndSaveRoutine(profile)
                .onSuccess { routine ->
                    _uiState.value = _uiState.value.copy(
                        isGeneratingRoutine = false,
                        activeRoutine = routine,
                        dailyWorkouts = buildDailyWorkouts(routine, profile)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isGeneratingRoutine = false,
                        generationError = error.message ?: "Error generando rutina"
                    )
                }
        }
    }

    fun startWorkout(sessionId: String) {
        viewModelScope.launch {
            _events.emit(HomeEvent.StartWorkout(sessionId))
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _events.emit(HomeEvent.NavigateToAuth)
        }
    }

    data class HomeUiState(
        val isLoading: Boolean = true,
        val user: User? = null,
        val profile: UserProfile? = null,
        val activeRoutine: Routine? = null,
        val dailyWorkouts: List<DailyWorkoutItem> = emptyList(),
        val isGeneratingRoutine: Boolean = false,
        val generationError: String? = null,
        val healthConnectConnected: Boolean = false,
        val activeDataSource: String = ""
    )

    sealed class HomeEvent {
        object NavigateToAuth : HomeEvent()
        object NavigateToProfile : HomeEvent()
        object NavigateToHistory : HomeEvent()
        data class StartWorkout(val sessionId: String) : HomeEvent()
    }
}
