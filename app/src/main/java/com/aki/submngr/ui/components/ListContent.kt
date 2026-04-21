@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aki.submngr.ui.components

import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aki.submngr.data.Entry
import com.aki.submngr.data.SectionType

@Composable
fun ListContent(
    entries: List<Entry>,
    sectionType: SectionType,
    onToggle: (Long, Boolean) -> Unit,
    onEditEntry: (Entry) -> Unit,
    onDeleteEntry: (Entry) -> Unit
) {
    var actionTarget by remember { mutableStateOf<Entry?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
    ) {
        if (entries.isEmpty()) {
            val hint = when (sectionType) {
                SectionType.CHECKLIST -> "チェックリストを追加してください\n買い物リストなどに使えます"
                SectionType.TABLE    -> "項目を追加してください\nキーと値のペアで記録できます"
                SectionType.LINK     -> "リンクを追加してください\nよく使うURLを保存できます"
                else                 -> "リストを追加してください"
            }
            item { EmptyHint(hint) }
        } else {
            when (sectionType) {
                SectionType.TABLE -> {
                    itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                        TableRow(entry = entry, onLongPress = { actionTarget = entry })
                        if (index < entries.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
                SectionType.LINK  -> {
                    itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                        LinkRow(entry = entry, onLongPress = { actionTarget = entry })
                        if (index < entries.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
                else -> {
                    itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                        CheckOrTextRow(
                            entry = entry,
                            isChecklist = sectionType == SectionType.CHECKLIST,
                            onToggle = { onToggle(entry.id, !entry.isChecked) },
                            onLongPress = { actionTarget = entry }
                        )
                        if (index < entries.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
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
private fun CheckOrTextRow(entry: Entry, isChecklist: Boolean, onToggle: () -> Unit, onLongPress: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = { onLongPress() })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isChecklist) {
            Checkbox(checked = entry.isChecked, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = entry.label,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (isChecklist && entry.isChecked) TextDecoration.LineThrough else null,
            color = if (isChecklist && entry.isChecked)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(vertical = 14.dp)
        )
    }
}

@Composable
private fun TableRow(entry: Entry, onLongPress: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = { onLongPress() })
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = entry.value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LinkRow(entry: Entry, onLongPress: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    val raw = entry.value.trim()
                    if (raw.isNotEmpty()) {
                        val normalized = if (URLUtil.isNetworkUrl(raw)) raw else "https://$raw"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                            addCategory(Intent.CATEGORY_BROWSABLE)
                        }
                        val chooser = Intent.createChooser(intent, null)
                        runCatching { context.startActivity(chooser) }
                    }
                },
                onLongClick = { onLongPress() }
            )
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.label, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium)
            if (entry.value.isNotEmpty()) {
                Text(entry.value, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "開く",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
    }
}
