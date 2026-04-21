package com.aki.submngr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aki.submngr.data.Entry
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PieChartContent(
    entries: List<Entry>,
    onEditEntry: (Entry) -> Unit,
    onDeleteEntry: (Entry) -> Unit
) {
    var actionTarget by remember { mutableStateOf<Entry?>(null) }
    val total = entries.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        if (entries.isEmpty()) {
            EmptyHint("固定支出を追加してください\n毎月の支払いを記録して可視化できます")
        } else {
            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                val colors = entries.map { parseHex(it.colorHex) }

                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    if (total > 0) {
                        val strokeWidth = size.minDimension * 0.18f
                        val inset = strokeWidth / 2f + 4.dp.toPx()
                        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                        val topLeft = Offset(inset, inset)
                        var startAngle = -90f
                        entries.forEachIndexed { i, entry ->
                            val sweep = (entry.amount / total * 360f).toFloat()
                            drawArc(
                                color = colors[i],
                                startAngle = startAngle,
                                sweepAngle = (sweep - 2f).coerceAtLeast(0.5f),
                                useCenter = false, topLeft = topLeft, size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += sweep
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "¥${NumberFormat.getNumberInstance(Locale.JAPAN).format(total.toLong())}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            entries.forEach { entry ->
                EntryRow(entry = entry, onLongPress = { actionTarget = entry })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
        Spacer(Modifier.height(88.dp))
    }

    actionTarget?.let { entry ->
        ItemActionDialog(
            itemName = entry.label,
            onEdit = { onEditEntry(entry); actionTarget = null },
            onDelete = { onDeleteEntry(entry); actionTarget = null },
            onDismiss = { actionTarget = null }
        )
    }
}

@Composable
private fun EntryRow(entry: Entry, onLongPress: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(parseHex(entry.colorHex)))
        Spacer(Modifier.width(14.dp))
        Text(entry.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = "¥${NumberFormat.getNumberInstance(Locale.JAPAN).format(entry.amount.toLong())}",
            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium
        )
    }
}
