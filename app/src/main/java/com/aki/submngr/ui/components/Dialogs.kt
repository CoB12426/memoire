package com.aki.submngr.ui.components

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aki.submngr.data.Card
import com.aki.submngr.data.Entry
import com.aki.submngr.data.SectionType
import com.aki.submngr.ui.theme.cardGradients
import com.aki.submngr.ui.theme.pieColors
import com.aki.submngr.viewmodel.MainViewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

// ─── IME fix for Dialog-based bottom sheets ───────────────────────────────────

@Composable
private fun ImeWorkaround() {
    val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
    SideEffect {
        dialogWindowProvider?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}

// ─── Auto-color helpers ───────────────────────────────────────────────────────

fun nextGradient(existingCards: List<Card>): Pair<String, String> =
    MainViewModel.nextGradient(existingCards)

fun nextPieColor(existingEntries: List<Entry>): String =
    MainViewModel.nextPieColor(existingEntries)

// ─── Add Section ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSectionSheet(onDismiss: () -> Unit, onConfirm: (String, SectionType, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(SectionType.CARD_STACK) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        ImeWorkaround()
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text("新しいセクション", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("セクション名") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
            )
            Spacer(Modifier.height(20.dp))
            Text("記録方法", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))

            val allTypes = SectionType.values()
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTypes.take(4).forEach { type ->
                    TypeChip(type, selectedType == type, { selectedType = type }, Modifier.weight(1f).fillMaxHeight())
                }
            }
            if (allTypes.size > 4) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTypes.drop(4).forEach { type ->
                        TypeChip(type, selectedType == type, { selectedType = type }, Modifier.weight(1f).fillMaxHeight())
                    }
                    repeat(4 - allTypes.drop(4).size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedType, selectedType.defaultEmoji()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
            ) {
                Text("追加", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TypeChip(type: SectionType, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(type.defaultEmoji(), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(type.displayName(), style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
        }
    }
}

// ─── Add / Edit Card ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardSheet(
    initialCard: Card? = null,
    existingCards: List<Card> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (title: String, subtitle: String, number: String,
                colorStart: String, colorEnd: String, photoUri: String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val isEdit = initialCard != null

    var title by remember { mutableStateOf(initialCard?.title ?: "") }
    var subtitle by remember { mutableStateOf(initialCard?.subtitle ?: "") }
    var number by remember { mutableStateOf(initialCard?.number ?: "") }
    var photoUri by remember { mutableStateOf(initialCard?.photoUri ?: "") }
    var isAutoGradient by remember { mutableStateOf(initialCard == null) }

    val autoGradient = remember { nextGradient(existingCards) }
    var selectedGradient by remember {
        mutableStateOf(
            if (isEdit) Pair(initialCard!!.colorStartHex, initialCard.colorEndHex) else autoGradient
        )
    }
    val effectiveGradient = if (isAutoGradient) autoGradient else selectedGradient
    val isPhotoMode = photoUri.isNotEmpty()

    // ML Kit Document Scanner
    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                ?.pages?.firstOrNull()?.imageUri?.let { uri ->
                    copyToInternalStorage(context, uri)?.let { path -> photoUri = path }
                }
        }
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { copyToInternalStorage(context, it)?.let { path -> photoUri = path } }
    }

    fun launchScanner() {
        val opts = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .build()
        GmsDocumentScanning.getClient(opts).getStartScanIntent(activity)
            .addOnSuccessListener { scanLauncher.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener {
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        ImeWorkaround()
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(if (isEdit) "カードを編集" else "カードを追加",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            if (isPhotoMode) {
                // ── 写真モード ──────────────────────────────────────────────
                AsyncImage(
                    model = ImageRequest.Builder(context).data(File(photoUri)).crossfade(true).build(),
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { photoUri = "" },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("写真を削除してテキストモードに戻る",
                        style = MaterialTheme.typography.bodySmall)
                }
            } else {
                // ── テキストモード ─────────────────────────────────────────
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("タイトル（例: CAVA）") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it },
                    label = { Text("サブタイトル（例: Member Card）") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = number, onValueChange = { number = it },
                    label = { Text("番号・ID") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                Spacer(Modifier.height(16.dp))

                // グラデーション選択（自動付き）
                Text("カラー", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        AutoCircle(size = 40, isSelected = isAutoGradient,
                            onClick = { isAutoGradient = true })
                    }
                    items(cardGradients) { gradient ->
                        val c1 = parseHex(gradient.first)
                        val c2 = parseHex(gradient.second)
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(c1, c2)))
                                .then(if (!isAutoGradient && selectedGradient == gradient)
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier)
                                .clickable { selectedGradient = gradient; isAutoGradient = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // 写真スキャンボタン（モード共通）
            OutlinedButton(
                onClick = { launchScanner() },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("写真でカードを読み込む（自動クロップ）")
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (isPhotoMode) {
                        onConfirm("", "", "", effectiveGradient.first, effectiveGradient.second, photoUri)
                    } else {
                        onConfirm(title.trim(), subtitle.trim(), number.trim(),
                            effectiveGradient.first, effectiveGradient.second, "")
                    }
                },
                enabled = isPhotoMode || title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isEdit) "保存" else "追加", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Add / Edit Pie Entry ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPieEntrySheet(
    initialEntry: Entry? = null,
    existingEntries: List<Entry> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (label: String, amount: Double, color: String) -> Unit
) {
    val isEdit = initialEntry != null
    var label by remember { mutableStateOf(initialEntry?.label ?: "") }
    var amount by remember { mutableStateOf(if (isEdit) initialEntry!!.amount.toLong().toString() else "") }
    var isAutoColor by remember { mutableStateOf(initialEntry == null) }
    val autoColor = remember { nextPieColor(existingEntries) }
    var selectedColor by remember {
        mutableStateOf(if (isEdit) initialEntry!!.colorHex else autoColor)
    }
    val effectiveColor = if (isAutoColor) autoColor else selectedColor

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        ImeWorkaround()
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(if (isEdit) "支出を編集" else "支出を追加",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(value = label, onValueChange = { label = it },
                label = { Text("項目名（例: 家賃）") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = amount, onValueChange = { amount = it },
                label = { Text("金額（円）") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(16.dp))
            Text("カラー", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AutoCircle(size = 32, isSelected = isAutoColor, onClick = { isAutoColor = true })
                pieColors.forEach { hex ->
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(parseHex(hex))
                            .then(if (!isAutoColor && selectedColor == hex)
                                Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                            else Modifier)
                            .clickable { selectedColor = hex; isAutoColor = false }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (label.isNotBlank() && amt > 0) onConfirm(label.trim(), amt, effectiveColor)
                },
                enabled = label.isNotBlank() && amount.toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isEdit) "保存" else "追加", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Add / Edit List Entry ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListEntrySheet(
    title: String, placeholder: String, initialText: String = "",
    onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    val isEdit = initialText.isNotEmpty()
    var text by remember { mutableStateOf(initialText) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        ImeWorkaround()
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(value = text, onValueChange = { text = it },
                label = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isEdit) "保存" else "追加", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Add / Edit Key-Value Entry ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKeyValueSheet(
    sheetTitle: String, keyLabel: String, valueLabel: String,
    valueKeyboardType: KeyboardType = KeyboardType.Text,
    initialLabel: String = "", initialValue: String = "",
    onDismiss: () -> Unit, onConfirm: (label: String, value: String) -> Unit
) {
    val isEdit = initialLabel.isNotEmpty()
    var label by remember { mutableStateOf(initialLabel) }
    var value by remember { mutableStateOf(initialValue) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        ImeWorkaround()
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(sheetTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text(keyLabel) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(valueLabel) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = valueKeyboardType))
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { if (label.isNotBlank()) onConfirm(label.trim(), value.trim()) },
                enabled = label.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isEdit) "保存" else "追加", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
fun DeleteConfirmDialog(itemName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("削除の確認") },
        text = { Text("「$itemName」を削除しますか？") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("削除", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemActionDialog(itemName: String, onEdit: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(itemName, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("編集")
                    }
                    Button(
                        onClick = onDelete, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("削除")
                    }
                }
            }
        }
    }
}

// ─── Shared UI pieces ─────────────────────────────────────────────────────────

/** Rainbow circle representing "auto" color selection */
@Composable
private fun AutoCircle(size: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape)
            .background(Brush.sweepGradient(listOf(
                Color.Red, Color(0xFFFF8800), Color.Yellow,
                Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )))
            .then(if (isSelected)
                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
            else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "自動",
                tint = Color.White, modifier = Modifier.size((size * 0.45).dp))
        }
    }
}

// ─── Utilities ────────────────────────────────────────────────────────────────

fun parseHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF6750A4)
}

private fun copyToInternalStorage(context: Context, uri: Uri): String? = try {
    val dir = File(context.filesDir, "card_images").also { it.mkdirs() }
    val dest = File(dir, "card_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        dest.outputStream().use { output -> input.copyTo(output) }
    }
    dest.absolutePath
} catch (e: Exception) { null }
