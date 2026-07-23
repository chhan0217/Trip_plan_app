package com.example.tripplan

import android.app.Application
import com.example.tripplan.data.local.KoreaRegionAssetSeeder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class TripPlanApplication : Application() {
    @Inject
    lateinit var koreaRegionAssetSeeder: KoreaRegionAssetSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            koreaRegionAssetSeeder.seedIfNeeded()
        }
    }
}
