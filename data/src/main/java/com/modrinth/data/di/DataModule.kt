package com.modrinth.data.di

import com.modrinth.data.api.ModrinthApiImpl
import com.modrinth.data.repository.ProjectRepositoryImpl
import com.modrinth.domain.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindProjectRepository(repository: ProjectRepositoryImpl): ProjectRepository

    companion object {
        /**
         * 版本前缀集中在 baseUrl 里。
         * 当前按任务要求用 v1 路径（/v1/search、/v1/project/... 的 Legacy 形状）；
         * 若线上对 v1 前缀的路由返回 410（upstream main 已把 /api/v1 标记 api_deprecated），
         * 改成 "https://api.modrinth.com/v2/" 即可 —— /v2 提供完全相同的 Legacy 响应形状
         *（2026-11-28 线上核对，见 /tmp/api-verification.md）。
         */
        private const val BASE_URL = "https://api.modrinth.com/v1/"

        @Provides
        @Singleton
        fun provideHttpClient(): OkHttpClient = OkHttpClient.Builder()
            // 不跟随 302：下载接口返回 302 -> CDN，仓库层要读 Location 头而不是下载整个文件
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        @Provides
        @Singleton
        fun provideModrinthApi(client: OkHttpClient): ModrinthApiImpl {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ModrinthApiImpl::class.java)
        }
    }
}
