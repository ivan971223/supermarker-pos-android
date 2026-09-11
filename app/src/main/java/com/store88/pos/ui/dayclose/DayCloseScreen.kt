package com.store88.pos.ui.dayclose

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.store88.pos.domain.PosConstants
import com.store88.pos.domain.Screen
import com.store88.pos.ui.theme.Border
import com.store88.pos.ui.theme.Teal
import com.store88.pos.ui.theme.TealHeader
import com.store88.pos.ui.theme.TextMuted
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DayCloseScreen(ui: UiState, vm: PosViewModel) {
    LaunchedEffect(Unit) {
        if (ui.cashLeftOvernight.isEmpty()) vm.fillCashLeftWithOpening()
    }
    val session = ui.state.session
    val sessionSales = Pricing.sessionSales(ui.state)
    val active = sessionSales.filter { it.isActive }
    val voided = sessionSales.filter { it.status == "voided" }
    val refunded = sessionSales.filter { it.status == "refunded" }
    val netSales = active.sumOf { it.total }
    val cashSales = Pricing.sessionCashSales(ui.state)
    val opening = session?.openingFloat ?: 0.0
    val expected = opening + cashSales
    val counted = ui.countedCash.toDoubleOrNull() ?: 0.0
    val left = ui.cashLeftOvernight.toDoubleOrNull()
        ?: opening.coerceAtMost(if (counted > 0) counted else opening)
    val bank = (counted - left).coerceAtLeast(0.0)
    val diff = counted - expected
    val breakdown = Pricing.paymentBreakdown(ui.state)
    val totalAll = breakdown.values.sum()
    val now = LocalDateTime.now()
    val dateStr = now.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy"))
    val timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"))

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Row(
            Modifier.fillMaxWidth().background(TealHeader).padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(vm.bi("Close Session", "日結"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text(
                    "${session?.cashier ?: "—"} · ${vm.bi("End of Day Cash Closing", "每日結算")}",
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Text("$dateStr\n$timeStr", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Row(
            Modifier.weight(1f).padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Border, RoundedCornerShape(16.dp))
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(vm.bi("Session summary", "本班概況"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                SummaryRow(vm.bi("Orders", "單數"), "${active.size}")
                SummaryRow(vm.bi("Voids", "作廢"), "${voided.size}", Color(0xFFDC2626))
                SummaryRow(vm.bi("Refunds", "退款"), "${refunded.size}", Color(0xFFEA580C))
                SummaryRow(vm.bi("Net sales", "淨銷售"), Pricing.money(netSales), Teal)
                Spacer(Modifier.height(16.dp))
                Text("1. Expected Cash in Drawer / 應有現金", color = TextMuted)
                Text(Pricing.money(expected), fontSize = 32.sp, fontWeight = FontWeight.Black, color = Teal)
                Text(
                    "Float ${Pricing.money(opening)}（琴日淨低）+ cash sales ${Pricing.money(cashSales)}",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("2. Counted Cash / 實收現金", color = TextMuted)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Teal, RoundedCornerShape(8.dp))
                            .clickable { vm.fillCountedWithExpected() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(vm.bi("Use expected", "填入應有"), color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Border, RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("HK$", fontWeight = FontWeight.Bold, color = Teal)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = ui.countedCash,
                        onValueChange = vm::setCountedCash,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (ui.countedCash.isEmpty()) Text("0.00", color = TextMuted)
                            inner()
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("3. Difference / 差額 ${if (diff >= 0) "(Over / 溢額)" else "(Short / 短欠)"}", color = TextMuted)
                Text(Pricing.money(diff), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = if (diff >= 0) Teal else Color.Red)
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("4. Cash left overnight / 現金淨低", color = TextMuted)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Teal, RoundedCornerShape(8.dp))
                            .clickable { vm.fillCashLeftWithOpening() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(vm.bi("Use opening", "用開市浮存"), color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(
                    vm.bi(
                        "Left in drawer for tomorrow (rest → bank)",
                        "留櫃作聽日開市；其餘入銀行",
                    ),
                    color = TextMuted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Border, RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("HK$", fontWeight = FontWeight.Bold, color = Teal)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = ui.cashLeftOvernight,
                        onValueChange = vm::setCashLeftOvernight,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (ui.cashLeftOvernight.isEmpty()) Text("%.2f".format(opening), color = TextMuted)
                            inner()
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("5. Bank deposit / 入銀行", color = TextMuted)
                Text(Pricing.money(bank), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Teal)
                Text(
                    "Counted ${Pricing.money(counted)} − left ${Pricing.money(left)}",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Border, RoundedCornerShape(16.dp))
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Payment Summary / 付款總結", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(vm.bi("Active sales only (void/refund excluded)", "只計有效單（已扣除作廢/退款）"), color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                PosConstants.PAYMENTS.forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${p.icon}  ${vm.bi(p.en, p.zh)}")
                        Text(Pricing.money(breakdown[p.method] ?: 0.0), fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total / 總計", fontWeight = FontWeight.ExtraBold)
                    Text(Pricing.money(totalAll), fontWeight = FontWeight.ExtraBold, color = Teal)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Teal, RoundedCornerShape(12.dp))
                        .clickable { vm.printDayCloseReport() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(vm.bi("Print Z report", "列印日結報表"), color = Teal, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .clickable { vm.setScreen(Screen.Checkout) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(vm.bi("Continue Selling", "繼續營業"), fontWeight = FontWeight.Bold)
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Teal)
                    .clickable { vm.closeDay() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    vm.bi("Close & Print Report", "結束並列印報表"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextMuted)
        Text(value, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
