package com.store88.pos.data


import android.content.Context
import com.store88.pos.domain.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class PosRepository(context: Context) {
    private val file = File(context.filesDir, "sunny-mart-pos-v1.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    suspend fun load(): AppState = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            val seed = SeedData.createSeedState()
            save(seed)
            return@withContext seed
        }
        runCatching {
            json.decodeFromString<AppState>(file.readText())
        }.getOrElse {
            SeedData.createSeedState().also { save(it) }
        }
    }

    suspend fun save(state: AppState) = withContext(Dispatchers.IO) {
        file.writeText(json.encodeToString(state))
    }

    suspend fun reset(): AppState = withContext(Dispatchers.IO) {
        val seed = SeedData.createSeedState()
        save(seed)
        seed
    }
}
