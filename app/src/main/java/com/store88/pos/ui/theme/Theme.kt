package com.store88.pos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Teal = Color(0xFF0D9488)
val TealDark = Color(0xFF0F766E)
val TealHeader = Color(0xFF115E59)
val TealSoft = Color(0xFFCCFBF1)
val TealBg = Color(0xFFF0FDFA)
val Bg = Color(0xFFF8FAFC)
val CheckoutBg = Color(0xFFF4F6F8)
val TextDark = Color(0xFF0F172A)
val TextMuted = Color(0xFF64748B)
val Danger = Color(0xFFDC2626)
val Ok = Color(0xFF059669)
val Border = Color(0xFFE2E8F0)
val PriceBlue = Color(0xFF0284C7)
val PayCash = Color(0xFF16A34A)
val PayOctopus = Color(0xFFEA580C)
val PayCard = Color(0xFF2563EB)
val PayWeChat = Color(0xFF07C160)
val PayUnion = Color(0xFF00736A)
val PayAlipay = Color(0xFF1677FF)
val Sun = Color(0xFFFBBF24)

private val Scheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = TealDark,
    background = Bg,
    surface = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    error = Danger,
)

@Composable
fun Store88Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = MaterialTheme.typography.copy(
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            ),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        ),
        content = content,
    )
}
