package com.aki.submngr.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aki.submngr.data.Card
import com.aki.submngr.data.Entry
import com.aki.submngr.data.SectionType
import com.aki.submngr.ui.components.*
import com.aki.submngr.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(
    navController: NavController,
    sectionId: Long,
    vm: MainViewModel = viewModel()
) {
    val sections by vm.sections.collectAsState()
    val section = sections.find { it.id == sectionId } ?: return
    val cards by vm.getCardsForSection(sectionId).collectAsState(initial = emptyList())
    val entries by vm.getEntriesForSection(sectionId).collectAsState(initial = emptyList())

    var showAddSheet by remember { mutableStateOf(false) }
    var editCard by remember { mutableStateOf<Card?>(null) }
    var editEntry by remember { mutableStateOf<Entry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(section.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "追加",
                    tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (section.type) {
                SectionType.CARD_STACK -> CardStackContent(
                    cards = cards,
                    onEditCard = { editCard = it },
                    onDeleteCard = { vm.deleteCard(it) }
                )
                SectionType.PIE_CHART -> PieChartContent(
                    entries = entries,
                    onEditEntry = { editEntry = it },
                    onDeleteEntry = { vm.deleteEntry(it) }
                )
                SectionType.CHECKLIST, SectionType.LIST, SectionType.TABLE, SectionType.LINK ->
                    ListContent(
                        entries = entries,
                        sectionType = section.type,
                        onToggle = { id, checked -> vm.toggleEntry(id, checked) },
                        onEditEntry = { editEntry = it },
                        onDeleteEntry = { vm.deleteEntry(it) }
                    )
            }
        }
    }

    // ── Add Sheet ────────────────────────────────────────────────────────────
    if (showAddSheet) {
        when (section.type) {
            SectionType.CARD_STACK -> AddCardSheet(
                existingCards = cards,
                onDismiss = { showAddSheet = false },
                onConfirm = { title, subtitle, number, cStart, cEnd, photo ->
                    vm.addCard(sectionId, title, subtitle, number, cStart, cEnd, photo)
                    showAddSheet = false
                }
            )
            SectionType.PIE_CHART -> AddPieEntrySheet(
                existingEntries = entries,
                onDismiss = { showAddSheet = false },
                onConfirm = { label, amount, color ->
                    vm.addEntry(sectionId, label, "", amount, color)
                    showAddSheet = false
                }
            )
            SectionType.CHECKLIST -> AddListEntrySheet(
                title = "アイテムを追加", placeholder = "例: 牛乳",
                onDismiss = { showAddSheet = false },
                onConfirm = { text -> vm.addEntry(sectionId, text, "", 0.0, "#6750A4"); showAddSheet = false }
            )
            SectionType.LIST -> AddListEntrySheet(
                title = "メモを追加", placeholder = "内容を入力",
                onDismiss = { showAddSheet = false },
                onConfirm = { text -> vm.addEntry(sectionId, text, "", 0.0, "#6750A4"); showAddSheet = false }
            )
            SectionType.TABLE -> AddKeyValueSheet(
                sheetTitle = "項目を追加", keyLabel = "キー（例: ユーザー名）", valueLabel = "値（例: john_doe）",
                onDismiss = { showAddSheet = false },
                onConfirm = { label, value -> vm.addEntry(sectionId, label, value, 0.0, "#6750A4"); showAddSheet = false }
            )
            SectionType.LINK -> AddKeyValueSheet(
                sheetTitle = "リンクを追加", keyLabel = "タイトル（例: GitHub）", valueLabel = "URL",
                onDismiss = { showAddSheet = false },
                onConfirm = { label, value -> vm.addEntry(sectionId, label, value, 0.0, "#6750A4"); showAddSheet = false }
            )
        }
    }

    // ── Edit Card Sheet ───────────────────────────────────────────────────────
    editCard?.let { card ->
        AddCardSheet(
            initialCard = card,
            existingCards = cards.filter { it.id != card.id },
            onDismiss = { editCard = null },
            onConfirm = { title, subtitle, number, cStart, cEnd, photo ->
                vm.updateCard(card.copy(title = title, subtitle = subtitle, number = number,
                    colorStartHex = cStart, colorEndHex = cEnd, photoUri = photo))
                editCard = null
            }
        )
    }

    // ── Edit Entry Sheet ──────────────────────────────────────────────────────
    editEntry?.let { entry ->
        when (section.type) {
            SectionType.PIE_CHART -> AddPieEntrySheet(
                initialEntry = entry,
                existingEntries = entries.filter { it.id != entry.id },
                onDismiss = { editEntry = null },
                onConfirm = { label, amount, color ->
                    vm.updateEntry(entry.copy(label = label, amount = amount, colorHex = color))
                    editEntry = null
                }
            )
            SectionType.CHECKLIST, SectionType.LIST -> AddListEntrySheet(
                title = "編集",
                placeholder = "内容を入力",
                initialText = entry.label,
                onDismiss = { editEntry = null },
                onConfirm = { text -> vm.updateEntry(entry.copy(label = text)); editEntry = null }
            )
            SectionType.TABLE -> AddKeyValueSheet(
                sheetTitle = "項目を編集", keyLabel = "キー", valueLabel = "値",
                initialLabel = entry.label, initialValue = entry.value,
                onDismiss = { editEntry = null },
                onConfirm = { label, value -> vm.updateEntry(entry.copy(label = label, value = value)); editEntry = null }
            )
            SectionType.LINK -> AddKeyValueSheet(
                sheetTitle = "リンクを編集", keyLabel = "タイトル", valueLabel = "URL",
                initialLabel = entry.label, initialValue = entry.value,
                onDismiss = { editEntry = null },
                onConfirm = { label, value -> vm.updateEntry(entry.copy(label = label, value = value)); editEntry = null }
            )
            else -> {}
        }
    }
}
