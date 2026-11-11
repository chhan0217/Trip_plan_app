package com.example.tripplan.di

import com.example.tripplan.data.repository.DefaultTripPlanRepository
import com.example.tripplan.data.repository.DefaultItineraryRepository
import com.example.tripplan.data.file.DefaultTripPlanFileRepository
import com.example.tripplan.data.repository.DefaultRegionRepository
import com.example.tripplan.domain.repository.RegionRepository
import com.example.tripplan.domain.repository.TripPlanRepository
import com.example.tripplan.domain.repository.ItineraryRepository
import com.example.tripplan.domain.repository.TripPlanFileRepository
import com.example.tripplan.data.repository.DefaultPackingRepository
import com.example.tripplan.domain.repository.PackingRepository
import com.example.tripplan.data.repository.DefaultTripCopyRepository
import com.example.tripplan.domain.repository.TripCopyRepository
import com.example.tripplan.data.repository.DefaultPlaceRepository
import com.example.tripplan.domain.repository.PlaceRepository
import com.example.tripplan.data.repository.DefaultExchangeRateRepository
import com.example.tripplan.data.repository.DefaultExchangeRatePreferenceRepository
import com.example.tripplan.domain.repository.ExchangeRateRepository
import com.example.tripplan.domain.repository.ExchangeRatePreferenceRepository
import com.example.tripplan.data.repository.DefaultTripBudgetRepository
import com.example.tripplan.domain.repository.TripBudgetRepository
import com.example.tripplan.data.repository.DefaultExpenseRepository
import com.example.tripplan.domain.repository.ExpenseRepository
import com.example.tripplan.data.repository.DefaultTripMoneyDisplayPreferenceRepository
import com.example.tripplan.domain.repository.TripMoneyDisplayPreferenceRepository
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

    @Binds
    abstract fun bindItineraryRepository(
        repository: DefaultItineraryRepository,
    ): ItineraryRepository

    @Binds
    abstract fun bindTripPlanFileRepository(
        repository: DefaultTripPlanFileRepository,
    ): TripPlanFileRepository

    @Binds
    abstract fun bindPackingRepository(repository: DefaultPackingRepository): PackingRepository

    @Binds
    abstract fun bindTripCopyRepository(repository: DefaultTripCopyRepository): TripCopyRepository

    @Binds
    abstract fun bindPlaceRepository(repository: DefaultPlaceRepository): PlaceRepository

    @Binds
    abstract fun bindExchangeRateRepository(
        repository: DefaultExchangeRateRepository,
    ): ExchangeRateRepository

    @Binds
    abstract fun bindExchangeRatePreferenceRepository(
        repository: DefaultExchangeRatePreferenceRepository,
    ): ExchangeRatePreferenceRepository

    @Binds
    abstract fun bindTripBudgetRepository(
        repository: DefaultTripBudgetRepository,
    ): TripBudgetRepository

    @Binds
    abstract fun bindExpenseRepository(repository: DefaultExpenseRepository): ExpenseRepository

    @Binds
    abstract fun bindTripMoneyDisplayPreferenceRepository(
        repository: DefaultTripMoneyDisplayPreferenceRepository,
    ): TripMoneyDisplayPreferenceRepository
}
