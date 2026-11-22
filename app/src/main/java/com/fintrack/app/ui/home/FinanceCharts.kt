package com.fintrack.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import java.time.YearMonth

data class SpendingPeriod(val label: String, val amountMinorUnits: Long)

fun monthlySpendingPeriods(
    transactions: List<Transaction>,
    month: YearMonth
): List<SpendingPeriod> {
    val expensesByPeriod = transactions
        .asSequence()
        .filter {
            it.type == TransactionType.EXPENSE &&
                YearMonth.from(it.date) == month
        }
        .groupBy { ((it.date.dayOfMonth - 1) / 7).coerceAtMost(3) }
        .mapValues { (_, values) -> values.sumOf(Transaction::amountMinorUnits) }

    return (0..3).map { index ->
        SpendingPeriod("Week ${index + 1}", expensesByPeriod[index] ?: 0L)
    }
}

@Composable
fun MonthlySpendingChart(periods: List<SpendingPeriod>) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val maxAmount = periods.maxOfOrNull(SpendingPeriod::amountMinorUnits)?.coerceAtLeast(1L) ?: 1L
    val animatedFractions = periods.map { period ->
        animateFloatAsState(
            targetValue = period.amountMinorUnits.toFloat() / maxAmount.toFloat(),
            label = "monthly-spending-${period.label}"
        ).value
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val bottom = size.height - 4.dp.toPx()
            val top = 8.dp.toPx()
            val chartHeight = bottom - top
            drawLine(
                color = grid,
                start = Offset(0f, bottom),
                end = Offset(size.width, bottom),
                strokeWidth = 1.dp.toPx()
            )
            val slotWidth = size.width / (periods.size * 2f)
            periods.forEachIndexed { index, _ ->
                val barHeight = chartHeight * animatedFractions[index]
                val barWidth = slotWidth.coerceAtMost(48.dp.toPx())
                val x = slotWidth * (index * 2 + 1) - barWidth / 2
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(x, bottom - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            periods.forEach { period ->
                Text(period.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun BatterySensorGauge(
    label: String,
    value: String,
    progress: Float?,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(76.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(76.dp)) {
                val stroke = 7.dp.toPx()
                drawArc(
                    color = track,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                progress?.let {
                    drawArc(
                        color = primary,
                        startAngle = 135f,
                        sweepAngle = 270f * it.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text(value, style = MaterialTheme.typography.labelMedium)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
