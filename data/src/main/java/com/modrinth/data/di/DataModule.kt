package com.modrinth.data.di

import com.modrinth.data.api.ModrinthApiImpl
import com.modrinth.data.repository.ProjectRepositoryImpl
import com.modrinth.domain.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindProjectRepository(repository: ProjectRepositoryImpl): ProjectRepository

    companion object {
        @Provides
        @Singleton
        fun provideModrinthApi(): ModrinthApiImpl {
            return Retrofit.Builder()
                .baseUrl("https://api.modrinth.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ModrinthApiImpl::class.java)
        }
    }
}
