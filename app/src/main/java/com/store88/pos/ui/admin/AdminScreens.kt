package com.store88.pos.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.store88.pos.ui.theme.TealDark
import com.store88.pos.ui.theme.TextDark
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.store88.pos.PosViewModel
import com.store88.pos.UiState
import com.store88.pos.data.Pricing
import com.store88.pos.domain.Category
import com.store88.pos.domain.PaymentMethod
import com.store88.pos.domain.PosConstants
import com.store88.pos.domain.Product
import com.store88.pos.domain.Promo
import com.store88.pos.domain.Screen
import com.store88.pos.hardware.PrinterConfig
import com.store88.pos.ui.theme.Border
import com.store88.pos.ui.theme.Teal
import com.store88.pos.ui.theme.TealBg
import com.store88.pos.ui.theme.TealHeader
import com.store88.pos.ui.theme.TextMuted

@Composable
fun AdminShell(
    title: String,
    subtitle: String,
    ui: UiState,
    vm: PosViewModel,
    content: @Composable () -> Unit,
) {
    val nav = buildList {
        add(Triple(Screen.Products, "🏷", "Products / 產品"))
        add(Triple(Screen.Categories, "📂", "Categories / 類別管理"))
        add(Triple(Screen.Promotions, "%", "Promotions / 優惠"))
        add(Triple(Screen.Transactions, "🧾", "Transaction Records / 交易紀錄"))
        add(Triple(Screen.Inventory, "📦", "Inventory / 庫存"))
        add(Triple(Screen.Receive, "🛒", "Receive Goods / 收貨"))
        add(Triple(Screen.Reports, "📈", "Reports / 報表"))
        add(Triple(Screen.Expiry, "⏰", "Near Expiry / 到期"))
        add(Triple(Screen.ReceiptDesign, "🖨", "Receipt Design / 單據範本"))
        if (vm.isTillAdmin()) {
            add(Triple(Screen.PrinterSettings, "🔌", "Printer / Sync / 打印機／同步"))
        }
    }
    Row(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Column(
            Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(Color.White)
                .border(1.dp, Border)
                .padding(10.dp, 16.dp),
        ) {
            Text(
                "← ${vm.bi("Back to POS", "返回收銀")}",
                color = Teal,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { vm.setScreen(Screen.Checkout) }.padding(10.dp),
            )
            Row(
                Modifier.fillMaxWidth().clickable { vm.setScreen(Screen.Checkout) }.padding(8.dp, 8.dp, 10.dp, 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Teal),
                    contentAlignment = Alignment.Center,
                ) { Text("🛒", fontSize = 16.sp) }
                Spacer(Modifier.width(10.dp))
                Text("88 Store Management", fontWeight = FontWeight.ExtraBold, color = TealDark, fontSize = 13.sp)
            }
            nav.forEach { (screen, icon, label) ->
                val on = ui.screen == screen
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) Teal else Color.Transparent)
                        .clickable { vm.setScreen(screen) }
                        .padding(11.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$icon  ${vm.bi(label.substringBefore(" / "), label.substringAfter(" / ", label))}", color = if (on) Color.White else TextDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            com.store88.pos.ui.components.LangSwitch(ui.state.uiLanguage, vm, compact = false)
            Text(
                vm.bi("Day Close", "日結"),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().clickable { vm.setScreen(Screen.DayClose) }.padding(12.dp),
            )
            Text(
                vm.bi("Logout", "登出"),
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB91C1C),
                modifier = Modifier.fillMaxWidth().clickable { vm.logoutCashier() }.padding(12.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Column(Modifier.fillMaxWidth().background(Color.White).border(1.dp, Border).padding(16.dp, 24.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(subtitle, color = TextMuted, fontSize = 14.sp)
            }
            Column(Modifier.weight(1f).padding(16.dp, 24.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ProductsAdmin(ui: UiState, vm: PosViewModel) {
    val q = ui.adminSearch.trim().lowercase()
    val list = ui.state.products.filter {
        q.isEmpty() ||
            it.nameEn.lowercase().contains(q) ||
            it.nameZh.contains(q) ||
            it.barcode.contains(q) ||
            it.category.lowercase().contains(q)
    }
    var editing by remember { mutableStateOf<Product?>(null) }
    var isNew by remember { mutableStateOf(false) }

    fun startAdd(barcode: String = "") {
        val defaultCat = ui.state.categories.find { it.id == "drinks" }?.id
            ?: ui.state.categories.find { it.id != "custom" }?.id
            ?: "drinks"
        isNew = true
        editing = Product(
            id = "p-${System.currentTimeMillis()}",
            nameEn = "",
            nameZh = "",
            barcode = barcode,
            category = defaultCat,
            cost = 5.0,
            price = 10.0,
            stock = 50,
            reorderMin = 10,
            trackExpiry = false,
            emoji = "📦",
            active = true,
        )
    }

    LaunchedEffect(ui.prefillBarcode) {
        if (ui.prefillBarcode.isNotBlank()) {
            startAdd(ui.prefillBarcode)
            vm.consumePrefill()
        }
    }

    AdminShell("Products / 產品", "Manage your product catalog. / 管理商品目錄。", ui, vm) {
        BasicTextField(
            value = ui.adminSearch,
            onValueChange = vm::setAdminSearch,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(10.dp),
            decorationBox = { inner ->
                if (ui.adminSearch.isEmpty()) {
                    Text("Search by name (EN/繁中), barcode, category…", color = TextMuted)
                }
                inner()
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { startAdd() },
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
        ) { Text("+ Add Product / 新增產品") }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list, key = { it.id }) { p ->
                val low = p.stock <= p.reorderMin
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.emoji.take(2).ifBlank { "📦" }, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.nameEn, fontWeight = FontWeight.SemiBold)
                        Text(p.nameZh, color = TextMuted, fontSize = 12.sp)
                        Text("${p.barcode} · ${p.category}", color = TextMuted, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Cost ${Pricing.money(p.cost)}", fontSize = 11.sp, color = TextMuted)
                        Text(Pricing.money(p.price), fontWeight = FontWeight.Bold)
                        Text(
                            "Stock ${p.stock}",
                            color = if (low) Color.Red else Teal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                    TextButton(onClick = {
                        isNew = false
                        editing = p
                    }) { Text("✏ Edit") }
                    TextButton(onClick = { vm.deleteProduct(p.id) }) {
                        Text("🗑 Delete", color = Color.Red)
                    }
                }
            }
        }
    }

    val draft = editing
    if (draft != null) {
        Dialog(onDismissRequest = { editing = null }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    if (isNew) "New Product / 新增產品" else "Edit Product / 編輯產品",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
                FunField("Name EN / 英文名稱 *", draft.nameEn) { editing = draft.copy(nameEn = it) }
                FunField("Name ZH / 中文名稱 *", draft.nameZh) { editing = draft.copy(nameZh = it) }
                FunField("Barcode / 條碼 *", draft.barcode) { editing = draft.copy(barcode = it) }
                FunField("Category / 類別", draft.category) { editing = draft.copy(category = it) }
                FunField("Cost Price ($) / 成本價", draft.cost.toString()) {
                    editing = draft.copy(cost = it.toDoubleOrNull() ?: draft.cost)
                }
                FunField("Selling Price ($) / 售價 *", draft.price.toString()) {
                    editing = draft.copy(price = it.toDoubleOrNull() ?: draft.price)
                }
                FunField("Stock Qty / 庫存數量", draft.stock.toString()) {
                    editing = draft.copy(stock = it.toIntOrNull() ?: draft.stock)
                }
                FunField("Reorder Min / 低庫存警戒", draft.reorderMin.toString()) {
                    editing = draft.copy(reorderMin = it.toIntOrNull() ?: draft.reorderMin)
                }
                FunField("Image URL or Emoji / 圖片連結或圖示", draft.emoji) {
                    editing = draft.copy(emoji = it)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = draft.trackExpiry,
                        onCheckedChange = { editing = draft.copy(trackExpiry = it) },
                    )
                    Text("Track expiry / 追蹤到期")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = draft.active,
                        onCheckedChange = { editing = draft.copy(active = it) },
                    )
                    Text("Active / 上架")
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { editing = null }) { Text("Cancel / 取消") }
                    Button(
                        onClick = {
                            if (draft.nameEn.isBlank() || draft.barcode.isBlank()) return@Button
                            vm.upsertProduct(draft)
                            editing = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) { Text("Save Product / 儲存") }
                }
            }
        }
    }
}

@Composable
fun CategoriesAdmin(ui: UiState, vm: PosViewModel) {
    var id by remember { mutableStateOf("") }
    var en by remember { mutableStateOf("") }
    var zh by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🏷️") }
    AdminShell("Categories / 類別管理", "Add, edit, reorder, or remove POS product categories. / 新增、編輯、排序或刪除收銀類別。", ui, vm) {
        FunField("ID", id) { id = it }
        FunField("Name EN", en) { en = it }
        FunField("Name ZH", zh) { zh = it }
        FunField("Icon", icon) { icon = it }
        Button(
            onClick = {
                if (id.isBlank() || en.isBlank()) return@Button
                vm.upsertCategory(Category(id = id.trim(), en = en, zh = zh, icon = icon, system = false))
                id = ""; en = ""; zh = ""; icon = "🏷️"
            },
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
        ) { Text("+ Add category / 新增類別") }
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(ui.state.categories, key = { it.id }) { c ->
                Row(
                    Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${c.icon} ${c.en} / ${c.zh}")
                    if (!c.system) {
                        TextButton(onClick = { vm.deleteCategory(c.id) }) {
                            Text("Delete / 刪除", color = Color.Red)
                        }
                    } else {
                        Text("system", color = TextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun PromosAdmin(ui: UiState, vm: PosViewModel) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("buy_x_percent") }
    var buyQty by remember { mutableStateOf("2") }
    var percent by remember { mutableStateOf("10") }
    var amount by remember { mutableStateOf("5") }
    var freeQty by remember { mutableStateOf("1") }
    var bundle by remember { mutableStateOf("10") }
    var categoryId by remember { mutableStateOf("drinks") }
    var start by remember { mutableStateOf(Pricing.todayISO()) }
    var end by remember { mutableStateOf("2099-12-31") }
    AdminShell("Promotions / 優惠", "Create buy-X-get-discount rules for your store. / 設定買滿折扣優惠規則。", ui, vm) {
        FunField("Name / 名稱", name) { name = it }
        FunField("Type (buy_x_percent / buy_x_amount / buy_x_get_y / buy_x_for_price)", type) { type = it }
        FunField("Buy qty / 買滿數量", buyQty) { buyQty = it }
        FunField("Discount %", percent) { percent = it }
        FunField("Discount amount $", amount) { amount = it }
        FunField("Get free qty", freeQty) { freeQty = it }
        FunField("Bundle price $", bundle) { bundle = it }
        FunField("Category ID", categoryId) { categoryId = it }
        FunField("Start date", start) { start = it }
        FunField("End date", end) { end = it }
        Button(
            onClick = {
                vm.upsertPromo(
                    Promo(
                        id = "pr-${System.currentTimeMillis()}",
                        name = name.ifBlank { "Promo $type" },
                        type = type,
                        buyQty = buyQty.toIntOrNull() ?: 2,
                        discountPercent = percent.toDoubleOrNull(),
                        discountAmount = amount.toDoubleOrNull(),
                        getFreeQty = freeQty.toIntOrNull(),
                        bundlePrice = bundle.toDoubleOrNull(),
                        categoryId = categoryId.ifBlank { null },
                        productIds = null,
                        startDate = start,
                        endDate = end,
                        active = true,
                    ),
                )
                name = ""
            },
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
        ) { Text("Create promotion / 新增優惠") }
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(ui.state.promos, key = { it.id }) { p ->
                Row(
                    Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.SemiBold)
                        Text("${p.type} · buy ${p.buyQty} · ${p.startDate}–${p.endDate}", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = p.active,
                        onCheckedChange = { vm.upsertPromo(p.copy(active = it)) },
                    )
                    TextButton(onClick = { vm.deletePromo(p.id) }) {
                        Text("Delete", color = Color.Red)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun InventoryAdmin(ui: UiState, vm: PosViewModel) {
    var lowOnly by remember { mutableStateOf(false) }
    var invQuery by remember { mutableStateOf("") }
    val q = invQuery.trim().lowercase()
    val list = ui.state.products
        .filter { it.category != "custom" && it.active }
        .filter { !lowOnly || it.stock <= it.reorderMin }
        .filter {
            q.isEmpty() ||
                it.nameEn.lowercase().contains(q) ||
                it.nameZh.contains(invQuery.trim()) ||
                it.barcode.contains(invQuery.trim()) ||
                it.category.lowercase().contains(q)
        }
        .sortedBy { it.nameEn }
    val lowCount = ui.state.products.count { it.category != "custom" && it.active && it.stock <= it.reorderMin }
    AdminShell("Inventory / 庫存", "Stock list with barcode search. / 庫存清單（可搜尋條碼）。", ui, vm) {
        BasicTextField(
            value = invQuery,
            onValueChange = { invQuery = it },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(1.dp, Border, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (invQuery.isEmpty()) Text("Search products… / 搜尋商品名稱、條碼、類別", color = TextMuted)
                    inner()
                }
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = lowOnly, onCheckedChange = { lowOnly = it })
            Text(vm.bi("Low stock only", "只顯示低庫存"))
            if (lowCount > 0) {
                Text("  $lowCount", color = Color.Red, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text("${list.size} items / 件", color = TextMuted, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list, key = { it.id }) { p ->
                val low = p.stock <= p.reorderMin
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (low) Color(0xFFFFF7ED) else Color.White)
                        .border(1.dp, if (low) Color(0xFFFDBA74) else Border, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${p.nameEn} / ${p.nameZh}", fontWeight = FontWeight.SemiBold)
                        Text(p.barcode, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TealDark)
                        Text(p.category, color = TextMuted, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("On Hand / 現貨 ${p.stock}", fontWeight = FontWeight.Bold, color = if (low) Color.Red else Teal)
                        Text("Reorder min ${p.reorderMin}", color = TextMuted, fontSize = 12.sp)
                        Text(Pricing.money(p.price), fontWeight = FontWeight.Bold)
                        Text(
                            if (low) "Low stock / 低庫存" else "OK / 充足",
                            color = if (low) Color(0xFFD97706) else Teal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpiryAdmin(ui: UiState, vm: PosViewModel) {
    val rows = ui.state.lots.filter { it.qty > 0 }.sortedBy { it.expiryDate }
    AdminShell("Near Expiry / 到期", "Lot expiry tracking (view only). / 批次到期追蹤（只供查看）。", ui, vm) {
        if (rows.isEmpty()) {
            Text("No lots tracked / 暫無批次紀錄", color = TextMuted)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rows, key = { it.id }) { lot ->
                val p = ui.state.products.find { it.id == lot.productId }
                val days = Pricing.daysUntil(lot.expiryDate)
                val badgeColor = when {
                    days <= 2 -> Color.Red
                    days <= 7 -> Color(0xFFD97706)
                    else -> Teal
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.dp, Border, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p?.let { "${it.nameEn} / ${it.nameZh}" } ?: lot.productId, fontWeight = FontWeight.SemiBold)
                        Text("Lot ${lot.lotNumber} · barcode ${p?.barcode ?: "—"}", color = TextMuted, fontSize = 12.sp)
                        Text("Expiry ${lot.expiryDate} · qty ${lot.qty}", color = TextMuted, fontSize = 12.sp)
                    }
                    Text(
                        "${days}d",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiveAdmin(ui: UiState, vm: PosViewModel) {
    var productId by remember { mutableStateOf(ui.state.products.firstOrNull { it.category != "custom" }?.id ?: "") }
    var qty by remember { mutableStateOf("10") }
    var lot by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf(Pricing.todayISO()) }
    AdminShell("Receive Goods / 收貨", "Add stock and optional lots. / 入貨及建立批次。", ui, vm) {
        Text(vm.bi("Product id", "貨品 ID"), color = TextMuted)
        BasicTextField(productId, { productId = it }, Modifier.fillMaxWidth().background(Color.White).padding(10.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(vm.bi("Qty", "數量"), color = TextMuted)
        BasicTextField(qty, { qty = it }, Modifier.fillMaxWidth().background(Color.White).padding(10.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(vm.bi("Lot (optional)", "批次（可選）"), color = TextMuted)
        BasicTextField(lot, { lot = it }, Modifier.fillMaxWidth().background(Color.White).padding(10.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(vm.bi("Expiry YYYY-MM-DD", "到期日"), color = TextMuted)
        BasicTextField(expiry, { expiry = it }, Modifier.fillMaxWidth().background(Color.White).padding(10.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                vm.receiveStock(productId, qty.toIntOrNull() ?: 0, lot.ifBlank { null }, expiry.ifBlank { null })
            },
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
        ) { Text(vm.bi("Receive", "確認收貨")) }
    }
}

@Composable
fun ReportsAdmin(ui: UiState, vm: PosViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val todayKey = Pricing.todayISO()
    val thisMonthKey = todayKey.slice(0..6)
    var mode by remember { mutableStateOf("daily") }
    var day by remember { mutableStateOf(todayKey) }
    var month by remember { mutableStateOf(thisMonthKey) }
    val periodKey = if (mode == "daily") day else month
    val periodSales = remember(ui.state.sales, mode, day, month) {
        if (mode == "daily") ui.state.sales.filter { it.at.slice(0..9) == day }
        else ui.state.sales.filter { it.at.slice(0..6) == month }
    }
    val total = periodSales.sumOf { it.total }
    val avg = if (periodSales.isNotEmpty()) total / periodSales.size else 0.0
    val br = remember(periodSales) {
        val m: MutableMap<PaymentMethod, Double> = PaymentMethod.entries.associateWith { 0.0 }.toMutableMap()
        for (s in periodSales) {
            val pm = PaymentMethod.fromId(s.payment)
            m[pm] = (m[pm] ?: 0.0) + s.total
        }
        m
    }
    val productStats = remember(periodSales) {
        val map = LinkedHashMap<String, ProdStat>()
        for (s in periodSales) {
            for (l in s.lines) {
                val key = l.productId.ifBlank { l.barcode.ifBlank { l.name } }
                val cur = map[key]
                map[key] = ProdStat(
                    idx = 0,
                    name = l.name.ifBlank { cur?.name ?: key },
                    barcode = l.barcode.ifBlank { cur?.barcode ?: "" },
                    qty = (cur?.qty ?: 0) + l.qty,
                    amt = (cur?.amt ?: 0.0) + l.lineTotal,
                )
            }
        }
        map.values.sortedByDescending { it.amt }
    }
    val top = productStats.take(10)
    val dailyInMonth = remember(periodSales, mode) {
        if (mode != "monthly") emptyList<DailyStat>()
        else {
            val map = LinkedHashMap<String, DailyStat>()
            for (s in periodSales) {
                val key = s.at.slice(0..9)
                val cur = map[key] ?: DailyStat(key, 0, 0.0)
                map[key] = cur.copy(orders = cur.orders + 1, total = cur.total + s.total)
            }
            map.values.sortedByDescending { it.date }
        }
    }
    val emptyMsg = if (mode == "daily")
        "No sales on $day — pick another date or complete a checkout."
        else "No sales in $month — pick another month or complete a checkout."

    fun copyCsv(name: String, rows: List<List<String>>) {
        val csv = rows.joinToString("\n") { it.joinToString(",") }
        clipboard.setText(androidx.compose.ui.text.AnnotatedString(csv))
        android.widget.Toast.makeText(ctx, "CSV copied: $name (${rows.size - 1} rows)", android.widget.Toast.LENGTH_LONG).show()
    }

    AdminShell("Reports / 報表", "Daily & monthly sales, payment mix, and top products. / 每日及每月報表、付款統計及熱賣貨品。", ui, vm) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            // Tabs
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F5F9)).border(1.dp, Border, RoundedCornerShape(12.dp)).padding(4.dp),
            ) {
                listOf("daily" to "Daily Report / 每日報表", "monthly" to "Monthly Report / 每月報表").forEach { (m, label) ->
                    val on = mode == m
                    Text(
                        label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (on) TealDark else TextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (on) Color.White else Color.Transparent)
                            .clickable { mode = m }
                            .padding(10.dp, 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // Period picker
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (mode == "daily") "Date / 日期" else "Month / 月份", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 13.sp)
                if (mode == "daily") {
                    Text(
                        "◀ Day before / 前一日",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TealDark,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Border, RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .clickable {
                                day = java.time.LocalDate.parse(day).minusDays(1).toString()
                            }
                            .padding(10.dp, 8.dp),
                    )
                }
                BasicTextField(
                    value = if (mode == "daily") day else month,
                    onValueChange = { if (mode == "daily") day = it else month = it },
                    singleLine = true,
                    modifier = Modifier
                        .width(160.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.dp, Border, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                    decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() } },
                )
                if (mode == "daily") {
                    Text(
                        "Day after / 後一日 ▶",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TealDark,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Border, RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .clickable {
                                day = java.time.LocalDate.parse(day).plusDays(1).toString()
                            }
                            .padding(10.dp, 8.dp),
                    )
                }
                if (mode == "daily" && day != todayKey) {
                    Text("Today / 今天", color = Teal, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { day = todayKey }.padding(8.dp))
                }
                if (mode == "monthly" && month != thisMonthKey) {
                    Text("This Month / 本月", color = Teal, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { month = thisMonthKey }.padding(8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            // Export buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⬇ Product Sales CSV / 商品銷售", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Teal).clickable {
                    val rows = listOf(listOf("period", "product_id", "barcode", "product_name", "qty_sold", "sales_amount", "pct_of_sales")) +
                        productStats.map { p -> listOf(periodKey, "", p.barcode, p.name, p.qty.toString(), "%.2f".format(p.amt), if (total > 0) "%.2f".format(p.amt / total * 100) else "0.00") }
                    copyCsv("product-sales-$periodKey", rows)
                }.padding(10.dp, 8.dp))
                Text("⬇ Payment CSV / 付款", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TealDark, modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Border, RoundedCornerShape(10.dp)).background(Color.White).clickable {
                    val rows = listOf(listOf("period", "payment_method", "amount")) +
                        PosConstants.PAYMENTS.map { listOf(periodKey, "${it.en} / ${it.zh}", "%.2f".format(br[it.method] ?: 0.0)) } +
                        listOf(listOf(periodKey, "TOTAL", "%.2f".format(total)))
                    copyCsv("payment-mix-$periodKey", rows)
                }.padding(10.dp, 8.dp))
            }
            Spacer(Modifier.height(16.dp))

            // KPI cards
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    (if (mode == "daily") "Day sales / 當日營業額" else "Month sales / 當月營業額") to Pricing.money(total),
                    "Orders / 訂單數" to periodSales.size.toString(),
                    "Avg ticket / 客單價" to Pricing.money(avg),
                    "Products sold / 有售商品" to productStats.size.toString(),
                ).forEach { (label, value) ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.White).border(1.dp, Border, RoundedCornerShape(12.dp)).padding(14.dp),
                    ) {
                        Text(label, color = TextMuted, fontSize = 12.sp)
                        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = TealDark, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // Daily breakdown (monthly only)
            if (mode == "monthly") {
                Text("Daily Breakdown / 每日明細（$month）", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                ReportHeader(listOf("Date / 日期", "Orders / 訂單數", "Total / 總額", "Avg / 客單價"))
                if (dailyInMonth.isEmpty()) {
                    Text(emptyMsg, color = TextMuted, modifier = Modifier.padding(16.dp))
                }
            dailyInMonth.forEach { stat ->
                val dAvg = if (stat.orders > 0) stat.total / stat.orders else 0.0
                ReportRow(listOf(stat.date, stat.orders.toString(), Pricing.money(stat.total), Pricing.money(dAvg)), boldIdx = 2, boldColor = Teal)
            }
                Spacer(Modifier.height(20.dp))
            }

            // Payment mix
            Text("Payment Mix / 付款方式統計（$periodKey）", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            ReportHeader(listOf("Payment Method / 付款方式", "Total Amount / 金額"))
            PosConstants.PAYMENTS.forEach { p ->
                ReportRow(listOf("${p.icon}  ${vm.bi(p.en, p.zh)}", Pricing.money(br[p.method] ?: 0.0)), boldIdx = 1)
            }
            Spacer(Modifier.height(20.dp))

            // Top products
            Text("Product Sales / 商品銷售（$periodKey）", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Top ${top.size} of ${productStats.size} — export CSV for full list", color = TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            ReportHeader(listOf("#", "Barcode / 條碼", "Product Name / 商品", "Qty / 數量", "Sales / 金額", "% of Sales"))
            if (top.isEmpty()) {
                Text(emptyMsg, color = TextMuted, modifier = Modifier.padding(16.dp))
            }
            top.forEachIndexed { i, p ->
                val pct = if (total > 0) "%.1f".format(p.amt / total * 100) else "0.0"
                ReportRow(listOf((i + 1).toString(), p.barcode.ifBlank { "—" }, p.name, "${p.qty} pcs / 件", Pricing.money(p.amt), "$pct%"), boldIdx = 4, boldColor = Teal)
            }
        }
    }
}

private data class ProdStat(val idx: Int, val name: String, val barcode: String = "", val qty: Int, val amt: Double)
private data class DailyStat(val date: String, val orders: Int, val total: Double)

@Composable
private fun ReportHeader(cols: List<String>) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF1F5F9)).border(1.dp, Border).padding(10.dp, 8.dp)) {
        cols.forEachIndexed { i, c ->
            Text(c, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark, modifier = Modifier.weight(if (i == 2) 1.4f else 1f))
        }
    }
}

@Composable
private fun ReportRow(cols: List<String>, boldIdx: Int = -1, boldColor: Color = TextDark) {
    Row(Modifier.fillMaxWidth().background(Color.White).border(1.dp, Border).padding(10.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
        cols.forEachIndexed { i, c ->
            Text(
                c,
                fontWeight = if (i == boldIdx) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (i == boldIdx) boldColor else TextDark,
                fontSize = 13.sp,
                modifier = Modifier.weight(if (i == 2) 1.4f else 1f),
            )
        }
    }
}

@Composable
fun TransactionsAdmin(ui: UiState, vm: PosViewModel) {
    var pendingAction by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // saleId to asRefund
    var managerPin by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    pendingAction?.let { (saleId, asRefund) ->
        Dialog(onDismissRequest = {
            pendingAction = null
            managerPin = ""
            reason = ""
        }) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(20.dp)
                    .width(360.dp),
            ) {
                Text(
                    if (asRefund) "Refund / 退款 — $saleId" else "Void / 作廢 — $saleId",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    "Manager PIN required (default 9999). Restores stock. / 需主管密碼，會還原庫存。",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
                Text("Manager PIN / 主管密碼", color = TextMuted, fontSize = 12.sp)
                BasicTextField(
                    value = managerPin,
                    onValueChange = { managerPin = it.filter { ch -> ch.isDigit() }.take(6) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("Reason (optional) / 原因", color = TextMuted, fontSize = 12.sp)
                BasicTextField(
                    value = reason,
                    onValueChange = { reason = it.take(80) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        pendingAction = null
                        managerPin = ""
                        reason = ""
                    }) { Text("Cancel") }
                    Button(
                        onClick = {
                            vm.voidOrRefundSale(saleId, asRefund, managerPin, reason)
                            pendingAction = null
                            managerPin = ""
                            reason = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (asRefund) Color(0xFFEA580C) else Color(0xFFDC2626),
                        ),
                    ) {
                        Text(if (asRefund) "Confirm refund" else "Confirm void")
                    }
                }
            }
        }
    }

    AdminShell("Transaction Records / 交易紀錄", "View past orders, void/refund with manager PIN, reprint. / 查看訂單、主管作廢/退款、重印。", ui, vm) {
        LazyColumn {
            items(ui.state.sales) { sale ->
                val statusColor = when (sale.status) {
                    "voided" -> Color(0xFFDC2626)
                    "refunded" -> Color(0xFFEA580C)
                    else -> Teal
                }
                Row(
                    Modifier.fillMaxWidth().background(Color.White).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(sale.id, fontWeight = FontWeight.SemiBold)
                            if (!sale.isActive) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    sale.status.uppercase(),
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Text(
                            "${sale.at.take(19)} · ${sale.payment} · ${sale.cashier}",
                            color = TextMuted,
                            fontSize = 12.sp,
                        )
                        if (!sale.voidReason.isNullOrBlank()) {
                            Text("Reason: ${sale.voidReason}", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    Text(
                        Pricing.money(sale.total),
                        fontWeight = FontWeight.Bold,
                        color = if (sale.isActive) TextDark else TextMuted,
                    )
                    TextButton(onClick = { vm.printSale(sale) }) {
                        Text(vm.bi("Reprint", "重印"))
                    }
                    if (sale.isActive) {
                        TextButton(onClick = { pendingAction = sale.id to false }) {
                            Text(vm.bi("Void", "作廢"), color = Color(0xFFDC2626))
                        }
                        TextButton(onClick = { pendingAction = sale.id to true }) {
                            Text(vm.bi("Refund", "退款"), color = Color(0xFFEA580C))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun ReceiptDesignAdmin(ui: UiState, vm: PosViewModel) {
    val cfg = ui.state.receiptConfig
    val sample = ui.state.sales.firstOrNull() ?: com.store88.pos.domain.Sale(
        id = "TX-8849201",
        at = Pricing.nowISO(),
        cashier = ui.state.session?.cashier ?: "Chan Tai Man",
        lines = listOf(
            com.store88.pos.domain.SaleLine(
                productId = "p1",
                name = "Coca-Cola 330ml / 可口可樂",
                barcode = "4890008000510",
                qty = 2,
                unitPrice = 8.0,
                lineTotal = 16.0,
            ),
            com.store88.pos.domain.SaleLine(
                productId = "p7",
                name = "Doritos Nacho Cheese 198g",
                barcode = "4891671175234",
                qty = 1,
                unitPrice = 15.0,
                lineTotal = 15.0,
            ),
        ),
        subtotal = 31.0,
        discount = 2.0,
        total = 29.0,
        payment = "cash",
        cashTendered = 50.0,
        change = 21.0,
    )
    AdminShell("Receipt Design / 單據範本", "Customize thermal printer receipt header, footer, paper width, and barcode. / 自訂收據抬頭、頁尾、紙寬及條碼。", ui, vm) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1.1f).verticalScroll(rememberScrollState())) {
                FunField("Shop Name / 店舖名稱", cfg.shopName) { vm.updateReceiptConfig(cfg.copy(shopName = it)) }
                FunField("Address / 地址", cfg.address) { vm.updateReceiptConfig(cfg.copy(address = it)) }
                FunField("Tel / 電話", cfg.tel) { vm.updateReceiptConfig(cfg.copy(tel = it)) }
                FunField("BR / 商業登記", cfg.brNo) { vm.updateReceiptConfig(cfg.copy(brNo = it)) }
                FunField("Header / 頁首", cfg.headerMsg) { vm.updateReceiptConfig(cfg.copy(headerMsg = it)) }
                FunField("Footer / 頁尾", cfg.footerMsg) { vm.updateReceiptConfig(cfg.copy(footerMsg = it)) }
                FunField("Paper width (80mm / 58mm)", cfg.paperWidth) {
                    vm.updateReceiptConfig(cfg.copy(paperWidth = it))
                }
                Text("Visibility / 顯示項目", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
                listOf(
                    "Logo" to cfg.showLogo,
                    "Address" to cfg.showAddress,
                    "Tel" to cfg.showTel,
                    "BR" to cfg.showBr,
                    "Header msg" to cfg.showHeaderMsg,
                    "Footer msg" to cfg.showFooterMsg,
                    "Receipt no" to cfg.showReceiptNo,
                    "Date/time" to cfg.showDateTime,
                    "Cashier" to cfg.showCashier,
                    "Barcode" to cfg.showBarcode,
                ).chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { (label, on) ->
                            Row(
                                Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (on) TealBg else Color(0xFFF8FAFC)).padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Switch(
                                    checked = on,
                                    onCheckedChange = { checked ->
                                        vm.updateReceiptConfig(
                                            when (label) {
                                                "Logo" -> cfg.copy(showLogo = checked)
                                                "Address" -> cfg.copy(showAddress = checked)
                                                "Tel" -> cfg.copy(showTel = checked)
                                                "BR" -> cfg.copy(showBr = checked)
                                                "Header msg" -> cfg.copy(showHeaderMsg = checked)
                                                "Footer msg" -> cfg.copy(showFooterMsg = checked)
                                                "Receipt no" -> cfg.copy(showReceiptNo = checked)
                                                "Date/time" -> cfg.copy(showDateTime = checked)
                                                "Cashier" -> cfg.copy(showCashier = checked)
                                                else -> cfg.copy(showBarcode = checked)
                                            },
                                        )
                                    },
                                )
                                Text("Show $label", fontSize = 12.sp)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.testPrint() }, colors = ButtonDefaults.buttonColors(containerColor = Teal), modifier = Modifier.fillMaxWidth()) {
                    Text(vm.bi("Test print", "測試列印"))
                }
            }
            Column(
                Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(16.dp),
            ) {
                Text("Live Preview / 即時預覽", fontWeight = FontWeight.Bold, color = TealDark)
                Text("Updates as you edit settings / 修改設定即時更新", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                ReceiptPreviewPaper(sale = sample, config = cfg)
            }
        }
    }
}

@Composable
private fun ReceiptPreviewPaper(sale: com.store88.pos.domain.Sale, config: com.store88.pos.domain.ReceiptConfig) {
    val paperW = if (config.paperWidth.contains("58")) 240.dp else 300.dp
    Column(
        Modifier
            .width(paperW)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .border(1.dp, Border, RoundedCornerShape(4.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (config.showLogo) Text("☀", fontSize = 18.sp, color = Color(0xFFFBBF24))
        Text(config.shopName, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (config.showAddress && config.address.isNotBlank()) {
            Text(config.address, fontSize = 11.sp, color = TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        val telBr = listOfNotNull(
            config.tel.takeIf { config.showTel && it.isNotBlank() }?.let { "Tel: $it" },
            config.brNo.takeIf { config.showBr && it.isNotBlank() }?.let { "BR: $it" },
        ).joinToString(" · ")
        if (telBr.isNotBlank()) Text(telBr, fontSize = 11.sp, color = TextMuted)
        if (config.showHeaderMsg && config.headerMsg.isNotBlank()) {
            Text(config.headerMsg, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        ReceiptDivider()
        Column(Modifier.fillMaxWidth()) {
            if (config.showReceiptNo) Text("Receipt #: ${sale.id}", fontSize = 12.sp)
            if (config.showDateTime) Text("Date/Time: ${sale.at.replace('T', ' ').take(19)}", fontSize = 12.sp)
            if (config.showCashier) Text("Cashier: ${sale.cashier}", fontSize = 12.sp)
        }
        ReceiptDivider()
        sale.lines.forEach { l ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(l.name, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 2)
                Text("HK$%.2f".format(l.lineTotal), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text("${l.qty} × HK$%.2f".format(l.unitPrice), fontSize = 11.sp, color = TextMuted)
            Spacer(Modifier.height(4.dp))
        }
        ReceiptDivider()
        PreviewTotalRow("Subtotal / 小計:", "HK$%.2f".format(sale.subtotal))
        if (sale.discount > 0) PreviewTotalRow("Discount / 折扣:", "-HK$%.2f".format(sale.discount), Color(0xFF059669))
        PreviewTotalRow("TOTAL / 合計:", "HK$%.2f".format(sale.total), TealDark, bold = true)
        PreviewTotalRow("Payment / 付款方式:", sale.payment.uppercase())
        sale.cashTendered?.let { PreviewTotalRow("Cash Tendered / 現金:", "HK$%.2f".format(it)) }
        sale.change?.let { PreviewTotalRow("Change / 找續:", "HK$%.2f".format(it)) }
        ReceiptDivider()
        if (config.showFooterMsg && config.footerMsg.isNotBlank()) {
            Text(config.footerMsg, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        if (config.showBarcode) {
            Spacer(Modifier.height(8.dp))
            Text("|||| ${sale.id.take(12)} ||||", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ReceiptDivider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(Border))
}

@Composable
private fun PreviewTotalRow(label: String, value: String, color: Color = TextDark, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = if (bold) 13.sp else 12.sp, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal, color = color)
        Text(value, fontSize = if (bold) 13.sp else 12.sp, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun FunField(label: String, value: String, onChange: (String) -> Unit) {
    Text(label, color = TextMuted, fontSize = 12.sp)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text("—", color = TextMuted)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() }
        },
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun PrinterSettingsAdmin(ui: UiState, vm: PosViewModel) {
    if (!vm.isTillAdmin()) {
        LaunchedEffect(Unit) {
            vm.setScreen(Screen.Checkout)
        }
        return
    }
    var host by remember(ui.printer) { mutableStateOf(ui.printer.host) }
    var port by remember(ui.printer) { mutableStateOf(ui.printer.port.toString()) }
    var dry by remember(ui.printer) { mutableStateOf(ui.printer.dryRun) }
    var paper by remember(ui.printer) { mutableStateOf(ui.printer.paperWidthMm) }
    var apiBase by remember(ui.sync) { mutableStateOf(ui.sync.baseUrl) }
    var apiKey by remember(ui.sync) { mutableStateOf(ui.sync.apiKey) }
    var shopCode by remember(ui.sync) { mutableStateOf(ui.sync.shopCode) }
    var periodic by remember(ui.sync) { mutableStateOf(ui.sync.periodicUploadEnabled) }
    AdminShell("Printer / Sync", "LAN printer + multi-shop sync. / 打印機及多店同步。", ui, vm) {
        Text(vm.bi("Printer IP (LAN ESC/POS)", "打印機 IP"), color = TextMuted)
        BasicTextField(host, { host = it }, Modifier.fillMaxWidth().background(Color.White).padding(10.dp))
        Spacer(Modifier.height(8.dp))
        Text("Port", color = TextMuted)
        BasicTextField(port, { port = it }, Modifier.fillMaxWidth().background(Color.White).padding(10.dp))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = dry, onCheckedChange = { dry = it })
            Text(vm.bi("Dry-run (no hardware)", "模擬模式（無硬件）"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { paper = 80 }) { Text("80mm") }
            Button(onClick = { paper = 58 }) { Text("58mm") }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                vm.savePrinter(
                    PrinterConfig(
                        host = host.trim(),
                        port = port.toIntOrNull() ?: 9100,
                        paperWidthMm = paper,
                        dryRun = dry,
                        printOnPayDefault = ui.printOnPay,
                    ),
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(vm.bi("Save printer", "儲存打印機")) }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.pingPrinter() }) { Text(vm.bi("Test connection", "測試連線")) }
            Button(onClick = { vm.testPrint() }) { Text(vm.bi("Test print", "測試列印")) }
            Button(onClick = { vm.openDrawer() }) { Text(vm.bi("Test drawer", "測試錢箱")) }
        }
        Spacer(Modifier.height(20.dp))
        Text(vm.bi("Cloud sync (multi-shop)", "雲端同步（多店）"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            vm.bi(
                "One API key per shop. Sales upload every 5 min when online. Sync pulls portal prices.",
                "每店一個 API Key。有網時每 5 分鐘上載銷售。同步拉取門市價。",
            ),
            color = TextMuted,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text("API base URL", color = TextMuted)
        Text(
            "Emulator: http://10.0.2.2:4000  (use : not / before port)",
            color = TextMuted,
            fontSize = 11.sp,
        )
        BasicTextField(apiBase, { apiBase = it }, Modifier.fillMaxWidth().background(Color.White).padding(10.dp))
        Spacer(Modifier.height(8.dp))
        Text("Shop API key", color = TextMuted)
        BasicTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth().background(Color.White).padding(10.dp))
        Spacer(Modifier.height(8.dp))
        Text("Shop code", color = TextMuted)
        BasicTextField(shopCode, { shopCode = it }, Modifier.fillMaxWidth().background(Color.White).padding(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = periodic, onCheckedChange = { periodic = it })
            Text(vm.bi("Periodic sales upload", "定期上載銷售"))
        }
        Button(
            onClick = {
                vm.saveSync(
                    com.store88.pos.sync.SyncConfig(
                        baseUrl = apiBase,
                        apiKey = apiKey,
                        shopCode = shopCode,
                        periodicUploadEnabled = periodic,
                    ),
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(vm.bi("Save sync", "儲存同步")) }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { vm.syncNow() },
            enabled = !ui.syncing,
            colors = ButtonDefaults.buttonColors(containerColor = TealDark),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (ui.syncing) "Syncing…" else vm.bi("Sync now", "立即同步"))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            vm.bi("Last sync", "上次同步") + ": " + vm.lastSyncLabel(),
            color = TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (ui.state.lastSyncAt != null) {
            Text(vm.lastSyncAbsolute(), color = TextMuted, fontSize = 11.sp)
        }
    }
}
