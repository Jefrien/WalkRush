package dev.jefrien.walkrush.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jefrien.walkrush.domain.model.userprofile.IntensityLevel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsViewModel.SettingsEvent.Saved -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.value.isLoading) {
                Spacer(modifier = Modifier.height(64.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Spacer(modifier = Modifier.height(16.dp))

                // Weight
                OutlinedTextField(
                    value = if (viewModel.weight > 0) "%.1f".format(viewModel.weight) else "",
                    onValueChange = { viewModel.weight = it.toFloatOrNull() ?: 0f },
                    label = { Text("Peso actual (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Target weight
                OutlinedTextField(
                    value = if (viewModel.targetWeight > 0) "%.1f".format(viewModel.targetWeight) else "",
                    onValueChange = { viewModel.targetWeight = it.toFloatOrNull() ?: 0f },
                    label = { Text("Peso objetivo (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Timeline
                Text(
                    text = "Timeline: ${viewModel.timelineMonths.toInt()} meses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = viewModel.timelineMonths,
                    onValueChange = { viewModel.timelineMonths = it },
                    valueRange = 1f..24f,
                    steps = 22,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Training days
                Text(
                    text = "Días de entrenamiento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                TrainingDaysSelector(
                    selectedDays = viewModel.trainingDays,
                    onToggle = { day ->
                        viewModel.trainingDays = if (day in viewModel.trainingDays) {
                            viewModel.trainingDays - day
                        } else {
                            viewModel.trainingDays + day
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Intensity
                Text(
                    text = "Nivel de intensidad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                IntensitySelector(
                    selected = viewModel.intensityLevel,
                    onSelect = { viewModel.intensityLevel = it }
                )

                Spacer(modifier = Modifier.height(32.dp))

                uiState.value.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = viewModel::saveChanges,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.value.isSaving && viewModel.trainingDays.isNotEmpty()
                ) {
                    if (uiState.value.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar cambios")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainingDaysSelector(
    selectedDays: List<Int>,
    onToggle: (Int) -> Unit
) {
    val dayNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dayNames.forEachIndexed { index, name ->
            val dayNumber = index + 1
            FilterChip(
                selected = dayNumber in selectedDays,
                onClick = { onToggle(dayNumber) },
                label = { Text(name) }
            )
        }
    }
}

@Composable
private fun IntensitySelector(
    selected: IntensityLevel,
    onSelect: (IntensityLevel) -> Unit
) {
    val options = listOf(
        IntensityLevel.BEGINNER to "Principiante",
        IntensityLevel.INTERMEDIATE to "Intermedio",
        IntensityLevel.INTENSE to "Intenso"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (level, label) ->
            val color = when (level) {
                IntensityLevel.BEGINNER -> MaterialTheme.colorScheme.primary
                IntensityLevel.INTERMEDIATE -> MaterialTheme.colorScheme.tertiary
                IntensityLevel.INTENSE -> MaterialTheme.colorScheme.error
            }
            Card(
                onClick = { onSelect(level) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected == level)
                        color.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected == level) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
