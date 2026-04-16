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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jefrien.walkrush.domain.model.userprofile.Gender
import dev.jefrien.walkrush.presentation.onboarding.components.NumberInputCard

@Composable
fun BasicInfoStep(
    weight: Float,
    height: Float,
    age: Int,
    gender: Gender?,
    onWeightChange: (Float) -> Unit,
    onHeightChange: (Float) -> Unit,
    onAgeChange: (Int) -> Unit,
    onGenderChange: (Gender) -> Unit,
    bmi: Float,
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
            text = "Datos básicos",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Estos nos ayudan a calcular tu IMC y calorías quemadas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Weight
        NumberInputCard(
            label = "Peso actual",
            value = weight,
            onValueChange = onWeightChange,
            unit = "kg",
            range = 30f..200f,
            steps = 340, // 1 kg steps
            quickValues = listOf(60f, 70f, 80f, 90f)
        )

        // Height
        NumberInputCard(
            label = "Altura",
            value = height,
            onValueChange = onHeightChange,
            unit = "cm",
            range = 100f..250f,
            steps = 150,
            quickValues = listOf(160f, 170f, 180f)
        )

        // Age
        NumberInputCard(
            label = "Edad",
            value = age.toFloat(),
            onValueChange = { onAgeChange(it.toInt()) },
            unit = "años",
            range = 10f..100f,
            steps = 90,
            decimals = 0,
            quickValues = listOf(20f, 30f, 40f, 50f)
        )

        // BMI Preview
        if (isValid) {
            Spacer(modifier = Modifier.height(8.dp))
            BMICard(bmi = bmi)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun BMICard(bmi: Float) {
    val (category, color) = when (bmi) {
        in 0f..18.5f -> "Bajo peso" to MaterialTheme.colorScheme.error
        in 18.5f..24.9f -> "Peso normal" to MaterialTheme.colorScheme.primary
        in 25f..29.9f -> "Sobrepeso" to MaterialTheme.colorScheme.tertiary
        else -> "Obesidad" to MaterialTheme.colorScheme.error
    }

    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Tu IMC: %.1f".format(bmi),
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
    }
}