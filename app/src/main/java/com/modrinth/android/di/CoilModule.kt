package com.modrinth.android.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Coil 全局 ImageLoader（Hilt 提供单例，@HiltViewModel / @Composable 注入使用）。
 *
 * 注意（已对 coil 2.7.0 源码核实）：
 *  - 本项目的 `io.coil-kt:coil-compose:2.7.0` 依赖不带 `coil`/`coil-singleton` 单例包，
 *    所以 `coil.compose.AsyncImage` 必须显式传 `imageLoader` 参数，
 *    不能走 ImageLoaderFactory 自动发现 —— 这里直接 @Provides ImageLoader。
 *  - Coil 2.x 已移除 BitmapPool（bitmap 池在 API 23+ 收益很小，官方主动删了），
 *    「bitmap 池」这一项不需要也不可以配置，等价优化靠下面的 memoryCache。
 *
 * 渲染策略（详见 docs/保活与渲染调研.md「Compose 渲染」）：
 *  - memoryCache 50MB：解码后的 bitmap 驻留内存，列表来回滚动不再二次解码
 *  - diskCache 128MB：原始响应缓存，切屏 / 冷启动不重拉网络
 *  - crossfade 200ms：滚动时新图淡入，观感更稳
 */
@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizeBytes(50 * 1000 * 1000) // 50MB 内存缓存
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil_disk_cache"))
                    .maxSizeBytes(128L * 1024 * 1024) // 磁盘缓存 128MB（按任务要求）
                    .build()
            }
            .crossfade(200)
            .build()
}
