package com.store88.pos


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.store88.pos.data.PosRepository
import com.store88.pos.data.Pricing
import com.store88.pos.data.SeedData
import com.store88.pos.domain.AppState
import com.store88.pos.domain.CartLine
import com.store88.pos.domain.Category
import com.store88.pos.domain.Lot
import com.store88.pos.domain.PaymentMethod
import com.store88.pos.domain.PosConstants
import com.store88.pos.domain.PendingChangeRequest
import com.store88.pos.domain.Product
import com.store88.pos.domain.Promo
import com.store88.pos.domain.ReceiptConfig
import com.store88.pos.domain.Screen
import com.store88.pos.domain.Session
import com.store88.pos.domain.UiLanguage
import com.store88.pos.hardware.EscPosClient
import com.store88.pos.hardware.PrinterConfig
import com.store88.pos.hardware.PrinterResult
import com.store88.pos.hardware.PrinterSettingsStore
import com.store88.pos.hardware.ReceiptFormatter
import com.store88.pos.sync.SyncClient
import com.store88.pos.sync.SyncConfig
import com.store88.pos.sync.SyncResult
import com.store88.pos.sync.SyncSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.NetworkInterface
import kotlin.math.ceil

data class UiState(
    val ready: Boolean = false,
    val state: AppState = SeedData.createSeedState(),
    val screen: Screen = Screen.Login,
    val selectedCashierId: String = PosConstants.CASHIERS.first().id,
    val pin: String = "",
    val loginError: String? = null,
    val categoryFilter: String = "favourites",
    val productPage: Int = 0,
    val searchQuery: String = "",
    val selectedPayment: PaymentMethod = PaymentMethod.CASH,
    val cashTendered: String = "",
    val printOnPay: Boolean = false,
    val flash: String? = null,
    val online: Boolean = true,
    val printer: PrinterConfig = PrinterConfig(),
    val countedCash: String = "",
    /** Day close: cash left overnight (淨低) for tomorrow opening. */
    val cashLeftOvernight: String = "",
    val adminSearch: String = "",
    val showMore: Boolean = false,
    val showScan: Boolean = false,
    val scanQuery: String = "",
    val missingBarcode: String? = null,
    val showDiscount: Boolean = false,
    val discountDraft: String = "5",
    val qtyEditId: String? = null,
    val qtyDraft: String = "",
    val nowMs: Long = System.currentTimeMillis(),
    val prefillBarcode: String = "",
    val customerThankYouUntil: Long = 0L,
    val customerThankYouTotal: Double = 0.0,
    val showCustomerPreview: Boolean = false,
    val sync: com.store88.pos.sync.SyncConfig = com.store88.pos.sync.SyncConfig(),
    val syncing: Boolean = false,
)

class PosViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PosRepository(app)
    private val printerStore = PrinterSettingsStore(app)
    private val syncStore = SyncSettingsStore(app)
    private val escPos = EscPosClient()

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = repo.load()
            val printer = printerStore.load()
            val sync = syncStore.load()
            _ui.update {
                it.copy(
                    ready = true,
                    state = loaded,
                    printer = printer,
                    sync = sync,
                    printOnPay = printer.printOnPayDefault,
                    online = guessOnline(),
                )
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _ui.update { it.copy(nowMs = System.currentTimeMillis(), online = guessOnline()) }
            }
        }
        // Periodic sales upload every 5 minutes when online (never blocks pay)
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L)
                val cur = _ui.value
                if (cur.ready && cur.sync.periodicUploadEnabled && cur.online) {
                    uploadSalesSilent()
                }
            }
        }
    }

    private fun guessOnline(): Boolean =
        runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList()?.any { ni ->
                ni.isUp && !ni.isLoopback && ni.inetAddresses.toList().isNotEmpty()
            } == true
        }.getOrDefault(true)

    private fun persist(state: AppState) {
        viewModelScope.launch { repo.save(state) }
    }

    private fun mutate(block: (AppState) -> AppState) {
        _ui.update { cur ->
            val next = block(cur.state)
            persist(next)
            cur.copy(state = next)
        }
    }

    fun setScreen(screen: Screen) {
        if (screen == Screen.PrinterSettings && !isTillAdmin()) {
            flash("Admin only — Printer/Sync / 僅管理員可開打印機／同步設定")
            return
        }
        _ui.update { it.copy(screen = screen, flash = null, loginError = null) }
    }

    /** Till cashier role ADMIN (from portal Cashiers). */
    fun isTillAdmin(): Boolean {
        val st = _ui.value.state
        val sid = st.session?.cashierId?.takeIf { it.isNotBlank() }
        val cashiers = st.cashiers.ifEmpty { PosConstants.CASHIERS }
        val me = when {
            sid != null -> cashiers.find { it.id == sid }
            else -> cashiers.find { it.name == st.session?.cashier }
        }
        return me?.role.equals("ADMIN", ignoreCase = true)
    }

    fun setLanguage(lang: UiLanguage) {
        mutate { it.copy(uiLanguage = when (lang) {
            UiLanguage.EN -> "en"
            UiLanguage.ZH -> "zh"
            UiLanguage.BOTH -> "both"
        }) }
    }

    fun selectCashier(id: String) {
        _ui.update { it.copy(selectedCashierId = id, pin = "", loginError = null) }
    }

    fun pinKey(digit: String) {
        _ui.update { cur ->
            if (digit == "C") return@update cur.copy(pin = "", loginError = null)
            if (digit == "⌫" || digit == "back") return@update cur.copy(pin = cur.pin.dropLast(1), loginError = null)
            if (cur.pin.length >= 4) return@update cur
            val next = cur.pin + digit
            cur.copy(pin = next, loginError = null)
        }
        val pin = _ui.value.pin
        if (pin.length == 4) tryLogin(pin)
    }

    fun tryLogin(pinValue: String? = null) {
        val cur = _ui.value
        val pin = pinValue ?: cur.pin
        val cashiers = cur.state.cashiers.ifEmpty { PosConstants.CASHIERS }
        val cashier = cashiers.find { it.id == cur.selectedCashierId }
        if (cashier == null || cashier.pin != pin) {
            _ui.update { it.copy(pin = "", loginError = "Wrong PIN / 密碼錯誤") }
            flash("Wrong PIN / 密碼錯誤")
            return
        }
        val open = cur.state.session?.takeIf { it.closedAt == null }
        val session = if (open != null) {
            open.copy(cashier = cashier.name, cashierId = cashier.id)
        } else {
            Session(
                id = "sess-${System.currentTimeMillis()}",
                cashier = cashier.name,
                cashierId = cashier.id,
                openedAt = Pricing.nowISO(),
                openingFloat = cur.state.cashFloatCarry,
            )
        }
        mutate { it.copy(session = session, cart = emptyList(), cartDiscountPercent = 0.0) }
        _ui.update { it.copy(screen = Screen.Checkout, pin = "", loginError = null, showMore = false) }
        flash("Welcome ${cashier.name}")
    }

    /** Switch cashier without day close — keeps till session open. */
    fun logoutCashier() {
        mutate { it.copy(cart = emptyList(), cartDiscountPercent = 0.0) }
        _ui.update {
            it.copy(
                screen = Screen.Login,
                pin = "",
                loginError = null,
                showMore = false,
            )
        }
        flash("Logged out / 已登出 — switch account")
    }

    fun resetDemo() {
        viewModelScope.launch {
            val seed = repo.reset()
            _ui.update {
                it.copy(
                    state = seed,
                    screen = Screen.Login,
                    pin = "",
                    categoryFilter = "favourites",
                    productPage = 0,
                )
            }
            flash("Demo reset / 已重設示範資料")
        }
    }

    fun setCategory(id: String) {
        _ui.update { it.copy(categoryFilter = id, productPage = 0) }
    }

    fun setProductPage(page: Int) {
        _ui.update { it.copy(productPage = page.coerceAtLeast(0)) }
    }

    fun setSearch(q: String) {
        // HID barcode scanners type digits then Enter/newline — auto-submit
        if (q.contains('\n') || q.contains('\r')) {
            val cleaned = q.replace("\r", "").replace("\n", "")
            _ui.update { it.copy(searchQuery = cleaned) }
            submitSearch()
            return
        }
        _ui.update { it.copy(searchQuery = q) }
    }

    fun submitSearch() {
        val q = _ui.value.searchQuery.trim()
        if (q.isEmpty()) return
        val p = Pricing.findByBarcode(_ui.value.state.products, q)
        if (p != null) {
            addProduct(p.id)
            _ui.update { it.copy(searchQuery = "") }
            flash("Added ${p.nameEn}")
        } else {
            _ui.update { it.copy(searchQuery = "", missingBarcode = q) }
        }
    }

    fun toggleMore() {
        _ui.update { it.copy(showMore = !it.showMore) }
    }

    fun setShowScan(show: Boolean) {
        _ui.update { it.copy(showScan = show, scanQuery = if (show) it.scanQuery else "") }
    }

    fun setScanQuery(q: String) {
        if (q.contains('\n') || q.contains('\r')) {
            val cleaned = q.replace("\r", "").replace("\n", "")
            _ui.update { it.copy(scanQuery = cleaned) }
            submitScan()
            return
        }
        _ui.update { it.copy(scanQuery = q) }
    }

    fun submitScan() {
        val code = _ui.value.scanQuery.trim()
        if (code.isEmpty()) return
        val p = Pricing.findByBarcode(_ui.value.state.products, code)
        if (p == null) {
            _ui.update { it.copy(scanQuery = "", showScan = false, missingBarcode = code) }
            return
        }
        addProduct(p.id)
        _ui.update { it.copy(scanQuery = "", showScan = false) }
        flash("Added ${p.nameEn}")
    }

    fun dismissMissing() {
        _ui.update { it.copy(missingBarcode = null) }
    }

    fun confirmAddMissingProduct() {
        val code = _ui.value.missingBarcode ?: return
        _ui.update { it.copy(missingBarcode = null, prefillBarcode = code, adminSearch = code, screen = Screen.Products) }
    }

    fun consumePrefill() {
        _ui.update { it.copy(prefillBarcode = "") }
    }

    fun setShowDiscount(show: Boolean) {
        _ui.update { it.copy(showDiscount = show, discountDraft = if (show) "5" else it.discountDraft) }
    }

    fun setDiscountDraft(v: String) {
        _ui.update { it.copy(discountDraft = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    }

    fun confirmDiscount() {
        val pct = _ui.value.discountDraft.toDoubleOrNull() ?: return
        if (pct < 0 || pct > 100) return
        applyCustomerDiscount(pct)
        _ui.update { it.copy(showDiscount = false) }
    }

    fun startQtyEdit(productId: String, qty: Int) {
        _ui.update { it.copy(qtyEditId = productId, qtyDraft = qty.toString()) }
    }

    fun setQtyDraft(v: String) {
        _ui.update { it.copy(qtyDraft = v.filter { it.isDigit() }) }
    }

    fun applyQtyEdit() {
        val id = _ui.value.qtyEditId ?: return
        val n = _ui.value.qtyDraft.toIntOrNull()
        if (n != null) setQty(id, n)
        _ui.update { it.copy(qtyEditId = null, qtyDraft = "") }
    }

    fun printLastReceipt() {
        val last = _ui.value.state.sales.firstOrNull()
        if (last == null) {
            flash("No transaction to print / 沒有可列印的單據")
            return
        }
        printSale(last)
    }

    fun sessionClock(): String {
        val opened = _ui.value.state.session?.openedAt ?: return "00:00:00"
        val start = runCatching { java.time.OffsetDateTime.parse(opened).toInstant().toEpochMilli() }
            .getOrElse { _ui.value.nowMs }
        val s = ((_ui.value.nowMs - start) / 1000).coerceAtLeast(0)
        val hh = (s / 3600).toString().padStart(2, '0')
        val mm = ((s % 3600) / 60).toString().padStart(2, '0')
        val ss = (s % 60).toString().padStart(2, '0')
        return "$hh:$mm:$ss"
    }

    /** Relative last catalog Sync time, e.g. "5 mins ago" / "5分鐘前". */
    fun lastSyncLabel(): String {
        val raw = _ui.value.state.lastSyncAt ?: return bi("Never synced", "未同步")
        return formatSyncRelative(raw)
    }

    fun lastSyncAbsolute(): String {
        val raw = _ui.value.state.lastSyncAt ?: return "—"
        return formatSyncTime(raw)
    }

    fun formatSyncTime(raw: String): String {
        return runCatching {
            val odt = java.time.OffsetDateTime.parse(raw)
            val local = odt.atZoneSameInstant(java.time.ZoneId.systemDefault())
            local.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        }.getOrElse {
            raw.take(16).replace('T', ' ')
        }
    }

    fun formatSyncRelative(raw: String): String {
        val thenMs = runCatching {
            java.time.OffsetDateTime.parse(raw).toInstant().toEpochMilli()
        }.getOrElse {
            runCatching { java.time.Instant.parse(raw).toEpochMilli() }.getOrElse { return formatSyncTime(raw) }
        }
        val secs = ((_ui.value.nowMs - thenMs) / 1000).coerceAtLeast(0)
        return when {
            secs < 45 -> bi("just now", "剛剛")
            secs < 90 -> bi("1 min ago", "1分鐘前")
            secs < 3600 -> {
                val m = (secs / 60).toInt()
                bi("$m mins ago", "${m}分鐘前")
            }
            secs < 3600 * 36 -> {
                val h = (secs / 3600).toInt()
                if (h == 1) bi("1 hour ago", "1小時前") else bi("$h hours ago", "${h}小時前")
            }
            secs < 86400L * 14 -> {
                val d = (secs / 86400).toInt()
                if (d == 1) bi("1 day ago", "1天前") else bi("$d days ago", "${d}天前")
            }
            else -> formatSyncTime(raw)
        }
    }

    fun addProduct(productId: String) {
        val product = _ui.value.state.products.find { it.id == productId } ?: return
        if (!product.active) return
        mutate { st ->
            st.copy(cart = Pricing.addToCart(st.cart, productId, 1))
        }
    }

    fun setQty(productId: String, qty: Int) {
        mutate { it.copy(cart = Pricing.setCartQty(it.cart, productId, qty)) }
    }

    fun clearCart() {
        mutate { it.copy(cart = emptyList(), cartDiscountPercent = 0.0) }
    }

    fun toggleFavourite(productId: String) {
        val wasFav = productId in _ui.value.state.favouriteIds
        mutate { st ->
            val fav = st.favouriteIds.toMutableList()
            if (productId in fav) fav.remove(productId) else fav.add(productId)
            st.copy(favouriteIds = fav)
        }
        flash(
            if (wasFav) "Removed from Favourites / 已移出常用"
            else "Added to Favourites / 已加入常用",
        )
    }

    fun applyCustomerDiscount(percent: Double) {
        mutate { it.copy(cartDiscountPercent = percent.coerceIn(0.0, 100.0)) }
    }

    fun goPay(method: PaymentMethod) {
        val priced = Pricing.priceCart(
            _ui.value.state.cart,
            _ui.value.state.products,
            _ui.value.state.promos,
            _ui.value.state.cartDiscountPercent,
        )
        if (priced.lines.isEmpty()) {
            flash("Cart is empty / 購物車是空的")
            return
        }
        _ui.update {
            it.copy(
                selectedPayment = method,
                cashTendered = ceil(priced.total).toInt().toString(),
                screen = Screen.Payment,
            )
        }
    }

    fun setCashTendered(v: String) {
        _ui.update { it.copy(cashTendered = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    }

    fun cashKey(key: String) {
        val cur = _ui.value.cashTendered
        val priced = Pricing.priceCart(
            _ui.value.state.cart,
            _ui.value.state.products,
            _ui.value.state.promos,
            _ui.value.state.cartDiscountPercent,
        )
        val next = when (key) {
            "clear" -> ""
            "back" -> cur.dropLast(1)
            "exact" -> priced.total.toString()
            "." -> if (cur.contains('.')) cur else (if (cur.isEmpty()) "0." else "$cur.")
            "00" -> if (cur.isEmpty()) cur else cur + "00"
            else -> if (key.startsWith("+")) {
                val add = key.removePrefix("+").toDoubleOrNull() ?: 0.0
                ((cur.toDoubleOrNull() ?: 0.0) + add).toString()
            } else if (cur == "0") key else cur + key
        }
        _ui.update { it.copy(cashTendered = next) }
    }

    fun setPrintOnPay(on: Boolean) {
        _ui.update { it.copy(printOnPay = on) }
    }

    fun confirmPay() {
        val cur = _ui.value
        val priced = Pricing.priceCart(
            cur.state.cart,
            cur.state.products,
            cur.state.promos,
            cur.state.cartDiscountPercent,
        )
        val tendered = cur.cashTendered.toDoubleOrNull()
        if (cur.selectedPayment == PaymentMethod.CASH) {
            if (tendered == null || tendered < priced.total) {
                flash("Cash tendered too low")
                return
            }
        }
        val (next, sale) = Pricing.completeSale(
            cur.state,
            cur.selectedPayment,
            if (cur.selectedPayment == PaymentMethod.CASH) tendered else null,
            online = cur.online,
        )
        persist(next)
        _ui.update {
            it.copy(
                state = next,
                screen = Screen.Checkout,
                cashTendered = "",
                customerThankYouUntil = System.currentTimeMillis() + 5_000L,
                customerThankYouTotal = sale.total,
            )
        }

        viewModelScope.launch {
            if (cur.selectedPayment == PaymentMethod.CASH) {
                when (val r = escPos.openDrawer(cur.printer)) {
                    is PrinterResult.Error -> flash("Drawer: ${r.message}")
                    PrinterResult.Ok -> if (cur.printer.dryRun) flash("Drawer opened (dry-run)")
                }
            }
            if (cur.printOnPay) {
                printSale(sale)
            } else {
                flash("Sale OK / 已完成 ${Pricing.money(sale.total)}")
            }
        }
    }

    fun printSale(sale: com.store88.pos.domain.Sale) {
        viewModelScope.launch {
            val cfg = _ui.value.printer
            val bytes = ReceiptFormatter.format(
                sale,
                _ui.value.state.receiptConfig,
                cfg.paperWidthMm,
            )
            when (val r = escPos.send(cfg, bytes)) {
                PrinterResult.Ok -> flash(
                    if (cfg.dryRun) "Printed (dry-run) / 模擬列印" else "Receipt printed / 已列印",
                )
                is PrinterResult.Error -> flash("Print failed: ${r.message}")
            }
        }
    }

    fun openDrawer() {
        viewModelScope.launch {
            when (val r = escPos.openDrawer(_ui.value.printer)) {
                PrinterResult.Ok -> flash(if (_ui.value.printer.dryRun) "Drawer (dry-run)" else "Drawer opened")
                is PrinterResult.Error -> flash(r.message)
            }
        }
    }

    fun testPrint() {
        val sample = _ui.value.state.sales.firstOrNull()
            ?: com.store88.pos.domain.Sale(
                id = "test-print",
                at = Pricing.nowISO(),
                cashier = "Test",
                lines = emptyList(),
                subtotal = 0.0,
                discount = 0.0,
                total = 0.0,
                payment = "cash",
            )
        printSale(sample)
    }

    fun pingPrinter() {
        viewModelScope.launch {
            when (val r = escPos.ping(_ui.value.printer)) {
                PrinterResult.Ok -> flash("Printer reachable / 打印機可連線")
                is PrinterResult.Error -> flash("Unreachable: ${r.message}")
            }
        }
    }

    fun savePrinter(config: PrinterConfig) {
        if (!isTillAdmin()) {
            flash("Admin only — Printer/Sync / 僅管理員")
            return
        }
        printerStore.save(config)
        _ui.update { it.copy(printer = config) }
        flash("Printer settings saved")
    }

    fun saveSync(config: SyncConfig) {
        if (!isTillAdmin()) {
            flash("Admin only — Printer/Sync / 僅管理員")
            return
        }
        syncStore.save(config)
        _ui.update { it.copy(sync = config) }
        flash("Sync settings saved / 已儲存同步設定")
    }

    fun uploadSalesSilent() {
        viewModelScope.launch {
            val cur = _ui.value
            when (val r = SyncClient.uploadSales(cur.sync, cur.state.sales)) {
                is SyncResult.Ok -> {
                    if (r.message.startsWith("Uploaded")) {
                        mutate { st ->
                            st.copy(sales = st.sales.map { if (!it.synced) it.copy(synced = true) else it })
                        }
                    }
                }
                is SyncResult.Err -> Unit
            }
        }
    }

    fun syncNow() {
        if (_ui.value.syncing) return
        viewModelScope.launch {
            _ui.update { it.copy(syncing = true) }
            val cur = _ui.value
            val parts = mutableListOf<String>()
            when (val upload = SyncClient.uploadSales(cur.sync, cur.state.sales)) {
                is SyncResult.Ok -> {
                    if (upload.message.startsWith("Uploaded")) {
                        mutate { st -> st.copy(sales = st.sales.map { it.copy(synced = true) }) }
                    }
                    parts += upload.message
                }
                is SyncResult.Err -> parts += "Upload failed: ${upload.message}"
            }
            val pending = _ui.value.state.pendingChangeRequests
            when (val cr = SyncClient.pushChangeRequests(_ui.value.sync, pending)) {
                is SyncResult.Ok -> {
                    if (pending.isNotEmpty()) {
                        mutate { it.copy(pendingChangeRequests = emptyList()) }
                    }
                    parts += cr.message
                }
                is SyncResult.Err -> parts += "Requests failed: ${cr.message}"
            }
            // Always pull catalog (roles / shop / prices) even if upload failed
            val (pullResult, payload) = SyncClient.pullCatalog(_ui.value.sync)
            when (pullResult) {
                is SyncResult.Ok -> {
                    if (payload != null) {
                        mutate { SyncClient.mergeCatalog(it, payload) }
                        // Keep sync.shopCode aligned with portal shop
                        payload.shopCode?.takeIf { it.isNotBlank() }?.let { code ->
                            val next = _ui.value.sync.copy(shopCode = code)
                            syncStore.save(next)
                            _ui.update { it.copy(sync = next) }
                        }
                    }
                    parts += pullResult.message
                    flash(parts.joinToString("; "))
                }
                is SyncResult.Err -> flash(parts.joinToString("; ") + "; Pull failed: ${pullResult.message}")
            }
            _ui.update { it.copy(syncing = false) }
        }
    }

    fun setCountedCash(v: String) {
        _ui.update { it.copy(countedCash = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    }

    fun setCashLeftOvernight(v: String) {
        _ui.update { it.copy(cashLeftOvernight = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    }

    fun closeDay() {
        val cur = _ui.value
        val session = cur.state.session ?: return
        val expected = session.openingFloat + Pricing.sessionCashSales(cur.state)
        val counted = cur.countedCash.toDoubleOrNull() ?: expected
        val left = cur.cashLeftOvernight.toDoubleOrNull()
            ?: session.openingFloat.coerceAtMost(counted)
        val bank = (counted - left).coerceAtLeast(0.0)
        val closed = session.copy(
            closedAt = Pricing.nowISO(),
            expectedCash = expected,
            countedCash = counted,
            cashLeftOvernight = left,
            bankDeposit = bank,
        )
        mutate {
            it.copy(
                session = closed,
                cashFloatCarry = left,
            )
        }
        printDayCloseReport(closed)
        _ui.update {
            it.copy(
                screen = Screen.Login,
                countedCash = "",
                cashLeftOvernight = "",
                pin = "",
            )
        }
        flash("Day closed — left ${Pricing.money(left)} / 已日結，淨低 ${Pricing.money(left)}")
    }

    fun fillCountedWithExpected() {
        val session = _ui.value.state.session ?: return
        val expected = session.openingFloat + Pricing.sessionCashSales(_ui.value.state)
        _ui.update { it.copy(countedCash = "%.2f".format(expected)) }
    }

    /** Prefill 淨低 with yesterday's opening float (or current float carry). */
    fun fillCashLeftWithOpening() {
        val session = _ui.value.state.session ?: return
        val counted = _ui.value.countedCash.toDoubleOrNull()
        val suggest = session.openingFloat
        val left = if (counted != null) suggest.coerceAtMost(counted) else suggest
        _ui.update { it.copy(cashLeftOvernight = "%.2f".format(left)) }
    }

    fun printDayCloseReport(sessionOverride: com.store88.pos.domain.Session? = null) {
        viewModelScope.launch {
            val cur = _ui.value
            val base = sessionOverride ?: cur.state.session ?: return@launch
            val session = if (sessionOverride != null) {
                base
            } else {
                val expected = base.openingFloat + Pricing.sessionCashSales(cur.state)
                val counted = cur.countedCash.toDoubleOrNull() ?: expected
                val left = cur.cashLeftOvernight.toDoubleOrNull()
                    ?: base.openingFloat.coerceAtMost(counted)
                base.copy(
                    expectedCash = expected,
                    countedCash = counted,
                    cashLeftOvernight = left,
                    bankDeposit = (counted - left).coerceAtLeast(0.0),
                )
            }
            val bytes = ReceiptFormatter.formatDayClose(
                state = cur.state,
                session = session,
                paperWidthMm = cur.printer.paperWidthMm,
            )
            when (val r = escPos.send(cur.printer, bytes)) {
                PrinterResult.Ok -> flash(
                    if (cur.printer.dryRun) "Day report (dry-run) / 日結報表模擬" else "Day report printed / 已列印日結",
                )
                is PrinterResult.Error -> flash("Day report failed: ${r.message}")
            }
        }
    }

    fun voidOrRefundSale(saleId: String, asRefund: Boolean, managerPin: String, reason: String = "") {
        if (managerPin != PosConstants.MANAGER_PIN) {
            flash("Wrong manager PIN / 主管密碼錯誤")
            return
        }
        val existing = _ui.value.state.sales.find { it.id == saleId }
        if (existing == null) {
            flash("Sale not found")
            return
        }
        if (!existing.isActive) {
            flash("Already ${existing.status} / 已處理")
            return
        }
        val by = _ui.value.state.session?.cashier ?: "Manager"
        mutate { st ->
            Pricing.voidOrRefundSale(st, saleId, asRefund, by, reason.ifBlank { null }).first
        }
        flash(
            if (asRefund) "Refunded $saleId / 已退款"
            else "Voided $saleId / 已作廢",
        )
        if (existing.payment == "cash") {
            viewModelScope.launch { escPos.openDrawer(_ui.value.printer) }
        }
    }

    private fun enqueuePortalChange(type: String, summary: String, payloadJson: String) {
        mutate { st ->
            val req = PendingChangeRequest(
                localId = "cr-${System.currentTimeMillis()}-${type}",
                type = type,
                summary = summary,
                payloadJson = payloadJson,
            )
            // Keep latest request per type+summary key for same product id in payload
            val filtered = st.pendingChangeRequests.filterNot {
                it.type == type && it.summary == summary
            }
            st.copy(pendingChangeRequests = filtered + req)
        }
    }

    fun upsertProduct(product: Product) {
        mutate { st ->
            val list = st.products.toMutableList()
            val i = list.indexOfFirst { it.id == product.id }
            if (i >= 0) list[i] = product else list.add(0, product)
            val fav = product.id in st.favouriteIds
            val payload = SyncClient.productPayloadJson(product, fav)
            val req = PendingChangeRequest(
                localId = "cr-product-${product.id}",
                type = "product_upsert",
                summary = "Product ${product.nameEn}",
                payloadJson = payload,
            )
            val pending = st.pendingChangeRequests.filterNot { it.localId == req.localId } + req
            st.copy(products = list, pendingChangeRequests = pending)
        }
        flash("Saved locally — Sync to request portal update")
    }

    fun deleteProduct(id: String) {
        if (id.startsWith("custom-")) {
            flash("Cannot delete custom price keys")
            return
        }
        mutate { st ->
            val req = PendingChangeRequest(
                localId = "cr-product-del-$id",
                type = "product_delete",
                summary = "Delete product $id",
                payloadJson = """{"id":"$id"}""",
            )
            st.copy(
                products = st.products.filterNot { p -> p.id == id },
                pendingChangeRequests = st.pendingChangeRequests.filterNot { it.localId == req.localId } + req,
            )
        }
        flash("Deleted locally — Sync to request portal update")
    }

    fun upsertCategory(cat: Category) {
        mutate { st ->
            val list = st.categories.toMutableList()
            val i = list.indexOfFirst { it.id == cat.id }
            if (i >= 0) list[i] = cat else list.add(cat)
            st.copy(categories = list)
        }
    }

    fun deleteCategory(id: String) {
        val cat = _ui.value.state.categories.find { it.id == id }
        if (cat?.system == true) {
            flash("System category")
            return
        }
        mutate { it.copy(categories = it.categories.filterNot { c -> c.id == id }) }
    }

    fun upsertPromo(promo: Promo) {
        mutate { st ->
            val list = st.promos.toMutableList()
            val i = list.indexOfFirst { it.id == promo.id }
            if (i >= 0) list[i] = promo else list.add(0, promo)
            st.copy(promos = list)
        }
    }

    fun deletePromo(id: String) {
        mutate { it.copy(promos = it.promos.filterNot { p -> p.id == id }) }
    }

    fun receiveStock(productId: String, qty: Int, lotNumber: String?, expiry: String?) {
        if (qty <= 0) return
        mutate { st ->
            val products = st.products.map {
                if (it.id == productId) it.copy(stock = it.stock + qty) else it
            }
            val lots = if (!lotNumber.isNullOrBlank() && !expiry.isNullOrBlank()) {
                st.lots + Lot(
                    id = "lot-${System.currentTimeMillis()}",
                    productId = productId,
                    lotNumber = lotNumber,
                    qty = qty,
                    expiryDate = expiry,
                )
            } else st.lots
            val lotPayload = """{"productId":"$productId","qty":$qty,"lotNumber":"${lotNumber ?: ""}","expiryDate":"${expiry ?: "2099-12-31"}"}"""
            val req = PendingChangeRequest(
                localId = "cr-lot-${System.currentTimeMillis()}",
                type = "lot_receive",
                summary = "Receive +$qty for $productId",
                payloadJson = lotPayload,
            )
            st.copy(products = products, lots = lots, pendingChangeRequests = st.pendingChangeRequests + req)
        }
        flash("Received +$qty locally — Sync to request portal update")
    }

    fun markdownLot(lotId: String) {
        mutate { st ->
            val lot = st.lots.find { it.id == lotId } ?: return@mutate st
            val products = st.products.map {
                if (it.id == lot.productId) it.copy(price = Pricing.round2(it.price * 0.7)) else it
            }
            st.copy(products = products)
        }
        flash("30% markdown applied")
    }

    fun updateReceiptConfig(cfg: ReceiptConfig) {
        mutate { st ->
            val req = PendingChangeRequest(
                localId = "cr-receipt",
                type = "receipt_update",
                summary = "Receipt settings",
                payloadJson = SyncClient.receiptPayloadJson(cfg),
            )
            st.copy(
                receiptConfig = cfg,
                pendingChangeRequests = st.pendingChangeRequests.filterNot { it.localId == "cr-receipt" } + req,
            )
        }
        flash("Receipt saved locally — Sync to request portal update")
    }

    fun setAdminSearch(q: String) {
        _ui.update { it.copy(adminSearch = q) }
    }

    fun holdCart() {
        val cart = _ui.value.state.cart
        if (cart.isEmpty()) {
            flash("Nothing to hold")
            return
        }
        mutate { st ->
            st.copy(
                held = st.held + com.store88.pos.domain.HeldCart(
                    id = "hold-${System.currentTimeMillis()}",
                    at = Pricing.nowISO(),
                    lines = cart,
                ),
                cart = emptyList(),
            )
        }
        flash("Cart held / 已暫停")
    }

    private fun flash(msg: String) {
        _ui.update { it.copy(flash = msg) }
    }

    fun clearFlash() {
        _ui.update { it.copy(flash = null) }
    }

    fun setShowCustomerPreview(show: Boolean) {
        _ui.update { it.copy(showCustomerPreview = show) }
    }

    fun bi(en: String, zh: String): String {
        return when (Pricing.parseUiLanguage(_ui.value.state.uiLanguage)) {
            UiLanguage.EN -> en
            UiLanguage.ZH -> zh
            UiLanguage.BOTH -> "$en / $zh"
        }
    }
}
