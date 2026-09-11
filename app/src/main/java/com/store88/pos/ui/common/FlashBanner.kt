package com.store88.pos.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Large till-facing flash banner (payment / print / errors).
 * Tap anywhere on the card to dismiss early.
 */
@Composable
fun FlashBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lower = message.lowercase()
    val isError = listOf("fail", "wrong", "error", "too low", "empty", "cannot", "unreachable")
        .any { lower.contains(it) }
    val bg = if (isError) Color(0xFFDC2626) else Color(0xFF0F766E)
    val icon = when {
        isError -> "⚠"
        lower.contains("print") || lower.contains("列印") -> "🖨"
        lower.contains("drawer") || lower.contains("錢箱") -> "🗄"
        lower.contains("sale") || lower.contains("完成") || lower.contains("refund") || lower.contains("void") -> "✓"
        else -> "✓"
    }

    LaunchedEffect(message) {
        delay(2000)
        onDismiss()
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.95f),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .shadow(16.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(bg)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 28.dp, vertical = 22.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(icon, fontSize = 36.sp)
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isError) "Notice / 注意" else "Done / 完成",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        message,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        lineHeight = 28.sp,
                    )
                }
                Text("OK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
