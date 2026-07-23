package com.example.tripplan.di

import com.example.tripplan.data.repository.DefaultTripPlanRepository
import com.example.tripplan.data.repository.DefaultRegionRepository
import com.example.tripplan.domain.repository.RegionRepository
import com.example.tripplan.domain.repository.TripPlanRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTripPlanRepository(
        repository: DefaultTripPlanRepository,
    ): TripPlanRepository

    @Binds
    abstract fun bindRegionRepository(
        repository: DefaultRegionRepository,
    ): RegionRepository
}
