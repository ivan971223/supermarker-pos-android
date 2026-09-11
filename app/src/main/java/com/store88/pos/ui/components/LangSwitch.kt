package com.store88.pos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.store88.pos.PosViewModel
import com.store88.pos.domain.UiLanguage
import com.store88.pos.ui.theme.Teal
import com.store88.pos.ui.theme.TealBg
import com.store88.pos.ui.theme.TextMuted

@Composable
fun LangSwitch(current: String, vm: PosViewModel, compact: Boolean = true) {
    val opts = listOf(
        UiLanguage.EN to "EN",
        UiLanguage.ZH to "中文",
        UiLanguage.BOTH to "EN+中文",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)) {
        opts.forEach { (lang, label) ->
            val id = when (lang) {
                UiLanguage.EN -> "en"
                UiLanguage.ZH -> "zh"
                UiLanguage.BOTH -> "both"
            }
            val on = current == id
            Text(
                label,
                color = if (on) Color.White else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 11.sp else 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) Teal else Color(0xFFF8FAFC))
                    .border(1.dp, if (on) Teal else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .clickable { vm.setLanguage(lang) }
                    .padding(
                        horizontal = if (compact) 8.dp else 10.dp,
                        vertical = if (compact) 5.dp else 6.dp,
                    ),
            )
        }
    }
}
