package com.aki.submngr.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aki.submngr.data.Card
import java.io.File
import kotlin.random.Random

private const val CREDIT_CARD_ASPECT = 1.586f
private val STACK_PEEK = 56.dp

private data class CardBubble(
    val size: Dp,
    val alignTopEnd: Boolean,
    val offsetX: Dp,
    val offsetY: Dp,
    val alpha: Float
)

@Composable
fun CardStackContent(
    cards: List<Card>,
    onEditCard: (Card) -> Unit,
    onDeleteCard: (Card) -> Unit
) {
    var actionTarget by remember { mutableStateOf<Card?>(null) }
    var selectedCardId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(cards) {
        if (cards.none { it.id == selectedCardId }) selectedCardId = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        if (cards.isEmpty()) {
            EmptyHint("カードを追加してください\n会員番号や ID などを記録できます")
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardHeight = maxWidth / CREDIT_CARD_ASPECT
                val selectedIndex = cards.indexOfFirst { it.id == selectedCardId }
                val totalHeight = if (selectedIndex >= 0) {
                    val afterCount = (cards.size - selectedIndex - 1).coerceAtLeast(0)
                    cardHeight + STACK_PEEK * selectedIndex.toFloat() + cardHeight + STACK_PEEK * afterCount.toFloat()
                } else {
                    cardHeight + STACK_PEEK * (cards.size - 1).toFloat()
                }

                Box(modifier = Modifier.fillMaxWidth().height(totalHeight)) {
                    cards.forEachIndexed { index, card ->
                        val targetY = when {
                            selectedIndex < 0 -> STACK_PEEK * index.toFloat()
                            index <= selectedIndex -> STACK_PEEK * index.toFloat()
                            else -> STACK_PEEK * selectedIndex.toFloat() + cardHeight + STACK_PEEK * (index - selectedIndex - 1).toFloat()
                        }
                        val animatedY by animateDpAsState(
                            targetValue = targetY,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "cardOffset"
                        )
                        val z = if (selectedIndex == index) 100f else index.toFloat()

                        MemberCard(
                            card = card,
                            onTap = {
                                selectedCardId = if (selectedCardId == card.id) null else card.id
                            },
                            onLongPress = { actionTarget = card },
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = animatedY)
                                .zIndex(z)
                        )
                        }
                }
            }
        }
        Spacer(Modifier.height(88.dp))
    }

    actionTarget?.let { card ->
        ItemActionDialog(
            itemName = card.title.ifEmpty { "写真カード" },
            onEdit = { onEditCard(card); actionTarget = null },
            onDelete = { onDeleteCard(card); actionTarget = null },
            onDismiss = { actionTarget = null }
        )
    }
}

@Composable
fun MemberCard(card: Card, onTap: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val colorStart = parseHex(card.colorStartHex)
    val colorEnd = parseHex(card.colorEndHex)
    val context = LocalContext.current
    val hasPhoto = card.photoUri.isNotEmpty()
    val hasText = card.title.isNotEmpty() || card.number.isNotEmpty()
    val bubbles = remember(card.id, card.title, card.number, card.colorStartHex, card.colorEndHex) {
        val seed = (card.id * 37L).toInt() xor card.title.hashCode() xor card.number.hashCode()
        val random = Random(seed)
        List(6) {
            CardBubble(
                size = random.nextInt(56, 140).dp,
                alignTopEnd = random.nextBoolean(),
                offsetX = random.nextInt(-36, 42).dp,
                offsetY = random.nextInt(-30, 34).dp,
                alpha = random.nextInt(4, 14) / 100f
            )
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(CREDIT_CARD_ASPECT)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(
                colors = listOf(colorStart, colorEnd),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ))
            .pointerInput(card.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        if (hasPhoto) {
            // 写真をカード全体に表示
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(card.photoUri)).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // テキストがある場合のみオーバーレイを表示
            if (hasText) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 80f
                        )
                    )
                )
            }
        } else {
            // ランダムバブル装飾
            bubbles.forEach { bubble ->
                Box(
                    modifier = Modifier
                        .size(bubble.size)
                        .align(if (bubble.alignTopEnd) Alignment.TopEnd else Alignment.BottomStart)
                        .offset(x = bubble.offsetX, y = bubble.offsetY)
                        .background(
                            Color.White.copy(alpha = bubble.alpha),
                            RoundedCornerShape(percent = 50)
                        )
                )
            }
        }

        // テキストコンテンツ（写真のみカードはテキストなし）
        if (!hasPhoto || hasText) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (card.title.isNotEmpty()) {
                        Text(card.title, style = MaterialTheme.typography.titleLarge,
                            color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    if (card.subtitle.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(card.subtitle, style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f))
                    }
                }
                if (card.number.isNotEmpty()) {
                    Text(card.number, color = Color.White, fontSize = 18.sp,
                        fontWeight = FontWeight.Light, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
