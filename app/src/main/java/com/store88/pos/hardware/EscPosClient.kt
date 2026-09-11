package com.store88.pos.hardware


import android.content.Context
import com.store88.pos.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

data class PrinterConfig(
    val host: String = "192.168.1.50",
    val port: Int = 9100,
    val paperWidthMm: Int = 80,
    val dryRun: Boolean = BuildConfig.PRINTER_DRY_RUN_DEFAULT,
    val printOnPayDefault: Boolean = false,
)

class PrinterSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("printer_settings", Context.MODE_PRIVATE)

    fun load(): PrinterConfig = PrinterConfig(
        host = prefs.getString("host", "192.168.1.50") ?: "192.168.1.50",
        port = prefs.getInt("port", 9100),
        paperWidthMm = prefs.getInt("paperWidthMm", 80),
        dryRun = prefs.getBoolean("dryRun", BuildConfig.PRINTER_DRY_RUN_DEFAULT),
        printOnPayDefault = prefs.getBoolean("printOnPayDefault", false),
    )

    fun save(config: PrinterConfig) {
        prefs.edit()
            .putString("host", config.host)
            .putInt("port", config.port)
            .putInt("paperWidthMm", config.paperWidthMm)
            .putBoolean("dryRun", config.dryRun)
            .putBoolean("printOnPayDefault", config.printOnPayDefault)
            .apply()
    }
}

sealed class PrinterResult {
    data object Ok : PrinterResult()
    data class Error(val message: String) : PrinterResult()
}

class EscPosClient {
    suspend fun send(config: PrinterConfig, bytes: ByteArray): PrinterResult = withContext(Dispatchers.IO) {
        if (config.dryRun) {
            return@withContext PrinterResult.Ok
        }
        if (config.host.isBlank()) {
            return@withContext PrinterResult.Error("Printer IP is empty")
        }
        runCatching {
            Socket().use { socket ->
                socket.soTimeout = 5_000
                socket.connect(InetSocketAddress(config.host, config.port), 5_000)
                val out: OutputStream = socket.getOutputStream()
                out.write(bytes)
                out.flush()
            }
            PrinterResult.Ok
        }.getOrElse {
            PrinterResult.Error(it.message ?: "Printer connection failed")
        }
    }

    suspend fun openDrawer(config: PrinterConfig): PrinterResult {
        // ESC p m t1 t2 — kick drawer pin 2
        val kick = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())
        return send(config, kick)
    }

    suspend fun ping(config: PrinterConfig): PrinterResult = withContext(Dispatchers.IO) {
        if (config.dryRun) return@withContext PrinterResult.Ok
        runCatching {
            Socket().use { socket ->
                socket.soTimeout = 3_000
                socket.connect(InetSocketAddress(config.host, config.port), 3_000)
            }
            PrinterResult.Ok
        }.getOrElse {
            PrinterResult.Error(it.message ?: "Unreachable")
        }
    }
}
