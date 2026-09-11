package com.store88.pos.data


import com.store88.pos.domain.AppState
import com.store88.pos.domain.CartLine
import com.store88.pos.domain.Category
import com.store88.pos.domain.CategoryId
import com.store88.pos.domain.PaymentMethod
import com.store88.pos.domain.PricedCart
import com.store88.pos.domain.Product
import com.store88.pos.domain.Promo
import com.store88.pos.domain.PromoType
import com.store88.pos.domain.Sale
import com.store88.pos.domain.SaleLine
import com.store88.pos.domain.UiLanguage
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

object Pricing {
    fun money(n: Double): String = "HK$%.2f".format(n)

    fun todayISO(): String = LocalDate.now().toString()

    fun nowISO(): String =
        OffsetDateTime.now(ZoneOffset.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    fun daysUntil(date: String): Long {
        val a = LocalDate.now()
        val b = LocalDate.parse(date)
        return ChronoUnit.DAYS.between(a, b)
    }

    private fun promoApplies(promo: Promo, product: Product, now: String = todayISO()): Boolean {
        if (!promo.active) return false
        if (promo.startDate > now || promo.endDate < now) return false
        if (promo.categoryId != null && promo.categoryId != product.category) return false
        if (!promo.productIds.isNullOrEmpty() && product.id !in promo.productIds) return false
        return true
    }

    private fun getEligibleLines(
        base: List<SaleLine>,
        promo: Promo,
        productMap: Map<String, Product>,
    ): List<SaleLine> =
        base.filter { line ->
            val p = productMap[line.productId] ?: return@filter false
            when {
                !promo.productIds.isNullOrEmpty() -> line.productId in promo.productIds
                promo.categoryId != null -> p.category == promo.categoryId
                else -> true
            }
        }

    fun priceCart(
        cart: List<CartLine>,
        products: List<Product>,
        promos: List<Promo>,
        cartDiscountPercent: Double = 0.0,
    ): PricedCart {
        val productMap = products.associateBy { it.id }
        val activePromos = promos.filter { it.active }
        val base = mutableListOf<SaleLine>()

        for (line in cart) {
            val p = productMap[line.productId] ?: continue
            if (line.qty <= 0) continue
            base += SaleLine(
                productId = p.id,
                name = "${p.nameEn} / ${p.nameZh}",
                barcode = p.barcode,
                qty = line.qty,
                unitPrice = p.price,
                lineTotal = round2(p.price * line.qty),
                discount = 0.0,
            )
        }

        var promoDiscount = 0.0

        for (promo in activePromos) {
            val type = PromoType.fromWire(promo.type)
            val eligible = getEligibleLines(base, promo, productMap).toMutableList()
            // mutate through base by productId index map
            fun updateLine(productId: String, transform: (SaleLine) -> SaleLine) {
                val idx = base.indexOfFirst { it.productId == productId }
                if (idx >= 0) base[idx] = transform(base[idx])
            }

            val totalQty = eligible.sumOf { it.qty }

            when (type) {
                PromoType.BUY_X_PERCENT -> {
                    val pct = promo.discountPercent
                    if (pct == null || promo.buyQty <= 0) continue
                    val sets = totalQty / promo.buyQty
                    if (sets <= 0) continue
                    val sorted = eligible.sortedBy { it.unitPrice }
                    var remaining = sets * promo.buyQty
                    for (line in sorted) {
                        if (remaining <= 0) break
                        val take = minOf(line.qty, remaining)
                        val d = round2(take * line.unitPrice * pct / 100.0)
                        updateLine(line.productId) { cur ->
                            val disc = cur.discount + d
                            cur.copy(
                                discount = disc,
                                lineTotal = round2(cur.unitPrice * cur.qty - disc),
                                promoLabel = promo.name.ifBlank { "Buy ${promo.buyQty} -${pct.toInt()}%" },
                            )
                        }
                        remaining -= take
                        promoDiscount += d
                    }
                }

                PromoType.BUY_X_AMOUNT -> {
                    val amount = promo.discountAmount
                    if (amount == null || promo.buyQty <= 0) continue
                    val sets = totalQty / promo.buyQty
                    if (sets <= 0) continue
                    val d = round2(sets * amount)
                    val first = eligible.firstOrNull() ?: continue
                    updateLine(first.productId) { cur ->
                        val disc = cur.discount + d
                        cur.copy(
                            discount = disc,
                            lineTotal = round2(cur.unitPrice * cur.qty - disc),
                            promoLabel = promo.name.ifBlank { "Buy ${promo.buyQty} -\$$amount" },
                        )
                    }
                    promoDiscount += d
                }

                PromoType.BUY_X_GET_Y -> {
                    val freeQty = promo.getFreeQty
                    if (freeQty == null || promo.buyQty <= 0) continue
                    val groupSize = promo.buyQty + freeQty
                    val sets = totalQty / groupSize
                    if (sets <= 0) continue
                    val sorted = eligible.sortedBy { it.unitPrice }
                    var freeUnits = sets * freeQty
                    for (line in sorted) {
                        if (freeUnits <= 0) break
                        val take = minOf(line.qty, freeUnits)
                        val d = round2(take * line.unitPrice)
                        updateLine(line.productId) { cur ->
                            val disc = cur.discount + d
                            cur.copy(
                                discount = disc,
                                lineTotal = round2(cur.unitPrice * cur.qty - disc),
                                promoLabel = promo.name.ifBlank { "Buy ${promo.buyQty} Get $freeQty Free" },
                            )
                        }
                        freeUnits -= take
                        promoDiscount += d
                    }
                }

                PromoType.BUY_X_FOR_PRICE -> {
                    val bundlePrice = promo.bundlePrice
                    if (bundlePrice == null || promo.buyQty <= 0) continue
                    val sets = totalQty / promo.buyQty
                    if (sets <= 0) continue
                    val sorted = eligible.sortedByDescending { it.unitPrice }
                    var remaining = sets * promo.buyQty
                    val allocations = mutableListOf<Pair<SaleLine, Int>>()
                    var regularValue = 0.0
                    for (line in sorted) {
                        if (remaining <= 0) break
                        val take = minOf(line.qty, remaining)
                        allocations += line to take
                        regularValue += take * line.unitPrice
                        remaining -= take
                    }
                    val bundleTotal = round2(sets * bundlePrice)
                    val totalDisc = max(0.0, round2(regularValue - bundleTotal))
                    if (totalDisc > 0 && regularValue > 0) {
                        var allocated = 0.0
                        allocations.forEachIndexed { idx, (line, take) ->
                            val share =
                                if (idx == allocations.lastIndex) round2(totalDisc - allocated)
                                else round2(take * line.unitPrice / regularValue * totalDisc)
                            updateLine(line.productId) { cur ->
                                val disc = cur.discount + share
                                cur.copy(
                                    discount = disc,
                                    lineTotal = round2(cur.unitPrice * cur.qty - disc),
                                    promoLabel = promo.name.ifBlank { "${promo.buyQty} for \$$bundlePrice" },
                                )
                            }
                            allocated += share
                        }
                        promoDiscount += totalDisc
                    }
                }
            }
        }

        // Re-read after mutations — base already updated
        val subtotal = round2(base.sumOf { it.unitPrice * it.qty })
        val cartDisc = round2((subtotal - promoDiscount) * (cartDiscountPercent / 100.0))
        val discount = round2(promoDiscount + cartDisc)
        val total = max(0.0, round2(subtotal - discount))
        return PricedCart(base.toList(), subtotal, discount, total)
    }

    fun findByBarcode(products: List<Product>, barcode: String): Product? {
        val code = barcode.trim()
        return products.find { it.active && it.barcode == code }
    }

    fun addToCart(cart: List<CartLine>, productId: String, qty: Int = 1): List<CartLine> {
        val next = cart.toMutableList()
        val i = next.indexOfFirst { it.productId == productId }
        if (i >= 0) next[i] = next[i].copy(qty = next[i].qty + qty)
        else next += CartLine(productId, qty)
        return next
    }

    fun setCartQty(cart: List<CartLine>, productId: String, qty: Int): List<CartLine> {
        if (qty <= 0) return cart.filter { it.productId != productId }
        return cart.map { if (it.productId == productId) it.copy(qty = qty) else it }
    }

    fun completeSale(
        state: AppState,
        payment: PaymentMethod,
        cashTendered: Double? = null,
        online: Boolean = true,
    ): Pair<AppState, Sale> {
        val priced = priceCart(state.cart, state.products, state.promos, state.cartDiscountPercent)
        val sale = Sale(
            id = "sale-${System.currentTimeMillis()}",
            at = nowISO(),
            cashier = state.session?.cashier ?: "Unknown",
            lines = priced.lines,
            subtotal = priced.subtotal,
            discount = priced.discount,
            total = priced.total,
            payment = payment.id,
            cashTendered = cashTendered,
            change = cashTendered?.let { round2(it - priced.total) },
            synced = false,
        )

        val products = state.products.map { it.copy() }.toMutableList()
        var lots = state.lots.map { it.copy() }

        for (line in priced.lines) {
            val pi = products.indexOfFirst { it.id == line.productId }
            if (pi < 0) continue
            products[pi] = products[pi].copy(stock = max(0, products[pi].stock - line.qty))

            if (products[pi].trackExpiry) {
                var left = line.qty
                val productLots = lots
                    .filter { it.productId == line.productId && it.qty > 0 }
                    .sortedBy { it.expiryDate }
                for (lot in productLots) {
                    if (left <= 0) break
                    val take = minOf(lot.qty, left)
                    lots = lots.map { if (it.id == lot.id) it.copy(qty = it.qty - take) else it }
                    left -= take
                }
            }
        }

        var next = state.copy(
            products = products,
            lots = lots,
            sales = listOf(sale) + state.sales,
            cart = emptyList(),
            cartDiscountPercent = 0.0,
            offlineQueue = if (online) state.offlineQueue else listOf(sale) + state.offlineQueue,
        )
        if (online) {
            next = next.copy(
                sales = next.sales.map { if (it.id == sale.id) it.copy(synced = true) else it },
            )
        }
        return next to sale
    }

    fun sessionCashSales(state: AppState): Double {
        val session = state.session ?: return 0.0
        return state.sales
            .filter { it.isActive && it.at >= session.openedAt && it.payment == "cash" }
            .sumOf { it.total }
    }

    fun sessionSales(state: AppState): List<Sale> {
        val session = state.session ?: return emptyList()
        return state.sales.filter { it.at >= session.openedAt }
    }

    fun paymentBreakdown(state: AppState): Map<PaymentMethod, Double> {
        val out = PaymentMethod.entries.associateWith { 0.0 }.toMutableMap()
        val session = state.session ?: return out
        for (s in state.sales) {
            if (!s.isActive) continue
            if (s.at < session.openedAt) continue
            val m = PaymentMethod.fromId(s.payment)
            out[m] = (out[m] ?: 0.0) + s.total
        }
        return out
    }

    /** Void or refund a completed sale: restore stock and mark status. */
    fun voidOrRefundSale(
        state: AppState,
        saleId: String,
        asRefund: Boolean,
        by: String,
        reason: String? = null,
    ): Pair<AppState, Sale?> {
        val sale = state.sales.find { it.id == saleId } ?: return state to null
        if (!sale.isActive) return state to sale
        val products = state.products.map { it.copy() }.toMutableList()
        for (line in sale.lines) {
            val pi = products.indexOfFirst { it.id == line.productId }
            if (pi < 0) continue
            products[pi] = products[pi].copy(stock = products[pi].stock + line.qty)
        }
        val updated = sale.copy(
            status = if (asRefund) "refunded" else "voided",
            voidedAt = nowISO(),
            voidedBy = by,
            voidReason = reason,
            synced = false,
        )
        val next = state.copy(
            products = products,
            sales = state.sales.map { if (it.id == saleId) updated else it },
        )
        return next to updated
    }

    fun categoryLabel(id: CategoryId, categories: List<Category>, lang: UiLanguage = UiLanguage.BOTH): String {
        val c = categories.find { it.id == id } ?: return id
        return when (lang) {
            UiLanguage.EN -> c.en
            UiLanguage.ZH -> c.zh
            UiLanguage.BOTH -> "${c.en} / ${c.zh}"
        }
    }

    fun parseUiLanguage(s: String): UiLanguage = when (s) {
        "en" -> UiLanguage.EN
        "zh" -> UiLanguage.ZH
        else -> UiLanguage.BOTH
    }

    fun round2(n: Double): Double = "%.2f".format(n).toDouble()
}
