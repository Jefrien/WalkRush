package dev.jefrien.walkrush.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jefrien.walkrush.domain.model.routine.Routine
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import dev.jefrien.walkrush.domain.repository.AuthRepository
import dev.jefrien.walkrush.domain.repository.RoutineRepository
import dev.jefrien.walkrush.domain.repository.UserProfileRepository
import dev.jefrien.walkrush.presentation.common.DailyWorkoutItem
import dev.jefrien.walkrush.presentation.common.buildDailyWorkouts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val routineRepository: RoutineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }

                val profile = userProfileRepository.getUserProfile(userId)
                val activeRoutine = routineRepository.getActiveRoutine(userId)
                val workouts = buildDailyWorkouts(activeRoutine, profile)

                _uiState.value = CalendarUiState(
                    isLoading = false,
                    currentMonth = YearMonth.now(),
                    workouts = workouts,
                    activeRoutine = activeRoutine,
                    profile = profile
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun previousMonth() {
        _uiState.value = _uiState.value.copy(
            currentMonth = _uiState.value.currentMonth.minusMonths(1)
        )
    }

    fun nextMonth() {
        _uiState.value = _uiState.value.copy(
            currentMonth = _uiState.value.currentMonth.plusMonths(1)
        )
    }

    data class CalendarUiState(
        val isLoading: Boolean = true,
        val currentMonth: YearMonth = YearMonth.now(),
        val workouts: List<DailyWorkoutItem> = emptyList(),
        val activeRoutine: Routine? = null,
        val profile: UserProfile? = null
    ) {
        fun dayStatus(date: LocalDate): CalendarDayStatus? {
            val item = workouts.find { it.date == date }
            return when (item) {
                is DailyWorkoutItem.RestDay -> CalendarDayStatus.REST
                is DailyWorkoutItem.TrainingDay -> when (item.status) {
                    dev.jefrien.walkrush.presentation.common.DailyWorkoutStatus.COMPLETED -> CalendarDayStatus.COMPLETED
                    dev.jefrien.walkrush.presentation.common.DailyWorkoutStatus.MISSED -> CalendarDayStatus.MISSED
                    dev.jefrien.walkrush.presentation.common.DailyWorkoutStatus.UPCOMING -> CalendarDayStatus.UPCOMING
                }
                null -> null
            }
        }
    }
}

enum class CalendarDayStatus {
    COMPLETED,
    MISSED,
    UPCOMING,
    REST
}
