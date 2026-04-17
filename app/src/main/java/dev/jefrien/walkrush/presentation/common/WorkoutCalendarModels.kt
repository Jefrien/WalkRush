package dev.jefrien.walkrush.presentation.common

import dev.jefrien.walkrush.domain.model.routine.Routine
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

fun buildDailyWorkouts(
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

fun LocalDate.formatHomeDate(): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.of("es", "ES"))
    return this.format(formatter).replaceFirstChar { it.uppercase() }
}

fun LocalDate.formatShortWeekday(): String {
    val formatter = DateTimeFormatter.ofPattern("EEE", Locale.of("es", "ES"))
    return this.format(formatter).replaceFirstChar { it.uppercase() }
}
