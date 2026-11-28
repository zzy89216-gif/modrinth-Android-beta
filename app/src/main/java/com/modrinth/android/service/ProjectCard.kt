package com.modrinth.android.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.modrinth.domain.model.Project

/** 固定行高：LazyColumn 预布局不需要测量行高，滚动时布局成本恒定 */
val ListCardHeight: androidx.compose.ui.unit.Dp = 76.dp

/**
 * 列表项通用卡片（LazyColumn 友好）：
 *  - 固定高度 [ListCardHeight]：行高恒定，列表预布局 / 滚动稳定
 *  - 只依赖稳定入参（[project] 是 data class，[download] 是 data class）：
 *    数据没变 → 不重组；下载进度变化只重组对应那一行
 *  - 图片写死 48dp：Coil 直接按目标尺寸解码，不做「下原图再缩」
 *
 * [download] 传 null 表示该项目无下载任务，显示「下载」字样。
 */
/**
 * 用法（LazyColumn）：
 * ```
 * // ImageLoader 由 CoilModule 以 @Singleton 提供，Composable 里用注入的 VM 持有，
 * // 或直接用 LocalContext + remember（推荐前者，见各 ViewModel）
 * LazyColumn {
 *     items(projects, key = { it.id }) { p ->
 *         ProjectCard(p, downloadMap[p.id], imageLoader, onClick = { ... })
 *     }
 * }
 * ```
 */
@Composable
fun ProjectCard(
    project: Project,
    download: DownloadItem? = null,
    /** coil 2.7.0 的 AsyncImage 必须显式传 loader（本项目没有 coil-singleton 自动发现） */
    imageLoader: coil.ImageLoader,
    modifier: Modifier = Modifier,
    onClick: (Project) -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(ListCardHeight),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = { onClick(project) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标：固定 48dp，Coil 按此尺寸解码；memoryCache 命中后滚动零解码
            AsyncImage(
                model = project.iconUrl,
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 文本区：weight 占满剩余宽度，单行截断，不参与测量协商
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${project.author} · ${formatCount(project.downloads)} 次下载",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 右侧：下载状态 / 进度
            DownloadStateView(download)
        }
    }
}

/** 卡片右侧状态区（按状态渲染，状态不变不重组） */
@Composable
private fun DownloadStateView(download: DownloadItem?) {
    when (download?.state) {
        DownloadItem.State.QUEUED ->
            Text(
                "排队中",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        DownloadItem.State.RUNNING ->
            if (download != null && download.size > 0) {
                LinearProgressIndicator(
                    progress = { download.progress },
                    modifier = Modifier
                        .width(72.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }

        DownloadItem.State.PAUSED ->
            Text(
                "已暂停",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

        DownloadItem.State.DONE ->
            Text(
                "✓ 已下载",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

        DownloadItem.State.FAILED ->
            Text(
                "失败，点击重试",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

        null ->
            Text(
                "下载",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
    }
}

/** 下载量格式化：1234567 → 123.5 万 */
private fun formatCount(n: Long): String =
    when {
        n >= 100_000_000L -> String.format("%.1f 千万", n / 100_000_000.0)
        n >= 10_000L -> String.format("%.1f 万", n / 10_000.0)
        else -> n.toString()
    }
