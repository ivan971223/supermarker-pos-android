package com.store88.pos.ui.payment

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.store88.pos.data.Pricing
import com.store88.pos.domain.PaymentMethod
import com.store88.pos.domain.PosConstants
import com.store88.pos.domain.Screen
import com.store88.pos.ui.theme.Border
import com.store88.pos.ui.theme.Ok
import com.store88.pos.ui.theme.Teal
import com.store88.pos.ui.theme.TealBg
import com.store88.pos.ui.theme.TealDark
import com.store88.pos.ui.theme.TealHeader
import com.store88.pos.ui.theme.TextMuted
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun PaymentScreen(ui: UiState, vm: PosViewModel) {
    val priced = Pricing.priceCart(
        ui.state.cart,
        ui.state.products,
        ui.state.promos,
        ui.state.cartDiscountPercent,
    )
    val tendered = ui.cashTendered.toDoubleOrNull() ?: 0.0
    val change = (tendered - priced.total).coerceAtLeast(0.0)
    val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    val txn = ui.state.sales.firstOrNull()?.id?.takeLast(8) ?: "NEW"

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().background(TealHeader).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("🛒 POS 收銀系統", color = Color.White, fontWeight = FontWeight.Bold)
            Text("${if (ui.online) "Wi‑Fi" else "Offline"} · 100% · $timeStr", color = Color.White)
        }
        Row(
            Modifier.fillMaxWidth().background(TealBg).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("🏷 收款付款 / Payment", fontWeight = FontWeight.Bold, color = TealDark)
            Text("單號 / Transaction No. $txn", color = TextMuted, fontSize = 13.sp)
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("總金額 / Total Due", color = TextMuted, modifier = Modifier.fillMaxWidth())
            Text(Pricing.money(priced.total), fontSize = 42.sp, fontWeight = FontWeight.Black, color = Teal)
            Text("請選擇付款方式 / Please select payment method", color = TextMuted, modifier = Modifier.padding(bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PosConstants.PAYMENTS.forEach { p ->
                    val on = ui.selectedPayment == p.method
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, if (on) Teal else Border, RoundedCornerShape(12.dp))
                            .background(if (on) TealBg else Color.White)
                            .clickable { vm.goPay(p.method) }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(p.icon, fontSize = 22.sp)
                        Text(vm.bi(p.en, p.zh), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    }
                }
            }

            if (ui.selectedPayment == PaymentMethod.CASH) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("現金金額 / Cash Tendered", color = TextMuted)
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, Border, RoundedCornerShape(10.dp)).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("HK$", fontWeight = FontWeight.ExtraBold, color = Teal)
                            Spacer(Modifier.size(8.dp))
                            Text(ui.cashTendered.ifEmpty { "0.00" }, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TealDark)
                        }
                    }
                    Text(" › ", fontSize = 28.sp, color = TextMuted)
                    Column(Modifier.weight(1f)) {
                        Text("找續 / Change", color = TextMuted)
                        Text(Pricing.money(change), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ok)
                    }
                }
                Column(
                    Modifier.padding(top = 10.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF8FAFC)).border(1.dp, Border, RoundedCornerShape(14.dp)).padding(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("exact" to "Exact / 正好 (${Pricing.money(priced.total)})", "+20" to "+$20", "+50" to "+$50", "+100" to "+$100", "+500" to "+$500").forEach { (k, label) ->
                            Text(
                                label,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = TealDark,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, Teal, RoundedCornerShape(10.dp))
                                    .background(TealBg)
                                    .clickable { vm.cashKey(k) }
                                    .padding(8.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "00", "back", "clear")
                    keys.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            row.forEach { k ->
                                Box(
                                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Border, RoundedCornerShape(12.dp)).background(Color.White).clickable { vm.cashKey(k) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        when (k) {
                                            "back" -> "⌫"
                                            "clear" -> "C"
                                            else -> k
                                        },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                    )
                                }
                            }
                            if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            } else {
                Text(
                    "Phase 1: complete payment on terminal, then confirm here.",
                    color = TextMuted,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (ui.printOnPay) TealBg else Color(0xFFF8FAFC)).clickable { vm.setPrintOnPay(!ui.printOnPay) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (ui.printOnPay) "ON" else "OFF", fontWeight = FontWeight.Bold, color = if (ui.printOnPay) Teal else TextMuted)
                Spacer(Modifier.size(10.dp))
                Text(if (ui.printOnPay) "🖨 Print receipt ON / 列印收據開啟" else "🖨 Print receipt OFF / 列印收據關閉")
            }
        }

        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Border, RoundedCornerShape(12.dp)).clickable { vm.setScreen(Screen.Checkout) },
                contentAlignment = Alignment.Center,
            ) { Text("← 返回 / Back", fontWeight = FontWeight.Bold) }
            Box(
                Modifier.weight(1.4f).height(56.dp).clip(RoundedCornerShape(12.dp)).background(Teal).clickable { vm.confirmPay() },
                contentAlignment = Alignment.Center,
            ) { Text("✓ 確認 / Confirm", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
        }
    }
}
