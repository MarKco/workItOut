package com.ilsecondodasinistra.workitout.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.domain.DayBar
import com.ilsecondodasinistra.workitout.domain.PeriodStats
import com.ilsecondodasinistra.workitout.domain.Stats
import com.ilsecondodasinistra.workitout.domain.WorkTimeCalculator

@Composable
fun StatsRoute(viewModel: StatsViewModel) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    stats?.let { StatsScreen(it) }
}

@Composable
fun StatsScreen(stats: Stats) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatsCard(stringResource(R.string.last_14_days)) {
            BarChart(days = stats.recentDays, modifier = Modifier.fillMaxWidth().height(180.dp))
        }
        PeriodCard(stringResource(R.string.this_week), stats.week)
        PeriodCard(stringResource(R.string.this_month), stats.month)
        PeriodCard(stringResource(R.string.all_time), stats.overall)
    }
}

@Composable
private fun StatsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun PeriodCard(title: String, stats: PeriodStats) {
    StatsCard(title) {
        StatRow(stringResource(R.string.stat_worked), WorkTimeCalculator.formatDuration(stats.workedMillis))
        StatRow(stringResource(R.string.stat_target), WorkTimeCalculator.formatDuration(stats.targetMillis))
        val balanceColor = when {
            stats.balanceMillis > 0 -> MaterialTheme.colorScheme.primary
            stats.balanceMillis < 0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }
        StatRow(
            stringResource(R.string.stat_balance),
            WorkTimeCalculator.formatBalance(stats.balanceMillis),
            valueColor = balanceColor,
            bold = true,
        )
        StatRow(stringResource(R.string.stat_average), WorkTimeCalculator.formatDuration(stats.averageMillis))
        StatRow(stringResource(R.string.stat_days), stats.daysWorked.toString())
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
            fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else null,
        )
    }
}

/** Hours worked per day. A tick on each bar marks that day's target; non-working days are tinted. */
@Composable
fun BarChart(days: List<DayBar>, modifier: Modifier = Modifier) {
    val workColor = MaterialTheme.colorScheme.primary
    val offDayColor = MaterialTheme.colorScheme.tertiary
    val targetColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    val measurer = rememberTextMeasurer()
    val description = stringResource(R.string.chart_description)

    Box(modifier.semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxSize()) {
            val labelHeight = 16.dp.toPx()
            val chartHeight = size.height - labelHeight
            val maxMillis = maxOf(
                days.maxOfOrNull { maxOf(it.workedMillis, it.targetMillis) } ?: 0L,
                3_600_000L,
            ).toFloat()
            val slot = size.width / days.size
            val barWidth = slot * 0.6f

            days.forEachIndexed { index, day ->
                val left = index * slot + (slot - barWidth) / 2
                val barHeight = chartHeight * day.workedMillis / maxMillis
                if (barHeight > 0f) {
                    drawRoundRect(
                        color = if (day.isWorkDay) workColor else offDayColor,
                        topLeft = Offset(left, chartHeight - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                }
                if (day.targetMillis > 0) {
                    val y = chartHeight - chartHeight * day.targetMillis / maxMillis
                    drawLine(
                        color = targetColor,
                        start = Offset(left - 2.dp.toPx(), y),
                        end = Offset(left + barWidth + 2.dp.toPx(), y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                    )
                }
                val label = measurer.measure(day.date.dayOfMonth.toString(), labelStyle)
                drawText(label, topLeft = Offset(left + (barWidth - label.size.width) / 2, chartHeight + 2.dp.toPx()))
            }
            drawLine(
                color = targetColor.copy(alpha = 0.3f),
                start = Offset(0f, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = Stroke.HairlineWidth,
            )
        }
    }
}
