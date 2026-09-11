package com.store88.pos.sync

import android.content.Context
import com.store88.pos.domain.AppState
import com.store88.pos.domain.Cashier
import com.store88.pos.domain.Category
import com.store88.pos.domain.PosConstants
import com.store88.pos.domain.Product
import com.store88.pos.domain.Promo
import com.store88.pos.domain.ReceiptConfig
import com.store88.pos.domain.Sale
import com.store88.pos.domain.SaleLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

data class SyncConfig(
    val baseUrl: String = "http://10.0.2.2:4000",
    val apiKey: String = "till-key-shop-a-dev",
    val shopCode: String = "shop-a",
    val periodicUploadEnabled: Boolean = true,
)

class SyncSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("sync_settings", Context.MODE_PRIVATE)

    fun load(): SyncConfig = SyncConfig(
        baseUrl = prefs.getString("baseUrl", "http://10.0.2.2:4000") ?: "http://10.0.2.2:4000",
        apiKey = prefs.getString("apiKey", "till-key-shop-a-dev") ?: "till-key-shop-a-dev",
        shopCode = prefs.getString("shopCode", "shop-a") ?: "shop-a",
        periodicUploadEnabled = prefs.getBoolean("periodicUploadEnabled", true),
    )

    fun save(config: SyncConfig) {
        prefs.edit()
            .putString("baseUrl", config.baseUrl.trim().trimEnd('/'))
            .putString("apiKey", config.apiKey.trim())
            .putString("shopCode", config.shopCode.trim())
            .putBoolean("periodicUploadEnabled", config.periodicUploadEnabled)
            .apply()
    }
}

sealed class SyncResult {
    data class Ok(val message: String) : SyncResult()
    data class Err(val message: String) : SyncResult()
}

object SyncClient {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun uploadSales(config: SyncConfig, sales: List<Sale>): SyncResult = withContext(Dispatchers.IO) {
        val pending = sales.filter { !it.synced }
        if (pending.isEmpty()) return@withContext SyncResult.Ok("No sales to upload")
        runCatching {
            val body = json.encodeToString(UploadBody(pending.map { it.toWire() }))
            val conn = open(config, "/sync/sales", "POST")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val text = conn.inputStream.bufferedReader().readText()
            if (conn.responseCode !in 200..299) {
                val err = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull()
                return@withContext SyncResult.Err(err ?: "HTTP ${conn.responseCode}")
            }
            val parsed = json.decodeFromString<UploadResponse>(text)
            SyncResult.Ok("Uploaded ${parsed.upserted} sales")
        }.getOrElse { SyncResult.Err(it.message ?: "Upload failed") }
    }

    suspend fun pullCatalog(config: SyncConfig): Pair<SyncResult, CatalogPayload?> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = open(config, "/sync/catalog", "GET")
            val text = conn.inputStream.bufferedReader().readText()
            if (conn.responseCode !in 200..299) {
                val err = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull()
                return@withContext SyncResult.Err(err ?: "HTTP ${conn.responseCode}") to null
            }
            val payload = json.decodeFromString<CatalogPayload>(text)
            SyncResult.Ok("Catalog pulled (${payload.products.size} products)") to payload
        }.getOrElse { SyncResult.Err(it.message ?: "Pull failed") to null }
    }

    fun mergeCatalog(state: AppState, payload: CatalogPayload): AppState {
        val categories = payload.categories.map {
            Category(it.id, it.en, it.zh, it.icon, it.system)
        }.ifEmpty { state.categories }
        val products = payload.products.map {
            Product(
                id = it.id,
                nameEn = it.nameEn,
                nameZh = it.nameZh,
                barcode = it.barcode,
                category = it.categoryId,
                cost = it.cost,
                price = it.price,
                stock = it.stock,
                reorderMin = it.reorderMin,
                trackExpiry = it.trackExpiry,
                emoji = it.emoji,
                active = it.active,
            )
        }
        val promos = payload.promos.map {
            Promo(
                id = it.id,
                name = it.name,
                type = it.type,
                buyQty = it.buyQty,
                discountPercent = it.discountPercent,
                discountAmount = it.discountAmount,
                getFreeQty = it.getFreeQty,
                bundlePrice = it.bundlePrice,
                categoryId = it.categoryId,
                productIds = runCatching {
                    json.decodeFromString<List<String>>(it.productIdsJson)
                }.getOrNull(),
                startDate = it.startDate,
                endDate = it.endDate,
                active = it.active,
            )
        }
        val receipt = payload.receipt?.let {
            ReceiptConfig(
                shopName = it.shopName,
                address = it.address,
                tel = it.tel,
                brNo = it.brNo,
                headerMsg = it.headerMsg,
                footerMsg = it.footerMsg,
                paperWidth = it.paperWidth,
                showLogo = it.showLogo,
                showAddress = it.showAddress,
                showTel = it.showTel,
                showBr = it.showBr,
                showHeaderMsg = it.showHeaderMsg,
                showFooterMsg = it.showFooterMsg,
                showReceiptNo = it.showReceiptNo,
                showDateTime = it.showDateTime,
                showCashier = it.showCashier,
                showBarcode = it.showBarcode,
            )
        } ?: state.receiptConfig
        val favs = products.filter { it.id in state.favouriteIds || payload.products.any { p -> p.id == it.id && p.favourite } }
            .map { it.id }
            .distinct()
            .ifEmpty { products.filter { p -> payload.products.find { it.id == p.id }?.favourite == true }.map { it.id } }

        return state.copy(
            shopName = payload.shopCode?.let { state.shopName } ?: state.shopName,
            categories = categories,
            products = products.ifEmpty { state.products },
            promos = promos,
            favouriteIds = favs.ifEmpty { state.favouriteIds },
            receiptConfig = receipt,
            lastSyncAt = payload.pulledAt,
        )
    }

    private fun open(config: SyncConfig, path: String, method: String): HttpURLConnection {
        val url = URL(config.baseUrl.trimEnd('/') + path)
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Api-Key", config.apiKey)
            connectTimeout = 8_000
            readTimeout = 20_000
            doInput = true
            if (method == "POST") doOutput = true
        }
    }

    private fun Sale.toWire() = WireSale(
        id = id,
        at = at,
        cashier = cashier,
        subtotal = subtotal,
        discount = discount,
        total = total,
        payment = payment,
        cashTendered = cashTendered,
        change = change,
        status = status,
        voidedAt = voidedAt,
        voidedBy = voidedBy,
        voidReason = voidReason,
        lines = lines.map {
            WireLine(it.productId, it.name, it.barcode, it.qty, it.unitPrice, it.lineTotal, it.discount, it.promoLabel)
        },
    )
}

@Serializable
private data class UploadBody(val sales: List<WireSale>)

@Serializable
private data class UploadResponse(val upserted: Int = 0)

@Serializable
private data class WireSale(
    val id: String,
    val at: String,
    val cashier: String,
    val subtotal: Double,
    val discount: Double,
    val total: Double,
    val payment: String,
    val cashTendered: Double? = null,
    val change: Double? = null,
    val status: String = "completed",
    val voidedAt: String? = null,
    val voidedBy: String? = null,
    val voidReason: String? = null,
    val lines: List<WireLine> = emptyList(),
)

@Serializable
private data class WireLine(
    val productId: String? = null,
    val name: String,
    val barcode: String,
    val qty: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val discount: Double = 0.0,
    val promoLabel: String? = null,
)

@Serializable
data class CatalogPayload(
    val shopId: String? = null,
    val shopCode: String? = null,
    val pulledAt: String? = null,
    val categories: List<WireCategory> = emptyList(),
    val products: List<WireProduct> = emptyList(),
    val promos: List<WirePromo> = emptyList(),
    val cashiers: List<WireCashier> = emptyList(),
    val receipt: WireReceipt? = null,
)

@Serializable
data class WireCategory(
    val id: String,
    val en: String,
    val zh: String,
    val icon: String,
    val system: Boolean = false,
)

@Serializable
data class WireProduct(
    val id: String,
    val nameEn: String,
    val nameZh: String,
    val barcode: String,
    val categoryId: String,
    val cost: Double,
    val price: Double,
    val stock: Int,
    val reorderMin: Int,
    val trackExpiry: Boolean = false,
    val emoji: String,
    val active: Boolean = true,
    val favourite: Boolean = false,
)

@Serializable
data class WirePromo(
    val id: String,
    val name: String,
    val type: String,
    val buyQty: Int,
    val discountPercent: Double? = null,
    val discountAmount: Double? = null,
    val getFreeQty: Int? = null,
    val bundlePrice: Double? = null,
    val categoryId: String? = null,
    val productIdsJson: String = "[]",
    val startDate: String,
    val endDate: String,
    val active: Boolean = true,
)

@Serializable
data class WireCashier(val id: String, val name: String, val nameZh: String, val pin: String)

@Serializable
data class WireReceipt(
    val shopName: String,
    val address: String,
    val tel: String,
    val brNo: String,
    val headerMsg: String,
    val footerMsg: String,
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
