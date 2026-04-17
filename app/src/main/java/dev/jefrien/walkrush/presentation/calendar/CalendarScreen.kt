package dev.jefrien.walkrush.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario") },
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
                .padding(horizontal = 16.dp)
        ) {
            if (state.isLoading) {
                Spacer(modifier = Modifier.height(64.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Spacer(modifier = Modifier.height(8.dp))

                MonthHeader(
                    yearMonth = state.currentMonth,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )

                Spacer(modifier = Modifier.height(16.dp))

                WeekdayHeader()

                Spacer(modifier = Modifier.height(8.dp))

                CalendarGrid(
                    yearMonth = state.currentMonth,
                    dayStatus = { date -> state.dayStatus(date) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Legend()
            }
        }
    }
}

@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.of("es", "ES"))
        .replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                contentDescription = "Mes anterior"
            )
        }
        Text(
            text = "$monthName ${yearMonth.year}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = "Mes siguiente"
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val days = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    dayStatus: (LocalDate) -> CalendarDayStatus?
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    // Monday = 1, Sunday = 7
    val startOffset = firstDayOfMonth.dayOfWeek.value - 1

    val cells = mutableListOf<CalendarCell>()
    repeat(startOffset) { index ->
        cells.add(CalendarCell.Empty(index))
    }
    for (day in 1..daysInMonth) {
        val date = yearMonth.atDay(day)
        cells.add(CalendarCell.Day(date, dayStatus(date)))
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(cells, key = { it.key }) { cell ->
            when (cell) {
                is CalendarCell.Empty -> Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                )
                is CalendarCell.Day -> DayCell(
                    date = cell.date,
                    status = cell.status,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    status: CalendarDayStatus?,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isToday = date == today

    val (bgColor, emoji) = when (status) {
        CalendarDayStatus.COMPLETED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) to "✅"
        CalendarDayStatus.MISSED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) to "❌"
        CalendarDayStatus.UPCOMING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) to "🏃"
        CalendarDayStatus.REST -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) to "🛌"
        null -> MaterialTheme.colorScheme.surface to null
    }

    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = bgColor
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${date.dayOfMonth}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Text(
                        text = "${date.dayOfMonth}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (status != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (emoji != null) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem("✅", "Completado")
        LegendItem("❌", "Perdido")
        LegendItem("🏃", "Pendiente")
        LegendItem("🛌", "Descanso")
    }
}

@Composable
private fun LegendItem(emoji: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.bodyMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private sealed class CalendarCell {
    abstract val key: String

    data class Empty(val index: Int) : CalendarCell() {
        override val key: String = "empty_$index"
    }

    data class Day(val date: LocalDate, val status: CalendarDayStatus?) : CalendarCell() {
        override val key: String = date.toString()
    }
}
