package dev.jefrien.walkrush.presentation.activeworkout

import android.app.Activity
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jefrien.walkrush.data.healthconnect.HealthConnectStatus
import dev.jefrien.walkrush.domain.model.routine.PhaseType
import dev.jefrien.walkrush.presentation.common.TtsCoach
import org.koin.androidx.compose.koinViewModel

@Composable
fun ActiveWorkoutScreen(
    sessionId: String,
    onWorkoutComplete: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ActiveWorkoutViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    val toneGenerator = remember { ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100) }
    val ttsCoach = remember { TtsCoach(context) }

    SideEffect {
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    DisposableEffect(Unit) {
        viewModel.loadSession(sessionId)
        onDispose {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            toneGenerator.release()
            ttsCoach.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ActiveWorkoutViewModel.Event.NavigateBack -> {
                    ttsCoach.stop()
                    onCancel()
                }
                is ActiveWorkoutViewModel.Event.WorkoutComplete,
                is ActiveWorkoutViewModel.Event.WorkoutAutoComplete -> {
                    ttsCoach.announceWorkoutComplete()
                    toneGenerator.startTone(ToneGenerator.TONE_SUP_RINGTONE, 900)
                    vibrate(context, 500)
                    onWorkoutComplete()
                }
                is ActiveWorkoutViewModel.Event.PhaseChanged -> {
                    ttsCoach.announcePhaseStart(event.phase)
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 350)
                    vibrate(context, 200)
                }
                is ActiveWorkoutViewModel.Event.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    LaunchedEffect(uiState.value.isRunning) {
        if (uiState.value.isRunning) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
        }
    }

    // Announce first phase once session loads and user starts running
    var hasAnnouncedStart by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.value.isLoading, uiState.value.isRunning) {
        if (!uiState.value.isLoading && uiState.value.isRunning && !hasAnnouncedStart) {
            hasAnnouncedStart = true
            uiState.value.currentPhase?.let { ttsCoach.announcePhaseStart(it) }
        }
    }

    BackHandler {
        if (!uiState.value.isCompleted) showCancelDialog = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.value.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Top progress bar
                TotalProgressBar(
                    progress = uiState.value.progress,
                    phaseName = uiState.value.currentPhase?.title ?: "",
                    remainingTotal = uiState.value.formattedTotalTime
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT: Phase timer big + Health Connect data
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        PhaseTimerPanel(
                            phaseTime = uiState.value.formattedPhaseTime,
                            phaseProgress = uiState.value.phaseProgress,
                            isRunning = uiState.value.isRunning,
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.value.healthConnectAvailable) {
                            HealthConnectStats(
                                data = uiState.value.healthConnectData,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    // CENTER: Phase info
                    PhaseInfoPanel(
                        phase = uiState.value.currentPhase,
                        modifier = Modifier.weight(1.2f)
                    )

                    // RIGHT: Controls
                    ControlsPanel(
                        isRunning = uiState.value.isRunning,
                        onPlayPause = {
                            viewModel.togglePauseResume()
                            vibrate(context, 40)
                        },
                        onSkip = {
                            viewModel.skipPhase()
                            vibrate(context, 60)
                        },
                        onFinish = { showFinishDialog = true },
                        onCancel = { showCancelDialog = true },
                        canSkip = uiState.value.currentPhaseIndex < (uiState.value.session?.phases?.size ?: 0) - 1,
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("¿Cancelar entrenamiento?") },
            text = { Text("Perderás el progreso de esta sesión.") },
            confirmButton = {
                TextButton(onClick = { showCancelDialog = false; viewModel.confirmCancel() }) {
                    Text("Sí, cancelar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Continuar") }
            }
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("¿Finalizar sesión?") },
            text = { Text("Guardaremos tu progreso en Supabase.") },
            confirmButton = {
                Button(
                    onClick = { showFinishDialog = false; viewModel.finishWorkout() },
                    enabled = !uiState.value.isSaving
                ) {
                    if (uiState.value.isSaving) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar y finalizar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Seguir") }
            }
        )
    }
}

@Composable
private fun TotalProgressBar(
    progress: Float,
    phaseName: String,
    remainingTotal: String
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "total_progress"
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = phaseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Restante: $remainingTotal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = ProgressIndicatorDefaults.linearTrackColor,
        )
    }
}

@Composable
private fun PhaseTimerPanel(
    phaseTime: String,
    phaseProgress: Float,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = phaseProgress,
        animationSpec = tween(1000),
        label = "phase_progress"
    )

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(200.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 14.dp,
                trackColor = ProgressIndicatorDefaults.circularColor.copy(alpha = 0.08f),
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(200.dp),
                color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                strokeWidth = 14.dp,
                trackColor = ProgressIndicatorDefaults.circularColor.copy(alpha = 0.08f),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(targetState = phaseTime, label = "phase_timer") { time ->
                    Text(
                        text = time,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (isRunning) "En marcha" else "Pausado",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PhaseInfoPanel(
    phase: dev.jefrien.walkrush.domain.model.routine.WorkoutPhase?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        phase?.let {
            val phaseColor = when (it.type) {
                PhaseType.WARM_UP -> MaterialTheme.colorScheme.tertiary
                PhaseType.WALK -> MaterialTheme.colorScheme.primary
                PhaseType.RUN -> MaterialTheme.colorScheme.error
                PhaseType.RECOVERY -> MaterialTheme.colorScheme.secondary
                PhaseType.COOL_DOWN -> MaterialTheme.colorScheme.tertiary
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(phaseColor.copy(alpha = 0.15f))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = it.title.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = phaseColor,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPill(label = "Velocidad", value = "${it.targetSpeedKmh} km/h")
                if ((it.targetInclinePercent ?: 0f) > 0f) {
                    StatPill(label = "Inclinación", value = "${it.targetInclinePercent?.toInt()}%")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = it.notes ?: "Mantén la postura erguida y respira de forma controlada.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ControlsPanel(
    isRunning: Boolean,
    onPlayPause: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    canSkip: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Big Play/Pause
        Button(
            onClick = onPlayPause,
            modifier = Modifier.size(90.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pausar" else "Continuar",
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Skip phase
        FilledTonalButton(
            onClick = onSkip,
            enabled = canSkip,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Icon(Icons.Default.FastForward, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Saltar fase")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Finish
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(0.85f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Stop, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Finalizar")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cancel
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Cancelar")
        }
    }
}

@Composable
private fun HealthConnectStats(
    data: dev.jefrien.walkrush.domain.repository.HealthSessionData?,
    modifier: Modifier = Modifier
) {
    val status = data?.healthConnectStatus
    val hasAnyData = data?.heartRateBpm != null || data?.caloriesBurned != null || data?.steps != null

    if (status == HealthConnectStatus.PERMISSIONS_MISSING) {
        Text(
            text = "Permisos de Health Connect requeridos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
            modifier = modifier
        )
        return
    }

    if (status == HealthConnectStatus.NOT_AVAILABLE) {
        Text(
            text = "Health Connect no disponible",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = modifier
        )
        return
    }

    if (!hasAnyData) {
        Text(
            text = "Sincronizando con reloj...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = modifier
        )
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        data.heartRateBpm?.let { bpm ->
            HealthStatPill(
                icon = androidx.compose.material.icons.Icons.Default.Favorite,
                value = "$bpm",
                unit = "bpm",
                tint = MaterialTheme.colorScheme.error
            )
        }
        data.caloriesBurned?.let { kcal ->
            HealthStatPill(
                icon = androidx.compose.material.icons.Icons.Default.LocalFireDepartment,
                value = "%.0f".format(kcal),
                unit = "kcal",
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
        data.steps?.let { steps ->
            HealthStatPill(
                icon = androidx.compose.material.icons.Icons.Default.DirectionsWalk,
                value = if (steps >= 1000) "%.1fk".format(steps / 1000.0) else "$steps",
                unit = "pasos",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun HealthStatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    unit: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tint
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = tint.copy(alpha = 0.9f)
        )
    }
}

private fun vibrate(context: android.content.Context, millis: Long) {
    val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(millis)
    }
}
