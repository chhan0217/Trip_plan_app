package com.example.tripplan.data.local

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class KoreaRegionAssetSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: TripPlanDatabase,
    private val regionDao: RegionDao,
) {
    suspend fun seedIfNeeded() {
        val assetVersion = readAssetVersion()
        val savedVersion = context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_KOREA_VERSION, null)

        if (assetVersion == savedVersion && regionDao.count() > 0) return

        database.withTransaction {
            regionDao.clear()
            insertRegionsFromAsset()
        }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_KOREA_VERSION, assetVersion)
        }
    }

    private fun readAssetVersion(): String {
        context.assets.open(ASSET_NAME).bufferedReader().useLines { lines ->
            val firstLine = lines.firstOrNull().orEmpty()
            return KoreaRegionAssetParser.parseVersion(firstLine)
                ?: error("$ASSET_NAME must start with '# korea_version='.")
        }
    }

    private suspend fun insertRegionsFromAsset() {
        val pendingRegions = mutableListOf<RegionEntity>()

        context.assets.open(ASSET_NAME).bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val region = KoreaRegionAssetParser.parseRegion(line) ?: return@forEach
                pendingRegions += region

                if (pendingRegions.size == INSERT_CHUNK_SIZE) {
                    regionDao.insertAll(pendingRegions.toList())
                    pendingRegions.clear()
                }
            }
        }

        if (pendingRegions.isNotEmpty()) {
            regionDao.insertAll(pendingRegions)
        }
    }

    private companion object {
        const val ASSET_NAME = "korea.txt"
        const val INSERT_CHUNK_SIZE = 500
        const val PREF_NAME = "region_seed_preferences"
        const val KEY_KOREA_VERSION = "korea_version"
    }
}
