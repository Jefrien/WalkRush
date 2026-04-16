package dev.jefrien.walkrush.presentation.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jefrien.walkrush.domain.model.userprofile.IntensityLevel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleStep(
    daysPerWeek: Int,
    intensityLevel: IntensityLevel,
    onDaysChange: (Int) -> Unit,
    onIntensityChange: (IntensityLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Horario de entrenamiento",
            style = MaterialTheme.typography.headlineSmall
        )

        // Days per week
        Text(
            text = "¿Cuántos días a la semana?",
            style = MaterialTheme.typography.titleMedium
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..7).forEach { day ->
                DayChip(
                    day = day,
                    isSelected = day == daysPerWeek,
                    onClick = { onDaysChange(day) }
                )
            }
        }

        // Intensity level
        Text(
            text = "Nivel de intensidad",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        val intensities = listOf(
            IntensityLevel.BEGINNER to "Principiante\n(20-30 min)",
            IntensityLevel.INTERMEDIATE to "Intermedio\n(30-45 min)",
            IntensityLevel.INTENSE to "Intenso\n(45-60 min)"
        )

        intensities.forEach { (level, label) ->
            IntensityCard(
                level = level,
                label = label,
                isSelected = level == intensityLevel,
                onClick = { onIntensityChange(level) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DayChip(
    day: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(day.toString()) }
    )
}

@Composable
private fun IntensityCard(
    level: IntensityLevel,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = when (level) {
        IntensityLevel.BEGINNER -> MaterialTheme.colorScheme.primary
        IntensityLevel.INTERMEDIATE -> MaterialTheme.colorScheme.tertiary
        IntensityLevel.INTENSE -> MaterialTheme.colorScheme.error
    }

    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected)
                color.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}