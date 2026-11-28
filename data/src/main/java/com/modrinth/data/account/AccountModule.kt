package com.modrinth.data.account

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.modrinth.domain.account.AccountRepository
import com.modrinth.domain.account.GetFavoritesUseCase
import com.modrinth.domain.account.LoginUseCase
import com.modrinth.domain.account.LogoutUseCase
import com.modrinth.domain.account.RegisterUseCase
import com.modrinth.domain.account.ToggleFavoriteUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    abstract fun bindAccountRepository(repository: AccountRepositoryImpl): AccountRepository

    companion object {

        /** 本地账户 DataStore 文件名 */
        private const val ACCOUNTS_DATASTORE = "accounts"

        /**
         * 提供本地账户使用的 DataStore Preferences 实例。
         * 文件位于 context.filesDir/datastore/accounts.preferences_pb，纯本机存储。
         */
        @Provides
        @Singleton
        fun provideAccountDataStore(
            @ApplicationContext context: Context
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(ACCOUNTS_DATASTORE)
        }

        @Provides
        @Singleton
        fun provideRegisterUseCase(repository: AccountRepository): RegisterUseCase =
            RegisterUseCase(repository)

        @Provides
        @Singleton
        fun provideLoginUseCase(repository: AccountRepository): LoginUseCase =
            LoginUseCase(repository)

        @Provides
        @Singleton
        fun provideLogoutUseCase(repository: AccountRepository): LogoutUseCase =
            LogoutUseCase(repository)

        @Provides
        @Singleton
        fun provideToggleFavoriteUseCase(repository: AccountRepository): ToggleFavoriteUseCase =
            ToggleFavoriteUseCase(repository)

        @Provides
        @Singleton
        fun provideGetFavoritesUseCase(repository: AccountRepository): GetFavoritesUseCase =
            GetFavoritesUseCase(repository)
    }
}
