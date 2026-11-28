/*
 * Part of ModBox. New code that consumes the ZalithLauncher2-ported keymap
 * modules (see NOTICE). Project files are under the repository LICENSE.
 */



package com.modrinth.android.keymap

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 键位映射仓库：把 [KeymapConfig] 持久化到 `context.filesDir/keymap.json`。
 *
 * 说明：需求原本指定用 Gson，但 app 模块当前没有声明 Gson 依赖（data 模块的
 * converter-gson 是 implementation，不外泄到 app 的 classpath）。为避免在 app 里
 * 新增未审批的三方依赖，这里改用 Android 自带的 org.json 实现同等能力（JSON 持久化）；
 * 接口（observe/save/reset）与「Gson 版本」保持一致，日后切回 Gson 只需改本文件实现。
 */
class KeymapRepository(private val context: Context) {

    private val file: java.io.File
        get() = java.io.File(context.filesDir, FILE_NAME)

    /** 内存态：null 表示尚未从磁盘加载 */
    private val _config = MutableStateFlow<KeymapConfig?>(null)

    /**
     * 响应式读取：首次收集时从磁盘加载（文件不存在/损坏回退默认键位），
     * 此后每次 [save]/[reset] 都会推送新值。
     */
    fun observe(): Flow<KeymapConfig> = flow {
        if (_config.value == null) {
            _config.value = load()
        }
        emitAll(_config.filterNotNull())
    }

    /** 读取配置；文件不存在或损坏时回退默认配置 */
    suspend fun load(): KeymapConfig = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@runCatching null
            val json = JSONObject(file.readText())
            val version = json.optInt("version", 1)
            val widthDp = json.optDouble("savedWidthDp", 0.0).toFloat()
            val arr = json.optJSONArray("bindings") ?: return@runCatching null
            val bindings = (0 until arr.length()).mapNotNull { i ->
                val b = arr.optJSONObject(i) ?: return@mapNotNull null
                KeyBinding(
                    actionId = b.optString("actionId"),
                    label = b.optString("label"),
                    keyCode = b.optInt("keyCode", -1),
                    modifiers = b.optInt("modifiers", 0),
                )
            }
            if (bindings.isEmpty()) null else KeymapConfig(bindings, widthDp, version)
        }.getOrNull() ?: DefaultKeymap.defaultConfig()
    }

    /** 保存配置到磁盘并推送给 [observe] 的订阅者 */
    suspend fun save(config: KeymapConfig) {
        writeToFile(config)
        _config.value = config
    }

    /** 重置为默认配置并持久化 */
    suspend fun reset(): KeymapConfig {
        val config = DefaultKeymap.defaultConfig()
        save(config)
        return config
    }

    private suspend fun writeToFile(config: KeymapConfig) {
        withContext(Dispatchers.IO) {
            val root = JSONObject()
            root.put("version", config.version)
            root.put("savedWidthDp", config.savedWidthDp.toDouble())
            val arr = JSONArray()
            config.bindings.forEach { b ->
                arr.put(
                    JSONObject()
                        .put("actionId", b.actionId)
                        .put("label", b.label)
                        .put("keyCode", b.keyCode)
                        .put("modifiers", b.modifiers)
                )
            }
            root.put("bindings", arr)
            file.parentFile?.mkdirs()
            // 先写临时文件再原子替换，避免中途崩溃损坏配置
            val tmp = java.io.File(file.parentFile, "$FILE_NAME.tmp")
            tmp.writeText(root.toString(2))
            tmp.renameTo(file) || tmp.copyTo(file, overwrite = true)
        }
    }

    private companion object {
        const val FILE_NAME = "keymap.json"
    }
}
