package dev.jefrien.walkrush.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jefrien.walkrush.domain.model.auth.User
import dev.jefrien.walkrush.domain.model.routine.Routine
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.domain.repository.HealthConnectRepository
import dev.jefrien.walkrush.domain.repository.RoutineRepository
import dev.jefrien.walkrush.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val routineRepository: RoutineRepository,
    private val healthConnectRepository: HealthConnectRepository
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
                val hcAvailable = healthConnectRepository.isAvailable()
                val hcConnected = hcAvailable && healthConnectRepository.hasPermissions()

                _uiState.value = HomeUiState(
                    isLoading = false,
                    user = user,
                    profile = profile,
                    activeRoutine = activeRoutine,
                    dailyWorkouts = buildDailyWorkouts(activeRoutine, profile),
                    healthConnectConnected = hcConnected
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

    private fun buildDailyWorkouts(
        routine: Routine?,
        profile: UserProfile?
    ): List<DailyWorkoutItem> {
        if (routine == null) return emptyList()

        val zone = ZoneId.systemDefault()
        val startDate = Instant.ofEpochMilli(routine.generatedAt)
            .atZone(zone)
            .toLocalDate()

        val trainingDays = profile?.trainingDays?.takeIf { it.isNotEmpty() }
            ?: (1..7).toList()

        val today = LocalDate.now(zone)
        val allSessions = routine.weeklyPlans
            .sortedBy { it.weekNumber }
            .flatMap { week -> week.sessions.sortedBy { it.dayOfWeek } }

        if (allSessions.isEmpty()) return emptyList()

        val estimatedDays = (allSessions.size.toDouble() * 7 / trainingDays.size).toLong() + 2
        val lastSessionDate = startDate.plusDays(estimatedDays)
        val endDate = maxOf(today.plusDays(3), lastSessionDate)

        val sessionIterator = allSessions.iterator()
        val items = mutableListOf<DailyWorkoutItem>()

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dayOfWeek = currentDate.dayOfWeek.value
            if (dayOfWeek in trainingDays) {
                if (sessionIterator.hasNext()) {
                    val session = sessionIterator.next()
                    val status = when {
                        session.isCompleted -> DailyWorkoutStatus.COMPLETED
                        currentDate.isBefore(today) -> DailyWorkoutStatus.MISSED
                        else -> DailyWorkoutStatus.UPCOMING
                    }
                    items.add(DailyWorkoutItem.TrainingDay(currentDate, session, status))
                } else {
                    items.add(DailyWorkoutItem.RestDay(currentDate))
                }
            } else {
                items.add(DailyWorkoutItem.RestDay(currentDate))
            }
            currentDate = currentDate.plusDays(1)
        }

        return items
    }

    data class HomeUiState(
        val isLoading: Boolean = true,
        val user: User? = null,
        val profile: UserProfile? = null,
        val activeRoutine: Routine? = null,
        val dailyWorkouts: List<DailyWorkoutItem> = emptyList(),
        val isGeneratingRoutine: Boolean = false,
        val generationError: String? = null,
        val healthConnectConnected: Boolean = false
    )

    sealed class HomeEvent {
        object NavigateToAuth : HomeEvent()
        object NavigateToProfile : HomeEvent()
        object NavigateToHistory : HomeEvent()
        data class StartWorkout(val sessionId: String) : HomeEvent()
    }
}

sealed class DailyWorkoutItem {
    abstract val date: LocalDate

    data class RestDay(override val date: LocalDate) : DailyWorkoutItem()
    data class TrainingDay(
        override val date: LocalDate,
        val session: WorkoutSession,
        val status: DailyWorkoutStatus
    ) : DailyWorkoutItem()
}

enum class DailyWorkoutStatus {
    UPCOMING,
    MISSED,
    COMPLETED
}

fun LocalDate.formatHomeDate(): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.of("es", "ES"))
    return this.format(formatter).replaceFirstChar { it.uppercase() }
}

fun LocalDate.formatShortWeekday(): String {
    val formatter = DateTimeFormatter.ofPattern("EEE", Locale.of("es", "ES"))
    return this.format(formatter).replaceFirstChar { it.uppercase() }
}
