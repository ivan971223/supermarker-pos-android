package com.store88.pos.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.store88.pos.PosViewModel
import com.store88.pos.UiState
import com.store88.pos.domain.PosConstants
import com.store88.pos.ui.components.LangSwitch
import com.store88.pos.ui.theme.Border
import com.store88.pos.ui.theme.Ok
import com.store88.pos.ui.theme.Sun
import com.store88.pos.ui.theme.Teal
import com.store88.pos.ui.theme.TealBg
import com.store88.pos.ui.theme.TealDark
import com.store88.pos.ui.theme.TextMuted

@Composable
fun LoginScreen(ui: UiState, vm: PosViewModel) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)),
    ) {
        Box(Modifier.padding(16.dp, 16.dp).align(Alignment.TopStart)) {
            LangSwitch(ui.state.uiLanguage, vm)
        }

        Column(
            Modifier
                .align(Alignment.Center)
                .width(720.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .padding(28.dp, 28.dp, 32.dp, 32.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("☀", fontSize = 40.sp, color = Sun)
                val shopEn = ui.state.shopName.ifBlank { "88 Store" }
                val shopZh = ui.state.shopNameZh.ifBlank { "88超市" }
                if (ui.state.uiLanguage != "zh") {
                    Text(shopEn, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TealDark)
                }
                if (ui.state.uiLanguage != "en") {
                    Text(
                        shopZh,
                        fontSize = if (ui.state.uiLanguage == "zh") 24.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ui.state.uiLanguage == "zh") TealDark else TextMuted,
                    )
                }
                val branch = listOfNotNull(
                    ui.state.shopCode.takeIf { it.isNotBlank() } ?: ui.sync.shopCode.takeIf { it.isNotBlank() },
                ).firstOrNull()
                if (branch != null) {
                    Text(
                        vm.bi("Branch / 分店：$branch", "分店：$branch"),
                        color = Teal,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(vm.bi("Point of Sale", "收銀系統"), color = TextMuted, fontSize = 15.sp)
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFCCFBF1)))
                Text("🛒", modifier = Modifier.padding(horizontal = 12.dp), color = Teal)
                Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFCCFBF1)))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Enter PIN / 請輸入 PIN 碼", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        repeat(4) { i ->
                            Box(
                                Modifier
                                    .padding(horizontal = 7.dp)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Teal, CircleShape)
                                    .background(if (ui.pin.length > i) Teal else Color.Transparent),
                            )
                        }
                    }
                    Text(
                        "Use built-in numpad / 使用機身數字鍵",
                        color = Teal,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    )
                    val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "back"))
                    keys.forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { k ->
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .then(if (k.isNotEmpty()) Modifier.clickable { vm.pinKey(if (k == "back") "back" else k) } else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(if (k == "back") "⌫" else k, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Select Cashier / 選擇收銀員", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        (ui.state.cashiers.ifEmpty { PosConstants.CASHIERS }).forEach { c ->
                            val selected = ui.selectedCashierId == c.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, if (selected) Teal else Border, RoundedCornerShape(12.dp))
                                    .background(if (selected) TealBg else Color.White)
                                    .clickable { vm.selectCashier(c.id) }
                                    .padding(12.dp, 12.dp),
                            ) {
                                Text(if (c.id == "amy" || c.id.contains("a")) "👩" else "👨", fontSize = 44.sp)
                                Text(c.nameZh, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Text(c.name, color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            ui.loginError?.let {
                Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Demo PIN: Chan Tai Man ", color = TextMuted, fontSize = 12.sp)
                Text("1234", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(" · Ken Wong ", color = TextMuted, fontSize = 12.sp)
                Text("5678", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(" · ", color = TextMuted, fontSize = 12.sp)
                Text(
                    if (ui.online) "Online" else "Offline",
                    color = if (ui.online) Ok else Color(0xFFD97706),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Text(" · ", color = TextMuted, fontSize = 12.sp)
                Text(
                    "Reset",
                    color = Teal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { vm.resetDemo() },
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(110.dp)
                .background(TealDark),
            contentAlignment = Alignment.Center,
        ) {
            Text("🔒  Secure. Simple. Reliable.", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
