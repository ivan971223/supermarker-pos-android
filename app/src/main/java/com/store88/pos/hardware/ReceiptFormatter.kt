package com.store88.pos.hardware


import com.store88.pos.data.Pricing
import com.store88.pos.domain.AppState
import com.store88.pos.domain.PosConstants
import com.store88.pos.domain.ReceiptConfig
import com.store88.pos.domain.Sale
import com.store88.pos.domain.Session
import java.nio.charset.Charset

object ReceiptFormatter {
    private val GB18030: Charset = Charset.forName("GB18030")

    fun format(sale: Sale, config: ReceiptConfig, paperWidthMm: Int = 80): ByteArray {
        val cols = if (paperWidthMm <= 58) 32 else 42
        val out = ArrayList<Byte>()
        fun raw(vararg b: Int) = b.forEach { out += it.toByte() }
        fun text(s: String) {
            out += s.toByteArray(GB18030).toList()
            raw(0x0A)
        }
        fun center(s: String) {
            val pad = ((cols - s.length).coerceAtLeast(0)) / 2
            text(" ".repeat(pad) + s)
        }
        fun line() = text("-".repeat(cols))

        // Init + code page
        raw(0x1B, 0x40)
        raw(0x1C, 0x26) // FS & — enable Kanji if supported; harmless on many printers

        if (config.showLogo || config.showHeaderMsg) {
            center(config.shopName.take(cols))
        }
        if (config.showHeaderMsg && config.headerMsg.isNotBlank()) {
            center(config.headerMsg.take(cols))
        }
        if (config.showAddress && config.address.isNotBlank()) {
            wrap(config.address, cols).forEach { text(it) }
        }
        if (config.showTel && config.tel.isNotBlank()) text("Tel: ${config.tel}")
        if (config.showBr && config.brNo.isNotBlank()) text("BR: ${config.brNo}")
        line()
        if (config.showReceiptNo) text("No: ${sale.id}")
        if (config.showDateTime) text(sale.at.take(19).replace('T', ' '))
        if (config.showCashier) text("Cashier: ${sale.cashier}")
        line()
        for (lineItem in sale.lines) {
            text(lineItem.name.take(cols))
            val right = "%.2f".format(lineItem.lineTotal)
            val left = "  ${lineItem.qty} x %.2f".format(lineItem.unitPrice)
            text(padRow(left, right, cols))
            if (!lineItem.promoLabel.isNullOrBlank()) {
                text("  * ${lineItem.promoLabel}".take(cols))
            }
        }
        line()
        text(padRow("Subtotal", "%.2f".format(sale.subtotal), cols))
        if (sale.discount > 0) text(padRow("Discount", "-%.2f".format(sale.discount), cols))
        text(padRow("TOTAL", "HK$%.2f".format(sale.total), cols))
        text(padRow("Pay", sale.payment.uppercase(), cols))
        sale.cashTendered?.let { text(padRow("Tendered", "%.2f".format(it), cols)) }
        sale.change?.let { text(padRow("Change", "%.2f".format(it), cols)) }
        line()
        if (config.showFooterMsg && config.footerMsg.isNotBlank()) {
            wrap(config.footerMsg, cols).forEach { center(it) }
        }
        if (config.showBarcode) {
            // CODE128 barcode of sale id (truncated for thermal)
            val data = sale.id.take(20)
            raw(0x1D, 0x68, 60) // height
            raw(0x1D, 0x77, 2) // width
            raw(0x1D, 0x48, 2) // HRI below
            raw(0x1D, 0x6B, 73) // CODE128
            out += data.length.toByte()
            out += data.toByteArray(Charsets.US_ASCII).toList()
            raw(0x0A)
        }
        // Feed + partial cut
        raw(0x0A, 0x0A, 0x0A)
        raw(0x1D, 0x56, 0x00)
        return out.toByteArray()
    }

    /** Z / day-close report for thermal printer. */
    fun formatDayClose(
        state: AppState,
        session: Session,
        paperWidthMm: Int = 80,
    ): ByteArray {
        val cols = if (paperWidthMm <= 58) 32 else 42
        val out = ArrayList<Byte>()
        fun raw(vararg b: Int) = b.forEach { out += it.toByte() }
        fun text(s: String) {
            out += s.toByteArray(GB18030).toList()
            raw(0x0A)
        }
        fun center(s: String) {
            val pad = ((cols - s.length).coerceAtLeast(0)) / 2
            text(" ".repeat(pad) + s)
        }
        fun line() = text("-".repeat(cols))

        val sales = Pricing.sessionSales(state.copy(session = session))
        val active = sales.filter { it.isActive }
        val voided = sales.filter { it.status == "voided" }
        val refunded = sales.filter { it.status == "refunded" }
        val netSales = active.sumOf { it.total }
        val cashSales = Pricing.sessionCashSales(state.copy(session = session))
        val expected = session.openingFloat + cashSales
        val counted = session.countedCash ?: expected
        val diff = counted - expected
        val breakdown = Pricing.paymentBreakdown(state.copy(session = session))
        val cfg = state.receiptConfig

        raw(0x1B, 0x40)
        raw(0x1C, 0x26)
        center(cfg.shopName.take(cols))
        center("Z REPORT / 日結報表")
        line()
        text("Cashier: ${session.cashier}".take(cols))
        text("Open:  ${session.openedAt.take(19).replace('T', ' ')}")
        text("Close: ${(session.closedAt ?: Pricing.nowISO()).take(19).replace('T', ' ')}")
        line()
        text(padRow("Orders", "${active.size}", cols))
        text(padRow("Voids", "${voided.size}", cols))
        text(padRow("Refunds", "${refunded.size}", cols))
        text(padRow("Net sales", "HK$%.2f".format(netSales), cols))
        line()
        text("Payments / 付款")
        for (p in PosConstants.PAYMENTS) {
            val amt = breakdown[p.method] ?: 0.0
            text(padRow(p.en.take(18), "%.2f".format(amt), cols))
        }
        line()
        text(padRow("Opening float", "%.2f".format(session.openingFloat), cols))
        text(padRow("Cash sales", "%.2f".format(cashSales), cols))
        text(padRow("Expected", "%.2f".format(expected), cols))
        text(padRow("Counted", "%.2f".format(counted), cols))
        text(padRow("Diff", "%.2f".format(diff), cols))
        line()
        center("*** END OF DAY ***")
        raw(0x0A, 0x0A, 0x0A)
        raw(0x1D, 0x56, 0x00)
        return out.toByteArray()
    }

    private fun padRow(left: String, right: String, cols: Int): String {
        val space = (cols - left.length - right.length).coerceAtLeast(1)
        return left + " ".repeat(space) + right
    }

    private fun wrap(s: String, cols: Int): List<String> {
        if (s.length <= cols) return listOf(s)
        val out = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            out += s.substring(i, minOf(i + cols, s.length))
            i += cols
        }
        return out
    }
}
