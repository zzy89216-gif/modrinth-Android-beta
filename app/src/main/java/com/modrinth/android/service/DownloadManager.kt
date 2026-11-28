package com.modrinth.android.service

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 下载任务管理器（进程内单例，Hilt 绑定见 di/DownloadModule）。
 *
 * 职责划分：
 *  - 本类 = 状态机：任务列表 StateFlow、提交 / 暂停 / 继续 / 重试 / 取消、排队调度
 *  - [DownloadService] = 执行器：观察到 QUEUED→RUNNING 的跃迁就在服务协程里跑 OkHttp 下载，
 *    并负责前台通知。Service 死亡只影响通知，状态在本进程内不丢。
 *
 * 并发：同一时刻只跑 1 个下载（手机场景足够），其余 QUEUED 排队，
 * 前一个 DONE/FAILED 后自动捞下一个。
 *
 * 生命周期说明（为什么不用 WorkManager，先不做）：
 *  下载是「会话内」行为——用户点下载后通常马上继续浏览或安装，
 *  前台服务已覆盖「切后台 / 锁屏继续」；WorkManager 的增量价值是
 *  「进程被杀后由系统重新调度」，需要把任务落库（Room 已在 data 模块就位）
 *  并重试 / 幂等语义，放到二期再做。
 */
class DownloadManager(private val context: Context) {

    /** 全部任务（含排队 / 进行中 / 已完成 / 失败），UI 直接 collect */
    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    // ---------- 对外 API（ViewModel / UI 调用） ----------

    /**
     * 提交一个下载任务，返回任务 id。
     * [size] 传 API 给的文件字节数（未知传 -1，通知里显示不定长进度条）；
     * [sha512Expected] 传 hex 小写期望值（modrinth version file 的 sha512 字段），不传则不校验。
     */
    fun enqueue(
        projectTitle: String,
        fileName: String,
        url: String,
        size: Long = -1L,
        sha512Expected: String? = null
    ): String {
        val id = DownloadItem.newId()
        _items.update {
            it + DownloadItem(
                id = id,
                projectTitle = projectTitle,
                fileName = fileName,
                url = url,
                size = size,
                state = DownloadItem.State.QUEUED,
                sha512Expected = sha512Expected?.lowercase()
            )
        }
        ensureServiceRunning()
        startNext()
        return id
    }

    /** 暂停所有运行中的任务（.part 文件保留，可续传） */
    fun pause() {
        _items.update {
            it.map { item ->
                if (item.state == DownloadItem.State.RUNNING) item.copy(state = DownloadItem.State.PAUSED)
                else item
            }
        }
    }

    /**
     * 继续：PAUSED → 排队；对「卡住的 RUNNING」（Service 被杀等）也生效，
     * 相当于重试。同时确保 Service 活着。
     */
    fun resume() {
        _items.update {
            it.map { item ->
                if (item.state == DownloadItem.State.PAUSED || item.state == DownloadItem.State.RUNNING) {
                    item.copy(state = DownloadItem.State.QUEUED)
                } else item
            }
        }
        ensureServiceRunning()
        startNext()
    }

    /** 取消所有未完成任务（排队 / 暂停 / 运行中 → FAILED「已取消」，保留在列表里可删可重试） */
    fun cancel() {
        _items.update {
            it.map { item ->
                when (item.state) {
                    DownloadItem.State.QUEUED,
                    DownloadItem.State.PAUSED,
                    DownloadItem.State.RUNNING ->
                        item.copy(state = DownloadItem.State.FAILED, error = "已取消")
                    else -> item
                }
            }
        }
    }

    /** 重试一个 FAILED 任务（保留 .part 可续传的进度） */
    fun retry(id: String) {
        _items.update {
            it.map { item ->
                if (item.id == id && item.state == DownloadItem.State.FAILED) {
                    item.copy(state = DownloadItem.State.QUEUED, error = null)
                } else item
            }
        }
        ensureServiceRunning()
        startNext()
    }

    /** 移除 DONE / FAILED 的任务（UI 的「清除已完成」） */
    fun clearFinished() {
        _items.update {
            it.filter { item ->
                item.state == DownloadItem.State.QUEUED ||
                    item.state == DownloadItem.State.RUNNING ||
                    item.state == DownloadItem.State.PAUSED
            }
        }
    }

    /** 是否还有活动任务（UI 决定要不要显示悬浮下载指示） */
    val activeCount: Int
        get() = _items.value.count {
            it.state == DownloadItem.State.RUNNING ||
                it.state == DownloadItem.State.QUEUED ||
                it.state == DownloadItem.State.PAUSED
        }

    // ---------- 内部：供 DownloadService 调用的状态写入（线程安全，flow.update 原子） ----------

    internal fun stateOf(id: String): DownloadItem.State? =
        _items.value.firstOrNull { it.id == id }?.state

    internal fun markRunning(id: String, workPath: String) = set(id) {
        it.copy(state = DownloadItem.State.RUNNING, destPath = workPath, error = null)
    }

    internal fun setSize(id: String, size: Long) = set(id) {
        it.copy(size = size)
    }

    internal fun reportProgress(id: String, downloaded: Long, size: Long) = set(id) {
        it.copy(downloaded = downloaded, size = if (size > 0) size else it.size)
    }

    internal fun markDone(id: String, finalPath: String) {
        set(id) {
            it.copy(
                state = DownloadItem.State.DONE,
                destPath = finalPath,
                downloaded = if (it.size > 0) it.size else it.downloaded
            )
        }
        startNext()
    }

    internal fun markFailed(id: String, error: String) {
        set(id) { it.copy(state = DownloadItem.State.FAILED, error = error) }
        startNext()
    }

    // crossinline：transform 会被传给非内联的 flow.update lambda，不能非局部返回
    private inline fun set(id: String, crossinline transform: (DownloadItem) -> DownloadItem) {
        _items.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    /** 没有任务在跑时，把最早的 QUEUED 提为 RUNNING（并发 = 1） */
    private fun startNext() {
        _items.update { list ->
            if (list.any { it.state == DownloadItem.State.RUNNING }) return@update list
            val next = list.firstOrNull { it.state == DownloadItem.State.QUEUED } ?: return@update list
            list.map { if (it.id == next.id) it.copy(state = DownloadItem.State.RUNNING) else it }
        }
    }

    /** 有任务就保证前台服务在跑（startForegroundService 幂等；调用时 App 必在前台，无后台启动限制问题） */
    private fun ensureServiceRunning() {
        if (_items.value.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(
                android.content.Intent(context, DownloadService::class.java)
            )
        } else {
            context.startService(android.content.Intent(context, DownloadService::class.java))
        }
    }
}
