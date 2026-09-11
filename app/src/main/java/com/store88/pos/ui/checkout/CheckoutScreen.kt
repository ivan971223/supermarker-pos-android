package com.store88.pos.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.store88.pos.PosViewModel
import com.store88.pos.UiState
import com.store88.pos.data.Pricing
import com.store88.pos.domain.PaymentMethod
import com.store88.pos.domain.Screen
import com.store88.pos.ui.components.LangSwitch
import com.store88.pos.ui.theme.Border
import com.store88.pos.ui.theme.CheckoutBg
import com.store88.pos.ui.theme.Danger
import com.store88.pos.ui.theme.PayAlipay
import com.store88.pos.ui.theme.PayCard
import com.store88.pos.ui.theme.PayCash
import com.store88.pos.ui.theme.PayOctopus
import com.store88.pos.ui.theme.PayUnion
import com.store88.pos.ui.theme.PayWeChat
import com.store88.pos.ui.theme.PriceBlue
import com.store88.pos.ui.theme.Sun
import com.store88.pos.ui.theme.Teal
import com.store88.pos.ui.theme.TealBg
import com.store88.pos.ui.theme.TealDark
import com.store88.pos.ui.theme.TextDark
import com.store88.pos.ui.theme.TextMuted

private const val PAGE_SIZE = 10

@Composable
fun CheckoutScreen(ui: UiState, vm: PosViewModel) {
    val state = ui.state
    val priced = Pricing.priceCart(state.cart, state.products, state.promos, state.cartDiscountPercent)
    val itemCount = priced.lines.sumOf { it.qty }
    val filter = ui.categoryFilter
    val filtered = state.products.filter { p ->
        if (!p.active) return@filter false
        when (filter) {
            "favourites" -> p.id in state.favouriteIds
            "all" -> p.category != "custom"
            else -> p.category == filter
        }
    }
    val pageSize = if (filter == "custom") 50 else PAGE_SIZE
    val pages = ((filtered.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    val page = ui.productPage.coerceIn(0, pages - 1)
    val pageItems = filtered.drop(page * pageSize).take(pageSize)
    val cashierZh = com.store88.pos.domain.PosConstants.CASHIERS.find { it.name == state.session?.cashier }?.nameZh
    val brand = vm.bi(state.shopName.ifBlank { "88 Store" }, state.shopNameZh.ifBlank { "88超市" })
    val cartTitle = when (state.uiLanguage) {
        "en" -> "Cart ($itemCount)"
        "zh" -> "購物車 (${itemCount}件)"
        else -> "Cart ($itemCount) / 購物車 (${itemCount}件)"
    }

    Column(Modifier.fillMaxSize().background(CheckoutBg)) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(10.dp, 10.dp, 14.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("☰", fontSize = 20.sp, modifier = Modifier.clickable { vm.setScreen(Screen.Products) }.padding(8.dp))
            Text("☀", color = Sun, fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
            Text(brand, fontWeight = FontWeight.ExtraBold, color = TealDark, fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            Row(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = ui.searchQuery,
                    onValueChange = vm::setSearch,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.submitSearch() }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (ui.searchQuery.isEmpty()) {
                            Text("Search / scan barcode / 搜尋或掃描條碼", color = TextMuted, fontSize = 15.sp)
                        }
                        inner()
                    },
                )
                Text("⌕", color = TextMuted, fontSize = 18.sp, modifier = Modifier.clickable { vm.setShowScan(true) }.padding(8.dp))
            }
            Spacer(Modifier.width(10.dp))
            StatusBlock("👤", "Cashier: ${state.session?.cashier}", "收銀員：${cashierZh ?: state.session?.cashier}")
            StatusBlock("🕐", "Session: ${vm.sessionClock()}", "機號：POS01${if (!ui.online) " · Offline" else ""}")
            Text("⋮", fontSize = 22.sp, modifier = Modifier.clickable { vm.toggleMore() }.padding(8.dp))
        }

        if (ui.showMore) {
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, Border)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item { LangSwitch(state.uiLanguage, vm, compact = true) }
                item { MoreBtn("％ Discount / 折扣") { vm.setShowDiscount(true) } }
                item { MoreBtn("⏸ Hold / 暫停") { vm.holdCart() } }
                item { MoreBtn("🖨 Print / 列印") { vm.printLastReceipt() } }
                item { MoreBtn("💰 Drawer / 開箱") { vm.openDrawer() } }
                item { MoreBtn("📺 客顯") { vm.setShowCustomerPreview(true) } }
                item { MoreBtn("🔄 Sync / 同步") { vm.syncNow() } }
                item { MoreBtn("🧾 Txns / 交易") { vm.setScreen(Screen.Transactions) } }
                item { MoreBtn("📅 Day Close / 日結") { vm.setScreen(Screen.DayClose) } }
            }
        }

        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.weight(1.7f).fillMaxHeight().padding(14.dp, 12.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val cats = listOf(Triple("favourites", "Favourites", "常用") to "⭐") +
                        state.categories.map { Triple(it.id, it.en, it.zh) to it.icon } +
                        listOf(Triple("all", "All", "全部") to "▦")
                    items(cats) { (triple, icon) ->
                        val (id, en, zh) = triple
                        val on = filter == id
                        Column(
                            Modifier
                                .width(92.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) Color(0xFFF8FAFC) else Color.White)
                                .border(1.5.dp, if (on) Color(0xFF94A3B8) else Border, RoundedCornerShape(12.dp))
                                .clickable { vm.setCategory(id) }
                                .padding(10.dp, 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(icon, fontSize = 20.sp)
                            Text(en, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(zh, color = TextMuted, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                if (filter == "custom") {
                    Hint("Tap a price for items without barcode / 無條碼或找不到商品時，點選價錢加入")
                }
                if (filter == "favourites" && filtered.isEmpty()) {
                    Hint("Star products in any category to pin them here for quick add. / 在其他類別點☆把商品加入常用，方便快速落單。")
                }
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(pageItems, key = { it.id }) { p ->
                        val isCustom = p.category == "custom"
                        Box(
                            Modifier
                                .height(if (isCustom) 88.dp else 208.dp)
                                .clip(RoundedCornerShape(if (isCustom) 14.dp else 12.dp))
                                .background(Color.White)
                                .border(if (isCustom) 2.dp else 1.dp, if (isCustom) Color(0xFFCBD5E1) else Border, RoundedCornerShape(if (isCustom) 14.dp else 12.dp))
                                .clickable { vm.addProduct(p.id) },
                        ) {
                            if (isCustom) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("$${p.price.toInt()}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = TealDark)
                                }
                            } else {
                                Column(Modifier.fillMaxSize().padding(8.dp, 8.dp, 8.dp, 10.dp)) {
                                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                        Text(p.emoji, fontSize = 42.sp)
                                    }
                                    Text(p.nameEn, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = TextDark)
                                    Text(p.nameZh, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(Pricing.money(p.price), color = PriceBlue, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                                }
                                val fav = p.id in state.favouriteIds
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (fav) Color(0xFFFFFBEB) else Color.White.copy(alpha = 0.92f))
                                        .clickable { vm.toggleFavourite(p.id) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(if (fav) "★" else "☆", color = if (fav) Color(0xFFF59E0B) else Color(0xFF94A3B8), fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
                if (pages > 1) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                        repeat(pages) { i ->
                            Box(
                                Modifier
                                    .padding(4.dp)
                                    .size(if (i == page) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (i == page) Color(0xFF64748B) else Color(0xFFCBD5E1))
                                    .clickable { vm.setProductPage(i) },
                            )
                        }
                    }
                }
            }

            Column(
                Modifier
                    .width(380.dp)
                    .fillMaxHeight()
                    .background(Color.White)
                    .border(1.dp, Border),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🛒  $cartTitle", fontWeight = FontWeight.Bold, color = Teal, fontSize = 16.sp)
                    Text("🗑 Clear Cart / 清空", color = Danger, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { vm.clearCart() })
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                    if (priced.lines.isEmpty()) {
                        Text("Tap products or scan barcode", color = TextMuted, modifier = Modifier.padding(12.dp))
                    }
                    priced.lines.forEach { line ->
                        val p = state.products.find { it.id == line.productId }
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8FAFC)),
                                contentAlignment = Alignment.Center,
                            ) { Text(p?.emoji ?: "📦", fontSize = 22.sp) }
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(p?.nameEn ?: line.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(p?.nameZh ?: "", color = TextMuted, fontSize = 12.sp, maxLines = 1)
                                Text(line.barcode, color = TextMuted, fontSize = 11.sp)
                                if (!line.promoLabel.isNullOrBlank()) {
                                    Text(line.promoLabel!!, color = Teal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    Modifier
                                        .padding(top = 4.dp)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.5.dp, Teal, RoundedCornerShape(8.dp)),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier.size(28.dp).clickable { vm.setQty(line.productId, line.qty - 1) },
                                        contentAlignment = Alignment.Center,
                                    ) { Text("−", color = Teal, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
                                    Box(
                                        Modifier.width(36.dp).fillMaxHeight().clickable { vm.startQtyEdit(line.productId, line.qty) },
                                        contentAlignment = Alignment.Center,
                                    ) { Text("${line.qty}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
                                    Box(
                                        Modifier.size(28.dp).clickable { vm.setQty(line.productId, line.qty + 1) },
                                        contentAlignment = Alignment.Center,
                                    ) { Text("+", color = Teal, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (line.discount > 0) {
                                    Text(Pricing.money(line.unitPrice * line.qty), color = TextMuted, fontSize = 11.sp, textDecoration = TextDecoration.LineThrough)
                                }
                                Text(Pricing.money(line.lineTotal), color = Teal, fontWeight = FontWeight.ExtraBold)
                            }
                            Box(
                                Modifier
                                    .padding(start = 8.dp)
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .clickable { vm.setQty(line.productId, 0) },
                                contentAlignment = Alignment.Center,
                            ) { Text("×", color = TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                Column(Modifier.padding(12.dp)) {
                    TotalRow("Subtotal / 小計", Pricing.money(priced.subtotal))
                    TotalRow("Discount / 折扣", "-${Pricing.money(priced.discount)}", Color(0xFF16A34A))
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total / 總計", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(Pricing.money(priced.total), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Teal)
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(10.dp, 10.dp, 14.dp, 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PayBtn(PayCash, "💵", "Cash 現金", { vm.goPay(PaymentMethod.CASH) }, Modifier.weight(1f))
            PayBtn(PayOctopus, "🐙", "Octopus 八達通", { vm.goPay(PaymentMethod.OCTOPUS) }, Modifier.weight(1f))
            PayBtn(PayCard, "💳", "Card 信用卡", { vm.goPay(PaymentMethod.CARD) }, Modifier.weight(1f))
            PayBtn(PayWeChat, "💬", "WeChat 微信", { vm.goPay(PaymentMethod.WECHAT) }, Modifier.weight(1f))
            PayBtn(PayUnion, "💳", "UnionPay 銀聯", { vm.goPay(PaymentMethod.UNIONPAY) }, Modifier.weight(1f))
            PayBtn(PayAlipay, "🟦", "Alipay 支付寶", { vm.goPay(PaymentMethod.ALIPAY) }, Modifier.weight(1f))
        }
    }

    if (ui.showScan) {
        Dialog(onDismissRequest = { vm.setShowScan(false) }) {
            Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(20.dp).width(420.dp)) {
                Text("Scan / Search / 掃描搜尋", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                BasicTextField(
                    value = ui.scanQuery,
                    onValueChange = vm::setScanQuery,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { vm.submitScan() }),
                    modifier = Modifier.fillMaxWidth().height(48.dp).border(1.dp, Border, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            if (ui.scanQuery.isEmpty()) Text("Scan or type barcode… / 掃描或輸入條碼", color = TextMuted)
                            inner()
                        }
                    },
                )
                Text(
                    "USB barcode scanner: focus this field, scan → Enter auto-adds to cart. / USB掃描器對準此欄，掃碼後自動加入購物車。",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", modifier = Modifier.clickable { vm.setShowScan(false) }.padding(12.dp))
                    Text("Add to cart", color = Teal, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { vm.submitScan() }.padding(12.dp))
                }
            }
        }
    }
    ui.missingBarcode?.let { code ->
        Dialog(onDismissRequest = { vm.dismissMissing() }) {
            Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(20.dp).width(420.dp)) {
                Text("Barcode not found / 找不到貨品", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Scanned barcode / 掃描條碼：", color = TextMuted, modifier = Modifier.padding(top = 8.dp))
                Text(code, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                Text("Add this product to catalog? / 是否新增此商品到貨品庫？", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.End) {
                    Text("No / 不用", modifier = Modifier.clickable { vm.dismissMissing() }.padding(12.dp))
                    Text("Yes, Add Item / 是，新增商品", color = Teal, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { vm.confirmAddMissingProduct() }.padding(12.dp))
                }
                Text("Tip: or use Custom $ / 亦可先用「自訂價錢」收款", color = TextMuted, fontSize = 12.sp)
            }
        }
    }
    if (ui.showDiscount) {
        Dialog(onDismissRequest = { vm.setShowDiscount(false) }) {
            Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(20.dp).width(360.dp)) {
                Text("Customer discount % / 顧客折扣 %", fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = ui.discountDraft,
                    onValueChange = vm::setDiscountDraft,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).border(1.dp, Border, RoundedCornerShape(10.dp)).padding(12.dp),
                )
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    Text("Cancel", modifier = Modifier.clickable { vm.setShowDiscount(false) }.padding(12.dp))
                    Text("OK", color = Teal, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { vm.confirmDiscount() }.padding(12.dp))
                }
            }
        }
    }
    if (ui.qtyEditId != null) {
        Dialog(onDismissRequest = { vm.applyQtyEdit() }) {
            Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(20.dp).width(280.dp)) {
                Text("Qty / 數量", fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = ui.qtyDraft,
                    onValueChange = vm::setQtyDraft,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).border(1.dp, Border, RoundedCornerShape(10.dp)).padding(12.dp),
                )
                Text("OK", color = Teal, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End).clickable { vm.applyQtyEdit() }.padding(12.dp))
            }
        }
    }
    if (ui.showCustomerPreview) {
        Dialog(onDismissRequest = { vm.setShowCustomerPreview(false) }) {
            Box(Modifier.fillMaxWidth().height(520.dp).clip(RoundedCornerShape(16.dp))) {
                com.store88.pos.ui.customer.CustomerDisplayPreviewHost(ui)
                Text(
                    "✕ Close preview / 關閉預覽",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { vm.setShowCustomerPreview(false) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusBlock(icon: String, en: String, zh: String) {
    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Column {
            Text(en, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(zh, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MoreBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = TextDark,
            maxLines = 1,
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        color = TealDark,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TealBg)
            .border(1.dp, Color(0xFF99F6E4), RoundedCornerShape(10.dp))
            .padding(10.dp, 10.dp, 14.dp, 10.dp),
    )
}

@Composable
private fun TotalRow(label: String, value: String, color: Color = TextDark) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = color, fontSize = 14.sp)
        Text(value, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PayBtn(bg: Color, tile: String, title: String, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(10.dp, 10.dp, 12.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
            contentAlignment = Alignment.Center,
        ) { Text(tile, fontSize = 20.sp) }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1)
            Text("Pay / 付款", color = Color.White.copy(alpha = 0.88f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}
