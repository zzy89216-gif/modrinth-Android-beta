package com.modrinth.data.di

import com.modrinth.data.api.ModrinthApi
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
        fun provideModrinthApi(): ModrinthApi {
            return Retrofit.Builder()
                .baseUrl("https://api.modrinth.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ModrinthApi::class.java)
        }
    }

    @Binds
    abstract fun bindProjectRepository(repository: ProjectRepositoryImpl): ProjectRepository
}
