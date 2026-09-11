package com.store88.pos.domain


import kotlinx.serialization.Serializable

typealias CategoryId = String

enum class UiLanguage { EN, ZH, BOTH }

enum class PaymentMethod {
    CASH, OCTOPUS, CARD, FPS, WECHAT, UNIONPAY, ALIPAY;

    val id: String
        get() = when (this) {
            CASH -> "cash"
            OCTOPUS -> "octopus"
            CARD -> "card"
            FPS -> "fps"
            WECHAT -> "wechat"
            UNIONPAY -> "unionpay"
            ALIPAY -> "alipay"
        }

    companion object {
        fun fromId(id: String): PaymentMethod =
            entries.firstOrNull { it.id == id } ?: CASH
    }
}

enum class PromoType {
    BUY_X_PERCENT, BUY_X_AMOUNT, BUY_X_GET_Y, BUY_X_FOR_PRICE;

    companion object {
        fun fromWire(s: String): PromoType = when (s) {
            "buy_x_percent" -> BUY_X_PERCENT
            "buy_x_amount" -> BUY_X_AMOUNT
            "buy_x_get_y" -> BUY_X_GET_Y
            "buy_x_for_price" -> BUY_X_FOR_PRICE
            else -> BUY_X_PERCENT
        }
    }

    fun toWire(): String = when (this) {
        BUY_X_PERCENT -> "buy_x_percent"
        BUY_X_AMOUNT -> "buy_x_amount"
        BUY_X_GET_Y -> "buy_x_get_y"
        BUY_X_FOR_PRICE -> "buy_x_for_price"
    }
}

sealed class Screen {
    data object Login : Screen()
    data object Checkout : Screen()
    data object Payment : Screen()
    data object DayClose : Screen()
    data object Products : Screen()
    data object Categories : Screen()
    data object Promotions : Screen()
    data object Inventory : Screen()
    data object Expiry : Screen()
    data object Receive : Screen()
    data object Reports : Screen()
    data object Transactions : Screen()
    data object ReceiptDesign : Screen()
    data object PrinterSettings : Screen()
}

@Serializable
data class Category(
    val id: CategoryId,
    val en: String,
    val zh: String,
    val icon: String,
    val system: Boolean = false,
)

@Serializable
data class ReceiptConfig(
    val shopName: String = "88 Store / 88超市",
    val address: String = "G/F, 88 Nathan Road, Tsim Sha Tsui, HK / 尖沙咀彌敦道88號地下",
    val tel: String = "+852 2345 6789",
    val brNo: String = "BR-68492011",
    val headerMsg: String = "Welcome to 88 Store! / 歡迎光臨88超市！",
    val footerMsg: String = "Thank you! / 多謝惠顧！",
    val paperWidth: String = "80mm",
    val showLogo: Boolean = true,
    val showAddress: Boolean = true,
    val showTel: Boolean = true,
    val showBr: Boolean = true,
    val showHeaderMsg: Boolean = true,
    val showFooterMsg: Boolean = true,
    val showReceiptNo: Boolean = true,
    val showDateTime: Boolean = true,
    val showCashier: Boolean = true,
    val showBarcode: Boolean = true,
)

@Serializable
data class Product(
    val id: String,
    val nameEn: String,
    val nameZh: String,
    val barcode: String,
    val category: CategoryId,
    val cost: Double,
    val price: Double,
    val stock: Int,
    val reorderMin: Int,
    val trackExpiry: Boolean,
    val emoji: String,
    val active: Boolean = true,
)

@Serializable
data class Lot(
    val id: String,
    val productId: String,
    val lotNumber: String,
    val qty: Int,
    val expiryDate: String,
)

@Serializable
data class Promo(
    val id: String,
    val name: String,
    val type: String,
    val buyQty: Int,
    val discountPercent: Double? = null,
    val discountAmount: Double? = null,
    val getFreeQty: Int? = null,
    val bundlePrice: Double? = null,
    val categoryId: CategoryId? = null,
    val productIds: List<String>? = null,
    val startDate: String,
    val endDate: String,
    val active: Boolean,
)

@Serializable
data class CartLine(
    val productId: String,
    val qty: Int,
)

@Serializable
data class SaleLine(
    val productId: String,
    val name: String,
    val barcode: String,
    val qty: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val discount: Double = 0.0,
    val promoLabel: String? = null,
)

@Serializable
data class Sale(
    val id: String,
    val at: String,
    val cashier: String,
    val lines: List<SaleLine>,
    val subtotal: Double,
    val discount: Double,
    val total: Double,
    val payment: String,
    val cashTendered: Double? = null,
    val change: Double? = null,
    val synced: Boolean = false,
    /** completed | voided | refunded */
    val status: String = "completed",
    val voidedAt: String? = null,
    val voidedBy: String? = null,
    val voidReason: String? = null,
) {
    val isActive: Boolean get() = status == "completed" || status.isBlank()
}

@Serializable
data class HeldCart(
    val id: String,
    val at: String,
    val lines: List<CartLine>,
    val note: String? = null,
)

@Serializable
data class Session(
    val id: String,
    val cashier: String,
    val openedAt: String,
    val closedAt: String? = null,
    val openingFloat: Double = 500.0,
    val expectedCash: Double? = null,
    val countedCash: Double? = null,
)

@Serializable
data class AppState(
    val version: Int = 7,
    val shopName: String = "88 Store",
    val shopNameZh: String = "88超市",
    val uiLanguage: String = "both",
    val categories: List<Category> = emptyList(),
    val favouriteIds: List<String> = emptyList(),
    val products: List<Product> = emptyList(),
    val lots: List<Lot> = emptyList(),
    val promos: List<Promo> = emptyList(),
    val sales: List<Sale> = emptyList(),
    val held: List<HeldCart> = emptyList(),
    val session: Session? = null,
    val cart: List<CartLine> = emptyList(),
    val cartDiscountPercent: Double = 0.0,
    val offlineQueue: List<Sale> = emptyList(),
    val lastSyncAt: String? = null,
    val receiptConfig: ReceiptConfig = ReceiptConfig(),
)

data class Cashier(
    val id: String,
    val name: String,
    val nameZh: String,
    val pin: String,
)

data class PaymentOption(
    val method: PaymentMethod,
    val en: String,
    val zh: String,
    val icon: String,
)

data class PricedCart(
    val lines: List<SaleLine>,
    val subtotal: Double,
    val discount: Double,
    val total: Double,
)

object PosConstants {
    val DEFAULT_CATEGORIES = listOf(
        Category("custom", "Custom $", "自訂價錢", "💲", system = true),
        Category("drinks", "Drinks", "飲品", "🥤"),
        Category("snacks", "Snacks", "零食", "🍪"),
        Category("instant", "Ready-to-eat", "即食", "🍜"),
        Category("fresh", "Fresh", "生鮮", "🥬"),
        Category("frozen", "Frozen", "急凍", "❄️"),
        Category("dairy", "Dairy", "乳製品", "🥛"),
        Category("rice_oil", "Rice & Oil", "米油糧", "🍚"),
        Category("household", "Daily Necessities", "日用品", "🧴"),
        Category("personal", "Personal Care", "個人護理", "🪥"),
    )

    val PAYMENTS = listOf(
        PaymentOption(PaymentMethod.CASH, "Cash", "現金", "💵"),
        PaymentOption(PaymentMethod.OCTOPUS, "Octopus", "八達通", "🐙"),
        PaymentOption(PaymentMethod.CARD, "Card", "信用卡", "💳"),
        PaymentOption(PaymentMethod.WECHAT, "WeChat Pay", "微信支付", "💬"),
        PaymentOption(PaymentMethod.UNIONPAY, "UnionPay", "銀聯", "💳"),
        PaymentOption(PaymentMethod.ALIPAY, "Alipay", "支付寶", "🟦"),
    )

    val CASHIERS = listOf(
        Cashier("amy", "Chan Tai Man", "陳大文", "1234"),
        Cashier("ken", "Ken Wong", "黃健", "5678"),
    )

    /** Manager PIN for void / refund (HK small shop). Change before production. */
    const val MANAGER_PIN = "9999"
}
