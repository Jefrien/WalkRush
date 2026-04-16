package dev.jefrien.walkrush.presentation.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jefrien.walkrush.domain.model.userprofile.GoalType
import dev.jefrien.walkrush.presentation.onboarding.components.NumberInputCard

@Composable
fun GoalsStep(
    currentWeight: Float,
    targetWeight: Float,
    timelineMonths: Int,
    goalType: GoalType,
    onTargetWeightChange: (Float) -> Unit,
    onTimelineChange: (Int) -> Unit,
    onGoalTypeChange: (GoalType) -> Unit,
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tu meta",
            style = MaterialTheme.typography.headlineSmall
        )

        // Goal type selector
        GoalTypeSelector(
            selected = goalType,
            onSelect = onGoalTypeChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Target weight (only for weight loss)
        if (goalType == GoalType.WEIGHT_LOSS) {
            NumberInputCard(
                label = "Peso objetivo",
                value = targetWeight,
                onValueChange = onTargetWeightChange,
                unit = "kg",
                range = 30f..currentWeight,
                steps = ((currentWeight - 30) * 2).toInt(),
                quickValues = listOf(
                    currentWeight - 5f,
                    currentWeight - 10f,
                    currentWeight - 15f
                ).filter { it > 30 }
            )

            val weightToLose = currentWeight - targetWeight
            if (weightToLose > 0) {
                Text(
                    text = "Meta: perder %.1f kg".format(weightToLose),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Timeline
        NumberInputCard(
            label = "¿En cuánto tiempo?",
            value = timelineMonths.toFloat(),
            onValueChange = { onTimelineChange(it.toInt()) },
            unit = "meses",
            range = 1f..24f,
            steps = 23,
            decimals = 0,
            quickValues = listOf(1f, 3f, 6f, 12f)
        )

        // Validation message
        if (!isValid && targetWeight > 0) {
            Text(
                text = "El peso objetivo debe ser menor al actual",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun GoalTypeSelector(
    selected: GoalType,
    onSelect: (GoalType) -> Unit
) {
    val options = listOf(
        GoalType.WEIGHT_LOSS to "Perder peso",
        GoalType.MAINTENANCE to "Mantener fitness",
        GoalType.ENDURANCE to "Mejorar resistencia",
        GoalType.CARDIO_HEALTH to "Salud cardiovascular"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (type, label) ->
            val isSelected = type == selected
            Card(
                onClick = { onSelect(type) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}