package com.przevolut.di

import com.przevolut.data.repository.RateRepositoryImpl
import com.przevolut.domain.repository.RateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module — wiąże interfejsy repozytoriów z ich implementacjami.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRateRepository(impl: RateRepositoryImpl): RateRepository
}
