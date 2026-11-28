package com.modrinth.android.di

import com.modrinth.domain.repository.ProjectRepository
import com.modrinth.domain.usecase.GetCategoriesUseCase
import com.modrinth.domain.usecase.GetFileDownloadUrlUseCase
import com.modrinth.domain.usecase.GetProjectUseCase
import com.modrinth.domain.usecase.GetProjectVersionsUseCase
import com.modrinth.domain.usecase.SearchProjectsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppUseCaseModule {

    @Provides
    @Singleton
    fun provideGetProjectUseCase(repository: ProjectRepository): GetProjectUseCase =
        GetProjectUseCase(repository)

    @Provides
    @Singleton
    fun provideSearchProjectsUseCase(repository: ProjectRepository): SearchProjectsUseCase =
        SearchProjectsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetProjectVersionsUseCase(repository: ProjectRepository): GetProjectVersionsUseCase =
        GetProjectVersionsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetCategoriesUseCase(repository: ProjectRepository): GetCategoriesUseCase =
        GetCategoriesUseCase(repository)

    @Provides
    @Singleton
    fun provideGetFileDownloadUrlUseCase(repository: ProjectRepository): GetFileDownloadUrlUseCase =
        GetFileDownloadUrlUseCase(repository)
}
