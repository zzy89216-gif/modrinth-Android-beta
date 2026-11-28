package com.modrinth.android.di

import android.content.Context
import com.modrinth.android.service.DownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 下载管理器 Hilt 绑定。
 * 放 app 模块（不放 data）：DownloadService / 通知 / 前台服务类型都是 app 层关注点，
 * data 模块不该反向依赖 app。
 */
@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context
    ): DownloadManager = DownloadManager(context)
}
