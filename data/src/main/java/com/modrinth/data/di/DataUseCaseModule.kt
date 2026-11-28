package com.modrinth.data.di

import com.modrinth.domain.repository.ProjectRepository
import com.modrinth.domain.usecase.GetCategoriesUseCase
import com.modrinth.domain.usecase.GetFileDownloadUrlUseCase
import com.modrinth.domain.usecase.GetProjectVersionsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * data 模块内提供数据层新增 usecase 的 Hilt 绑定。
 *
 * 注意：GetProjectUseCase / SearchProjectsUseCase 已由 app 层
 * app/di/AppUseCaseModule.kt 提供，这里不重复提供（否则 Hilt 报 duplicate binding）。
 */
@Module
@InstallIn(SingletonComponent::class)
object DataUseCaseModule {

    @Provides
    @Singleton
    fun provideGetProjectVersionsUseCase(
        repository: ProjectRepository
    ): GetProjectVersionsUseCase = GetProjectVersionsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetCategoriesUseCase(
        repository: ProjectRepository
    ): GetCategoriesUseCase = GetCategoriesUseCase(repository)

    @Provides
    @Singleton
    fun provideGetFileDownloadUrlUseCase(
        repository: ProjectRepository
    ): GetFileDownloadUrlUseCase = GetFileDownloadUrlUseCase(repository)
}
