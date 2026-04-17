package dev.jefrien.walkrush.presentation.postworkout

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jefrien.walkrush.data.healthconnect.HealthConnectRepositoryImpl
import org.koin.androidx.compose.koinViewModel

@Composable
fun PostWorkoutScreen(
    sessionId: String,
    onNavigateHome: () -> Unit,
    viewModel: PostWorkoutViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = HealthConnectRepositoryImpl.createPermissionContract()
    ) { granted ->
        val allGranted = HealthConnectRepositoryImpl.getAllPermissions().all { it in granted }
        if (allGranted) {
            viewModel.syncWithHealthConnectAfterPermissions()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSession(sessionId)
        viewModel.events.collect { event ->
            when (event) {
                is PostWorkoutViewModel.PostWorkoutEvent.NavigateHome -> onNavigateHome()
                is PostWorkoutViewModel.PostWorkoutEvent.RequestHealthConnectPermissions -> {
                    permissionsLauncher.launch(HealthConnectRepositoryImpl.getAllPermissions())
                }
            }
        }
    }

    LaunchedEffect(uiState.value.healthConnectSyncSuccess) {
        if (uiState.value.healthConnectSyncSuccess == true) {
            snackbarHostState.showSnackbar("Sincronizado con Health Connect")
        } else if (uiState.value.healthConnectSyncSuccess == false && uiState.value.healthConnectError != null) {
            snackbarHostState.showSnackbar("Health Connect: ${uiState.value.healthConnectError}")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.value.isLoading) {
                CircularProgressIndicator()
            } else {
                val session = uiState.value.session

                // Celebration icon
                val scale = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    scale.animateTo(1f, animationSpec = tween(500))
                }

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale.value),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "¡Sesión completada!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Excelente trabajo, sigue así",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Stats card
                if (session != null) {
                    val durationMinutes = session.phases.sumOf { it.durationSeconds } / 60
                    val calories = session.actualCalories ?: session.estimatedCalories

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatColumn(
                                icon = Icons.Default.Schedule,
                                value = "$durationMinutes",
                                label = "Minutos"
                            )
                            StatColumn(
                                icon = Icons.Default.LocalFireDepartment,
                                value = "$calories",
                                label = "Kcal"
                            )
                            StatColumn(
                                icon = Icons.Default.CheckCircle,
                                value = "${session.phases.size}",
                                label = "Fases"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Health Connect permission hint
                if (uiState.value.healthConnectAvailable && uiState.value.healthConnectPermissionsNeeded) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Sincronizar con Health Connect",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Permite guardar tu actividad en la app de Salud de Google",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.requestHealthConnectPermissions() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Conceder permisos")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Rating
                Text(
                    text = "¿Cómo te sentiste?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                StarRating(
                    rating = uiState.value.rating ?: 0,
                    onRatingChange = viewModel::setRating
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = viewModel::saveRatingAndFinish,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.value.isSaving
                ) {
                    if (uiState.value.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar y continuar", fontSize = 17.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = viewModel::skipRating,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.value.isSaving
                ) {
                    Text("Omitir", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun StarRating(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    maxStars: Int = 5
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        (1..maxStars).forEach { index ->
            val isSelected = index <= rating
            IconButton(
                onClick = { onRatingChange(index) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "$index estrellas",
                    modifier = Modifier.size(40.dp),
                    tint = if (isSelected) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
