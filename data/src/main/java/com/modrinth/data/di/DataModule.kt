package com.modrinth.data.di

import com.modrinth.data.api.ModrinthApiImpl
import com.modrinth.data.repository.ProjectRepositoryImpl
import com.modrinth.domain.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.provisioning.ProvidesBinding
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
abstract class DataModule {

    @Module
    companion object {
        @ProvidesBinding
        fun provideModrinthApi(): ModrinthApiImpl {
            return Retrofit.Builder()
                .baseUrl("https://api.modrinth.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ModrinthApiImpl::class.java)
        }
    }

    @Binds
    abstract fun bindProjectRepository(repository: ProjectRepositoryImpl): ProjectRepository
}
