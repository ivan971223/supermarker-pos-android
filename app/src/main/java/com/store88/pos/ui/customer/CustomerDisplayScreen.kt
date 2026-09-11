package com.store88.pos.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.store88.pos.UiState
import com.store88.pos.data.Pricing
import com.store88.pos.domain.PaymentMethod
import com.store88.pos.domain.PosConstants
import com.store88.pos.domain.Screen
import com.store88.pos.ui.theme.Border
import com.store88.pos.ui.theme.Ok
import com.store88.pos.ui.theme.Sun
import com.store88.pos.ui.theme.Teal
import com.store88.pos.ui.theme.TealBg
import com.store88.pos.ui.theme.TealDark
import com.store88.pos.ui.theme.TextDark
import com.store88.pos.ui.theme.TextMuted

/**
 * Customer-facing dual-screen UI (客顯): cart lines + totals, idle welcome, pay prompt, thank-you.
 */
@Composable
fun CustomerDisplayScreen(ui: UiState) {
    val state = ui.state
    val lang = state.uiLanguage
    val brand = when (lang) {
        "en" -> state.shopName.ifBlank { "88 Store" }
        "zh" -> state.shopNameZh.ifBlank { "88超市" }
        else -> "${state.shopName.ifBlank { "88 Store" }} / ${state.shopNameZh.ifBlank { "88超市" }}"
    }
    val thankYou = ui.customerThankYouUntil > System.currentTimeMillis()
    val priced = Pricing.priceCart(state.cart, state.products, state.promos, state.cartDiscountPercent)
    val itemCount = priced.lines.sumOf { it.qty }
    val paying = ui.screen == Screen.Payment && priced.lines.isNotEmpty()
    val payLabel = PosConstants.PAYMENTS.find { it.method == ui.selectedPayment }?.let {
        when (lang) {
            "en" -> it.en
            "zh" -> it.zh
            else -> "${it.en} / ${it.zh}"
        }
    } ?: ui.selectedPayment.id

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF0FDFA)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(TealDark)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("☀", color = Sun, fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Text(brand, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            }
            Text(
                when {
                    thankYou -> bi(lang, "Thank you!", "多謝惠顧！")
                    paying -> bi(lang, "Please pay", "請付款")
                    itemCount > 0 -> bi(lang, "Your items ($itemCount)", "您的商品（${itemCount}件）")
                    else -> bi(lang, "Welcome", "歡迎光臨")
                },
                color = Color.White.copy(alpha = 0.95f),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }

        when {
            thankYou -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 72.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(bi(lang, "Payment complete", "付款完成"), fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = TealDark)
                        Text(
                            Pricing.money(ui.customerThankYouTotal),
                            fontWeight = FontWeight.Black,
                            fontSize = 48.sp,
                            color = Teal,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(bi(lang, "Have a nice day!", "歡迎再次光臨！"), color = TextMuted, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
            priced.lines.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("🛒", fontSize = 80.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(bi(lang, "Welcome to 88 Store", "歡迎光臨 88超市"), fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, color = TealDark, textAlign = TextAlign.Center)
                        Text(
                            bi(lang, "Your cart will appear here", "購物車內容會顯示於此"),
                            color = TextMuted,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
            else -> {
                Row(Modifier.weight(1f).fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LazyColumn(
                        Modifier
                            .weight(1.4f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Border, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(priced.lines, key = { it.productId }) { line ->
                            val p = state.products.find { it.id == line.productId }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
                                    contentAlignment = Alignment.Center,
                                ) { Text(p?.emoji ?: "📦", fontSize = 28.sp) }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        when (lang) {
                                            "zh" -> p?.nameZh ?: line.name
                                            "en" -> p?.nameEn ?: line.name
                                            else -> "${p?.nameEn ?: line.name}${if (!p?.nameZh.isNullOrBlank()) " / ${p?.nameZh}" else ""}"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = TextDark,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text("× ${line.qty}", color = TextMuted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    Pricing.money(line.lineTotal),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Teal,
                                )
                            }
                        }
                    }

                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Border, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            if (paying) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(TealBg)
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        bi(lang, "Paying with $payLabel", "付款方式：$payLabel"),
                                        fontWeight = FontWeight.Bold,
                                        color = TealDark,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                            TotalLine(bi(lang, "Subtotal", "小計"), Pricing.money(priced.subtotal))
                            TotalLine(bi(lang, "Discount", "折扣"), "-${Pricing.money(priced.discount)}", Ok)
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
                            Spacer(Modifier.height(12.dp))
                            Text(bi(lang, "Total", "總計"), color = TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                Pricing.money(priced.total),
                                fontWeight = FontWeight.Black,
                                fontSize = 48.sp,
                                color = Teal,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            if (paying && ui.selectedPayment == PaymentMethod.CASH) {
                                val tendered = ui.cashTendered.toDoubleOrNull() ?: 0.0
                                val change = (tendered - priced.total).coerceAtLeast(0.0)
                                Spacer(Modifier.height(16.dp))
                                TotalLine(bi(lang, "Cash tendered", "現金"), Pricing.money(tendered))
                                TotalLine(bi(lang, "Change", "找續"), Pricing.money(change), Ok)
                            }
                        }
                        Text(
                            bi(lang, "Prices in HKD", "金額以港幣計算"),
                            color = TextMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalLine(label: String, value: String, color: Color = TextDark) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 18.sp, color = color)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun bi(lang: String, en: String, zh: String): String = when (lang) {
    "en" -> en
    "zh" -> zh
    else -> "$en / $zh"
}
