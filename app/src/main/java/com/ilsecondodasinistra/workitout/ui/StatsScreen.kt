package com.ilsecondodasinistra.workitout.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.domain.DayBar
import com.ilsecondodasinistra.workitout.domain.PeriodStats
import com.ilsecondodasinistra.workitout.domain.Stats
import com.ilsecondodasinistra.workitout.domain.WorkTimeCalculator
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun StatsRoute(viewModel: StatsViewModel) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    stats?.let { StatsScreen(it) }
}

private enum class Period(val labelRes: Int) {
    Week(R.string.stats_tab_week),
    Month(R.string.stats_tab_month),
    All(R.string.stats_tab_all),
}

@Composable
fun StatsScreen(stats: Stats) {
    var period by rememberSaveable { mutableStateOf(Period.Week) }
    val selected = when (period) {
        Period.Week -> stats.week
        Period.Month -> stats.month
        Period.All -> stats.overall
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Period.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = period == item,
                    onClick = { period = item },
                    shape = SegmentedButtonDefaults.itemShape(index, Period.entries.size),
                ) { Text(stringResource(item.labelRes)) }
            }
        }

        SummaryTiles(selected)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.last_14_days), style = MaterialTheme.typography.titleMedium)
                BarChart(days = stats.recentDays, modifier = Modifier.fillMaxWidth())
                ChartLegend()
            }
        }
    }
}

// --- Summary tiles --------------------------------------------------------------------------

@Composable
private fun SummaryTiles(stats: PeriodStats) {
    val balance = stats.balanceMillis
    val balanceContainer = when {
        balance > 0 -> MaterialTheme.colorScheme.tertiaryContainer
        balance < 0 -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val balanceContent = when {
        balance > 0 -> MaterialTheme.colorScheme.onTertiaryContainer
        balance < 0 -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.stat_worked),
                value = WorkTimeCalculator.formatDuration(stats.workedMillis),
                caption = stringResource(R.string.hero_of_target, WorkTimeCalculator.formatDuration(stats.targetMillis)),
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.stat_balance),
                value = WorkTimeCalculator.formatBalance(balance),
                container = balanceContainer,
                content = balanceContent,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.stat_average),
                value = WorkTimeCalculator.formatDuration(stats.averageMillis),
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                content = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.stat_days),
                value = stats.daysWorked.toString(),
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                content = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = content.copy(alpha = 0.75f))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
            )
            // Keeps every tile the same height, whether or not it has a caption.
            Text(
                text = caption ?: " ",
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.7f),
            )
        }
    }
}

// --- Chart ----------------------------------------------------------------------------------

@Composable
private fun ChartLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(MaterialTheme.colorScheme.primary, stringResource(R.string.legend_target_met))
        LegendItem(MaterialTheme.colorScheme.primary.copy(alpha = BELOW_TARGET_ALPHA), stringResource(R.string.legend_below))
        LegendItem(MaterialTheme.colorScheme.tertiary, stringResource(R.string.legend_day_off))
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private const val BELOW_TARGET_ALPHA = 0.4f

/**
 * Hours worked per day. Bars are full color when the day's target is met, faded when below it,
 * and tinted on non-working days. Tap a bar to read that day's value.
 */
@Composable
fun BarChart(days: List<DayBar>, modifier: Modifier = Modifier) {
    val metColor = MaterialTheme.colorScheme.primary
    val belowColor = metColor.copy(alpha = BELOW_TARGET_ALPHA)
    val offDayColor = MaterialTheme.colorScheme.tertiary
    val targetColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectionColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    val todayLabelStyle = labelStyle.copy(color = metColor, fontWeight = FontWeight.Bold)
    val measurer = rememberTextMeasurer()
    val description = stringResource(R.string.chart_description)
    val locale = LocalConfiguration.current.locales[0]
    var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val selected = days.getOrNull(selectedIndex)
        Text(
            text = if (selected == null) {
                stringResource(R.string.stats_chart_hint)
            } else {
                val day = selected.date.format(dateFormat)
                val worked = if (selected.workedMillis > 0) {
                    WorkTimeCalculator.formatDuration(selected.workedMillis)
                } else {
                    stringResource(R.string.stats_no_data)
                }
                "$day · $worked"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected == null) null else FontWeight.SemiBold,
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .semantics { contentDescription = description }
                .pointerInput(days.size) {
                    detectTapGestures { offset ->
                        val index = (offset.x / (size.width / days.size)).toInt().coerceIn(0, days.lastIndex)
                        selectedIndex = if (selectedIndex == index) -1 else index
                    }
                },
        ) {
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
                    val color = when {
                        !day.isWorkDay -> offDayColor
                        day.workedMillis >= day.targetMillis -> metColor
                        else -> belowColor
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, chartHeight - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                }
                if (index == selectedIndex) {
                    drawRoundRect(
                        color = selectionColor,
                        topLeft = Offset(left - 2.dp.toPx(), 0f),
                        size = Size(barWidth + 4.dp.toPx(), chartHeight),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(1.5.dp.toPx()),
                    )
                }
                if (day.targetMillis > 0) {
                    val y = chartHeight - chartHeight * day.targetMillis / maxMillis
                    drawLine(
                        color = targetColor.copy(alpha = 0.6f),
                        start = Offset(left - 3.dp.toPx(), y),
                        end = Offset(left + barWidth + 3.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                val isToday = index == days.lastIndex
                val label = measurer.measure(
                    day.date.dayOfMonth.toString(),
                    if (isToday) todayLabelStyle else labelStyle,
                )
                drawText(label, topLeft = Offset(left + (barWidth - label.size.width) / 2, chartHeight + 3.dp.toPx()))
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
