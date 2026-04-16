package dev.jefrien.walkrush.presentation.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jefrien.walkrush.presentation.onboarding.components.NumberInputCard

@Composable
fun TreadmillStep(
    hasIncline: Boolean,
    maxInclinePercent: Float,
    maxSpeedKmh: Float,
    onHasInclineChange: (Boolean) -> Unit,
    onMaxInclineChange: (Float) -> Unit,
    onMaxSpeedChange: (Float) -> Unit,
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
            text = "Tu caminadora",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Esto afecta cómo generamos tus rutinas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Incline support question (CRITICAL)
        InclineQuestionCard(
            hasIncline = hasIncline,
            onAnswer = onHasInclineChange
        )

        // Only show incline settings if supported
        if (hasIncline) {
            Spacer(modifier = Modifier.height(8.dp))

            NumberInputCard(
                label = "Inclinación máxima",
                value = maxInclinePercent,
                onValueChange = onMaxInclineChange,
                unit = "%",
                range = 1f..15f,
                steps = 28, // 0.5% steps
                quickValues = listOf(5f, 10f, 12f, 15f)
            )

            Text(
                text = "💡 Con inclinación quemas más calorías y fortaleces piernas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "✓ Entendido. Tus rutinas serán planas (sin inclinación). " +
                            "Aún así podrás quemar calorías y mejorar tu resistencia.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Max speed (optional)
        NumberInputCard(
            label = "Velocidad máxima de tu caminadora (opcional)",
            value = maxSpeedKmh,
            onValueChange = onMaxSpeedChange,
            unit = "km/h",
            range = 8f..20f,
            steps = 24,
            quickValues = listOf(10f, 12f, 14f, 16f)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InclineQuestionCard(
    hasIncline: Boolean,
    onAnswer: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "¿Tu caminadora tiene inclinación?",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Yes option
                InclineOption(
                    selected = hasIncline,
                    onClick = { onAnswer(true) },
                    icon = Icons.Default.Check,
                    label = "Sí, tiene"
                )

                // No option
                InclineOption(
                    selected = !hasIncline,
                    onClick = { onAnswer(false) },
                    icon = Icons.Default.Close,
                    label = "No tiene"
                )
            }
        }
    }
}

@Composable
private fun InclineOption(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                color = if (selected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}