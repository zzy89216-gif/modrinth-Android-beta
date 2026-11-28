package com.modrinth.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.modrinth.android.MainActivity
import com.modrinth.android.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 下载前台服务（dataSync 类型：切后台 / 锁屏也能继续；manifest 声明见 AndroidManifest）。
 *
 * 职责划分：
 *  - [DownloadManager] = 状态机（进程内单例）：任务列表 StateFlow、提交 / 暂停 / 继续 / 取消
 *  - 本服务 = 执行器 + 通知：onCreate 起一个主循环协程收集 [DownloadManager.items]，
 *    QUEUED→RUNNING 就起协程跑 OkHttp 流式下载，状态离开 RUNNING 就取消对应协程
 *    （暂停即停流，.part 保留续传）
 *
 * 进程活着时 Service 被回收状态不丢；进程被杀则状态丢失——
 * 跨进程死亡的断点续传留二期（WorkManager + Room 落库；当前不做：
 * 下载是「会话内」场景，前台服务已覆盖切后台 / 锁屏）。
 *
 * 通知（同一个 id，两态）：
 *  - 有活动任务：ongoing + 进度条 + 「暂停 / 取消」操作，点按回 MainActivity
 *  - 全部结束：一次性通知（setAutoCancel），服务退出前台并自毁
 */
@AndroidEntryPoint
class DownloadService @Inject constructor(
    /** Hilt 注入的单例状态机：与 ViewModel 持有同一个实例 */
    private val dm: DownloadManager
) : Service() {

    companion object {
        const val CHANNEL_ID = "channel_download"
        const val ACTION_PAUSE = "com.modrinth.android.service.action.PAUSE"
        const val ACTION_CANCEL = "com.modrinth.android.service.action.CANCEL"
        private const val FOREGROUND_ID = 1
    }

    /** 服务内协程作用域（IO 池）：onDestroy 时 cancel，所有下载协程随之取消 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var client: OkHttpClient? = null

    /** id → 下载协程；状态离开 RUNNING 时 cancel（暂停 / 取消 / 完成 / 失败） */
    private val inFlight = HashMap<String, Job>()

    private var isForeground = false
    private var lastPostAt = 0L

    override fun onCreate() {
        super.onCreate()
        client = OkHttpClient.Builder().build()
        createChannel()
        // 唯一的主循环：跟随任务状态 → 起 / 停下载协程 + 刷新通知（300ms 节流，状态跃迁立即刷）
        scope.launch {
            var prev: Map<String, DownloadItem.State> = emptyMap()
            dm.items.collect { items ->
                val states = items.associate { it.id to it.state }

                // 1) RUNNING 且尚无协程 → 起协程
                for (item in items) {
                    if (item.state == DownloadItem.State.RUNNING && inFlight[item.id] == null) {
                        inFlight[item.id] = scope.launch { execute(item) }
                    }
                }
                // 2) 已有协程但状态不再是 RUNNING → 取消
                for ((id, job) in inFlight.toList()) {
                    if (states[id] != DownloadItem.State.RUNNING) {
                        job.cancel()
                        inFlight.remove(id)
                    }
                }
                // 3) 通知刷新
                val stateChanged = states != prev
                prev = states
                val now = System.currentTimeMillis()
                if (stateChanged || now - lastPostAt >= 300) {
                    lastPostAt = now
                    post()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> dm.pause()
            ACTION_CANCEL -> dm.cancel()
        }
        // startForegroundService 之后必须尽快 startForeground，这里兜住首次
        post()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        client?.dispatcher.executorService.shutdown()
        super.onDestroy()
    }

    // ---------- 通知 ----------

    private fun createChannel() {
        getSystemService<NotificationManager>()?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "下载",
                NotificationManager.IMPORTANCE_LOW // 有进度条、无提示音
            ).apply { description = "模组下载进度" }
        )
    }

    /** 刷新 id=FOREGROUND_ID 的通知；无活动任务时退出前台、发一次性通知并自毁 */
    private fun post() {
        val items = dm.items.value
        if (items.isEmpty()) {
            // 无任务（如服务被 action 意图拉起但列表已空）：不发通知，直接退出
            if (isForeground) stopForegroundCompat()
            stopSelf()
            return
        }
        val running = items.filter { it.state == DownloadItem.State.RUNNING }
        val queuedCount = items.count { it.state == DownloadItem.State.QUEUED }
        val pausedCount = items.count { it.state == DownloadItem.State.PAUSED }
        val active = running.isNotEmpty() || queuedCount > 0

        val notification: Notification = if (active) {
            val totalSize = running.sumOf { it.size }
            val totalDone = running.sumOf { it.downloaded }
            val builder = baseBuilder()
                .setContentTitle("Modrinth 下载")
                .setContentText(
                    when {
                        running.isNotEmpty() ->
                            if (running.size > 1) "正在下载 ${running.size} 个模组…"
                            else "正在下载 ${running.first().projectTitle}"
                        else -> "等待网络…（$queuedCount 个排队）"
                    }
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
            if (totalSize > 0) {
                builder.setProgress(totalSize.toInt(), totalDone.toInt(), false)
            } else {
                builder.setProgress(0, 0, true) // 大小未知：不定长进度条
            }
            builder.addAction(0, "暂停", servicePending(ACTION_PAUSE, 1))
            builder.addAction(0, "取消", servicePending(ACTION_CANCEL, 2))
            builder.build()
        } else {
            val hasFailed = items.any { it.state == DownloadItem.State.FAILED }
            val hasDone = items.any { it.state == DownloadItem.State.DONE }
            baseBuilder()
                .setContentTitle(
                    when {
                        hasFailed -> "有下载失败"
                        hasDone -> "下载完成"
                        pausedCount > 0 -> "下载已暂停"
                        else -> "下载已取消"
                    }
                )
                .setContentText(
                    items.firstOrNull { it.state != DownloadItem.State.QUEUED }
                        ?.let {
                            when (it.state) {
                                DownloadItem.State.DONE -> "${it.projectTitle}：${it.fileName}"
                                DownloadItem.State.FAILED -> "${it.projectTitle}：${it.error ?: "失败"}"
                                DownloadItem.State.PAUSED -> "${it.projectTitle}：${it.downloaded} B"
                                else -> it.projectTitle
                            }
                        } ?: "点按返回应用"
                )
                .setAutoCancel(true) // 一次性：点按后消失
                .build()
        }

        if (active) {
            if (!isForeground) startForegroundSafe(notification)
            else getSystemService<NotificationManager>()?.notify(FOREGROUND_ID, notification)
        } else {
            if (isForeground) stopForegroundCompat()
            getSystemService<NotificationManager>()?.notify(FOREGROUND_ID, notification)
            stopSelf() // 全部结束：一次性通知留在栏里，服务自毁
        }
    }

    private fun startForegroundSafe(notification: Notification) {
        // 30+ 启动前台服务必须声明 type；dataSync 需要 FOREGROUND_SERVICE_DATA_SYNC 权限
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(FOREGROUND_ID, notification)
        }
        isForeground = true
    }

    private fun stopForegroundCompat() {
        // 退出前台但保留一次性通知
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        isForeground = false
    }

    private fun baseBuilder() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

    private fun servicePending(action: String, reqCode: Int) = PendingIntent.getService(
        this, reqCode,
        Intent(this, DownloadService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // ---------- 执行 ----------

    private suspend fun execute(item: DownloadItem) {
        try {
            val dir = getExternalFilesDir(null)
                ?: throw IOException("应用外部文件目录不可用")
            val downloadsDir = File(dir, "downloads").apply { mkdirs() }
            val part = File(downloadsDir, "${item.fileName}.part")
            val dest = File(downloadsDir, item.fileName)
            val client = client ?: throw IOException("OkHttp 未初始化")

            val expected = item.sha512Expected?.lowercase()
            val digest = expected?.let { MessageDigest.getInstance("SHA-512") }

            dm.markRunning(item.id, part.absolutePath)

            // 断点：.part 已有字节（本会话内暂停过）且未下完 → 带 Range 续传
            var startOffset = if (part.exists()) part.length() else 0L
            if (item.size > 0 && startOffset >= item.size) {
                part.delete() // 已下完但没改名（防御性），重来
                startOffset = 0L
            }

            val request = Request.Builder()
                .url(item.url)
                .addHeader("User-Agent", "ModrinthAndroid/1.0")
                .apply { if (startOffset > 0) addHeader("Range", "bytes=$startOffset-") }
                .build()

            // OkHttp 异步回调 → 挂起等待；协程被取消（暂停）时不 resume，回调里关掉响应
            val response: Response = suspendCancellableCoroutine { cont ->
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if (cont.isCancelled) response.close() else cont.resume(response)
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        if (!cont.isCancelled) cont.resumeWithException(e)
                    }
                })
            }

            try {
                val body = response.body ?: throw IOException("响应体为空")
                if (response.code !in 200..299) throw IOException("HTTP ${response.code}")

                // 206 = 服务端接受 Range，真续传；200 = 从头给，.part 作废
                val resumed = response.code == 206 && startOffset > 0L
                if (!resumed && part.exists()) {
                    part.delete()
                    startOffset = 0L
                }
                // 续传时 sha512 要覆盖「旧字节 + 新字节」：先把 .part 现有内容喂进 digest
                var downloaded = startOffset
                if (resumed && digest != null) feedDigest(part, 0L, downloaded, digest)

                // 206 的 contentLength 只是剩余字节
                val remaining = body.contentLength().coerceAtLeast(0L)
                val total = if (resumed) downloaded + remaining else remaining
                dm.setSize(item.id, if (total > 0) total else -1L)

                if (resumed) {
                    RandomAccessFile(part, "rw").use { raf ->
                        raf.seek(raf.length())
                        stream(body, raf, item, downloaded, digest, total)
                    }
                } else {
                    FileOutputStream(part).use { out ->
                        stream(body, out, item, downloaded, digest, total)
                    }
                }

                // sha512 校验（全程流式算好，整体比较）
                if (digest != null && expected != null) {
                    val actual = digest.digest().joinToString("") { "%02x".format(it) }
                    if (actual.lowercase() != expected) {
                        part.delete()
                        dm.markFailed(item.id, "sha512 校验失败")
                        return
                    }
                }

                // .part → 正式文件名：原子替换，避免半截文件被当成品
                if (dest.exists()) dest.delete()
                if (!part.renameTo(dest)) {
                    part.copyTo(dest, overwrite = true) // 同分区 rename 几乎不会失败，兜底
                    part.delete()
                }
                dm.markDone(item.id, dest.absolutePath)
            } finally {
                response.close()
            }
        } catch (e: java.util.concurrent.CancellationException) {
            // 暂停 / 取消：状态已由 DownloadManager 改好（PAUSED / FAILED「已取消」），
            // .part 保留供续传；重新抛出保持协程取消语义
            throw e
        } catch (e: Exception) {
            dm.markFailed(item.id, e.message ?: "下载失败")
        }
    }

    /**
     * 流式写 [out] + 流式更新 [digest]，每 300ms 向 [DownloadManager] 报一次进度。
     * 注意 read / write 是阻塞调用，协程取消不会打断阻塞 I/O，
     * 所以每块数据前检查一次取消（暂停后最多再写一个 64KB 块就停）。
     */
    private suspend fun stream(
        body: okhttp3.ResponseBody,
        out: java.io.OutputStream,
        item: DownloadItem,
        initial: Long,
        digest: MessageDigest?,
        total: Long
    ) {
        var downloaded = initial
        val source = body.source()
        val buffer = ByteArray(64 * 1024)
        var lastReport = 0L
        while (true) {
            checkCancelled() // 阻塞 I/O 不响应取消，手动检查
            val n = source.read(buffer)
            if (n == -1) break
            out.write(buffer, 0, n)
            digest?.update(buffer, 0, n)
            downloaded += n
            val now = System.currentTimeMillis()
            if (now - lastReport >= 300) {
                lastReport = now
                dm.reportProgress(item.id, downloaded, total)
            }
        }
        dm.reportProgress(item.id, downloaded, total) // 收尾补一次
    }

    /** 本协程被取消（暂停 / 取消 / 服务销毁）就抛 CancellationException */
    private suspend fun checkCancelled() {
        val job = coroutineContext[Job]
        if (job == null || !job.isActive) {
            throw java.util.concurrent.CancellationException("下载已取消")
        }
    }

    /** 把 [file] 的 [from]..[to] 字节喂给 [digest]（续传前补齐旧部分，保证整体 sha512 正确） */
    private fun feedDigest(file: File, from: Long, to: Long, digest: MessageDigest) {
        if (to <= from) return
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(from)
            val buffer = ByteArray(64 * 1024)
            var left = to - from
            while (left > 0) {
                val n = raf.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                if (n == -1) break
                digest.update(buffer, 0, n)
                left -= n
            }
        }
    }
}
