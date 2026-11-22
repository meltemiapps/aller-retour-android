package io.github.pocketbook.data

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

/** Loads the bundled country packs from assets/packs. No network, ever. */
class PackRepository(context: Context) {
    val packs: Map<String, CountryPack> = context.assets.list("packs").orEmpty()
        .filter { it.endsWith(".json") }
        .map { name ->
            context.assets.open("packs/$name").bufferedReader().use { AppJson.decodeFromString(CountryPack.serializer(), it.readText()) }
        }
        .sortedBy { it.name }
        .associateBy { it.code }

    operator fun get(code: String): CountryPack = packs[code] ?: packs.values.first()
}

/**
 * Stores everything in a single JSON file in the app's private storage.
 * The file is excluded from cloud backup (see data_extraction_rules.xml) and is
 * deleted by Android when the app is uninstalled.
 */
class StateStore(context: Context) {
    private val file = File(context.filesDir, "pocketbook-state.json")

    fun load(): AppState = try {
        if (file.exists()) AppJson.decodeFromString(AppState.serializer(), file.readText()) else AppState()
    } catch (e: Exception) {
        // Keep a copy of an unreadable file rather than silently losing it.
        runCatching { file.copyTo(File(file.parentFile, "pocketbook-state.corrupt.json"), overwrite = true) }
        AppState()
    }

    fun save(state: AppState) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(AppJson.encodeToString(AppState.serializer(), state))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    fun export(state: AppState): String =
        Json(from = AppJson) { prettyPrint = true }.encodeToString(AppState.serializer(), state)

    fun import(text: String): AppState = AppJson.decodeFromString(AppState.serializer(), text)
}
