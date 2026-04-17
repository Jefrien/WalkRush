package dev.jefrien.walkrush.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jefrien.walkrush.domain.model.routine.PhaseType
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.model.userprofile.IntensityLevel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onStartWorkout: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    var selectedSession by remember { mutableStateOf<WorkoutSession?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeViewModel.HomeEvent.NavigateToAuth -> onSignOut()
                is HomeViewModel.HomeEvent.NavigateToProfile -> onNavigateToProfile()
                is HomeViewModel.HomeEvent.NavigateToHistory -> onNavigateToHistory()
                is HomeViewModel.HomeEvent.StartWorkout -> onStartWorkout(event.sessionId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WalkRush") },
                actions = {
                    OutlinedButton(
                        onClick = viewModel::signOut,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Salir")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.value.isLoading) {
                Spacer(modifier = Modifier.height(64.dp))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Cargando tu perfil...")
            } else {
                val user = uiState.value.user
                val profile = uiState.value.profile
                val dailyWorkouts = uiState.value.dailyWorkouts
                val activeRoutine = uiState.value.activeRoutine

                Spacer(modifier = Modifier.height(16.dp))

                // Saludo
                Text(
                    text = "¡Hola${user?.email?.let { ", ${it.substringBefore("@")}" } ?: ""}!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Listo para caminar hoy",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (uiState.value.healthConnectConnected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Health Connect",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Resumen de perfil
                if (profile != null) {
                    ProfileSummaryCard(profile)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "No se encontró tu perfil. Completa el onboarding.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                when {
                    uiState.value.isGeneratingRoutine -> {
                        GeneratingRoutineCard()
                    }

                    uiState.value.generationError != null -> {
                        GenerationErrorCard(
                            error = uiState.value.generationError!!,
                            onRetry = viewModel::generateRoutine
                        )
                    }

                    activeRoutine != null && dailyWorkouts.isNotEmpty() -> {
                        Text(
                            text = "Tu plan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DailyWorkoutPager(
                            items = dailyWorkouts,
                            onSessionClick = { session ->
                                selectedSession = session
                            }
                        )
                    }

                    else -> {
                        CreateRoutineCard(
                            hasProfile = profile != null,
                            onCreate = viewModel::generateRoutine
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Botones de navegación
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Historial")
                    }

                    FilledTonalButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Perfil")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (selectedSession != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedSession = null },
            sheetState = sheetState
        ) {
            SessionDetailSheet(
                session = selectedSession!!,
                onStart = {
                    viewModel.startWorkout(selectedSession!!.id)
                    selectedSession = null
                },
                onDismiss = { selectedSession = null }
            )
        }
    }
}

@Composable
private fun DailyWorkoutPager(
    items: List<DailyWorkoutItem>,
    onSessionClick: (WorkoutSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val startIndex = remember(items) {
        items.indexOfFirst { !it.date.isBefore(today) }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { items.size }
    )

    Column(modifier = modifier.fillMaxWidth().height(420.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val item = items[page]
            DailyWorkoutCard(
                item = item,
                isToday = item.date == today,
                onClick = {
                    if (item is DailyWorkoutItem.TrainingDay) {
                        onSessionClick(item.session)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Page indicator dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, _ ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
private fun DailyWorkoutCard(
    item: DailyWorkoutItem,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (item) {
                is DailyWorkoutItem.RestDay -> MaterialTheme.colorScheme.surfaceVariant
                is DailyWorkoutItem.TrainingDay -> when (item.status) {
                    DailyWorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                    DailyWorkoutStatus.MISSED -> MaterialTheme.colorScheme.outlineVariant
                    DailyWorkoutStatus.UPCOMING -> MaterialTheme.colorScheme.surface
                }
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 6.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Date
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.date.formatShortWeekday(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${item.date.dayOfMonth}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.date.month.getDisplayName(
                        java.time.format.TextStyle.SHORT,
                        java.util.Locale.of("es", "ES")
                    ).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isToday) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Hoy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Middle: Content
            when (item) {
                is DailyWorkoutItem.RestDay -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🛌",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Día de descanso",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is DailyWorkoutItem.TrainingDay -> {
                    val session = item.session
                    val statusColor = when (item.status) {
                        DailyWorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        DailyWorkoutStatus.MISSED -> MaterialTheme.colorScheme.outline
                        DailyWorkoutStatus.UPCOMING -> MaterialTheme.colorScheme.tertiary
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sessionTypeLabel(session.type.name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SessionMiniStat(
                                icon = Icons.Default.WatchLater,
                                value = "${session.totalDurationMinutes} min"
                            )
                            SessionMiniStat(
                                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                value = "${session.estimatedCalories} kcal"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        StatusChip(status = item.status)
                    }
                }
            }

            // Bottom: action hint
            when (item) {
                is DailyWorkoutItem.RestDay -> {
                    Text(
                        text = "Recupera energías",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is DailyWorkoutItem.TrainingDay -> {
                    if (item.status != DailyWorkoutStatus.COMPLETED) {
                        Text(
                            text = "Toca para ver detalles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "¡Sesión completada!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: DailyWorkoutStatus) {
    val (label, color) = when (status) {
        DailyWorkoutStatus.UPCOMING -> "Pendiente" to MaterialTheme.colorScheme.tertiary
        DailyWorkoutStatus.MISSED -> "Perdida" to MaterialTheme.colorScheme.outline
        DailyWorkoutStatus.COMPLETED -> "Completada" to MaterialTheme.colorScheme.primary
    }

    val bgColor = when (status) {
        DailyWorkoutStatus.UPCOMING -> MaterialTheme.colorScheme.tertiaryContainer
        DailyWorkoutStatus.MISSED -> MaterialTheme.colorScheme.surfaceVariant
        DailyWorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
    }

    val textColor = when (status) {
        DailyWorkoutStatus.UPCOMING -> MaterialTheme.colorScheme.onTertiaryContainer
        DailyWorkoutStatus.MISSED -> MaterialTheme.colorScheme.onSurfaceVariant
        DailyWorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        if (status == DailyWorkoutStatus.COMPLETED) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
private fun SessionMiniStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionDetailSheet(
    session: WorkoutSession,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = sessionTypeLabel(session.type.name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${session.totalDurationMinutes} min · ${session.estimatedCalories} kcal",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (session.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = session.notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Fases",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        session.phases.forEach { phase ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val phaseColor = when (phase.type) {
                        PhaseType.WARM_UP -> MaterialTheme.colorScheme.tertiary
                        PhaseType.WALK -> MaterialTheme.colorScheme.primary
                        PhaseType.RUN -> MaterialTheme.colorScheme.error
                        PhaseType.RECOVERY -> MaterialTheme.colorScheme.secondary
                        PhaseType.COOL_DOWN -> MaterialTheme.colorScheme.tertiary
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(phaseColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = phase.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (phase.notes?.isNotBlank() == true) {
                            Text(
                                text = phase.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = "${phase.durationSeconds / 60} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!session.isCompleted) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Iniciar entrenamiento")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar")
        }
    }
}

private fun sessionTypeLabel(type: String): String = when (type) {
    "STEADY_STATE" -> "Ritmo constante"
    "INTERVALS" -> "Intervalos"
    "HIIT" -> "HIIT"
    "INCLINE" -> "Inclinación"
    "RECOVERY" -> "Recuperación"
    "ENDURANCE" -> "Resistencia"
    else -> type.replace("_", " ")
}

@Composable
private fun GeneratingRoutineCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tu entrenador IA está creando tu plan...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Esto puede tomar unos segundos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun GenerationErrorCard(error: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun CreateRoutineCard(hasProfile: Boolean, onCreate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "¡Estás listo para crear tu plan!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tu entrenador IA personalizado creará rutinas seguras y efectivas basadas en tu perfil.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                enabled = hasProfile
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasProfile) "Crear rutinas con IA" else "Completa tu perfil primero",
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(profile: dev.jefrien.walkrush.domain.model.userprofile.UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Tu perfil",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStat(
                    label = "Peso",
                    value = "%.1f kg".format(profile.weightKg)
                )
                ProfileStat(
                    label = "Altura",
                    value = "%.0f cm".format(profile.heightCm)
                )
                ProfileStat(
                    label = "Edad",
                    value = "${profile.age} años"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStat(
                    label = "Meta",
                    value = profile.fitnessGoal.type.name.replace("_", " ")
                )
                ProfileStat(
                    label = "Nivel",
                    value = when (profile.intensityLevel) {
                        IntensityLevel.BEGINNER -> "Principiante"
                        IntensityLevel.INTERMEDIATE -> "Intermedio"
                        IntensityLevel.INTENSE -> "Intenso"
                    }
                )
                ProfileStat(
                    label = "Días/Sem",
                    value = "${profile.daysPerWeek}"
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
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
