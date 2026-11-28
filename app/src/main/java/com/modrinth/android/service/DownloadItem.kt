package com.modrinth.android.service

/**
 * 一个下载任务的状态。data class = LazyColumn 友好的 stable 数据：
 * 相同内容的两次赋值不会触发重组（structural equality）。
 */
data class DownloadItem(
    val id: String,
    /** 项目标题（通知和列表展示用） */
    val projectTitle: String,
    /** 目标文件名，如 1.2.0-universal.jar */
    val fileName: String,
    val url: String,
    /** 预期总字节数，未知时 -1 */
    val size: Long = -1L,
    val downloaded: Long = 0L,
    var state: State = State.QUEUED,
    /** 十六进制小写；为 null 表示不校验 */
    val sha512Expected: String? = null,
    /** 目标文件绝对路径（DONE 后有效） */
    val destPath: String = "",
    val error: String? = null
) {
    enum class State { QUEUED, RUNNING, PAUSED, DONE, FAILED }

    /** 0..1；size 未知时为 0（通知里不画进度条） */
    val progress: Float
        get() = if (size > 0) (downloaded.toFloat() / size).coerceIn(0f, 1f) else 0f

    companion object {
        fun newId(): String = java.util.UUID.randomUUID().toString()
    }
}
