package com.example.tripplan.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.tripplan.data.local.ItineraryPhotoEntity
import com.example.tripplan.data.local.TripPlanDatabase
import com.example.tripplan.data.mapper.toDomain
import com.example.tripplan.domain.repository.ItineraryPhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultItineraryPhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: TripPlanDatabase,
    private val clock: Clock,
) : ItineraryPhotoRepository {
    private val dao = database.itineraryPhotoDao()
    private val authority = "${context.packageName}.fileprovider"

    override fun observe(tripId: Long) = dao.observe(tripId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getJpeg(tripId: Long, photoId: Long) = dao.getJpeg(tripId, photoId)

    override suspend fun add(tripId: Long, itineraryId: Long, source: String): Boolean {
        if (!canAdd(tripId, itineraryId)) return false
        val jpeg = withContext(Dispatchers.IO) { readJpeg(Uri.parse(source)) }
        return database.withTransaction {
            if (!canAdd(tripId, itineraryId)) return@withTransaction false
            dao.insert(ItineraryPhotoEntity(itineraryId = itineraryId, jpeg = jpeg))
            true
        }
    }

    override suspend fun createCaptureTarget(): String = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "itinerary_captures")
        check(directory.isDirectory || directory.mkdirs())
        val expiredAt = clock.millis() - CAPTURE_MAX_AGE_MILLIS
        directory.listFiles()?.filter { it.lastModified() < expiredAt }?.forEach { it.delete() }
        val file = File.createTempFile("capture-", ".jpg", directory)
        FileProvider.getUriForFile(context, authority, file).toString()
    }

    override suspend fun discardCaptureTarget(target: String) {
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(target)
            if (uri.authority == authority && uri.pathSegments.firstOrNull() == "itinerary_captures") {
                context.contentResolver.delete(uri, null, null)
            }
        }
    }

    private suspend fun canAdd(tripId: Long, itineraryId: Long): Boolean {
        val item = database.itineraryItemDao().getItem(itineraryId) ?: return false
        if (item.tripPlanId != tripId) return false
        val trip = database.tripPlanDao().getTripPlan(tripId) ?: return false
        return !trip.isDraft && trip.completedAt == null &&
            LocalDate.now(clock) in LocalDate.parse(trip.startDate)..LocalDate.parse(trip.endDate)
    }

    private fun readJpeg(uri: Uri): ByteArray {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val width = info.size.width
            val height = info.size.height
            val maxSide = maxOf(width, height)
            if (maxSide > MAX_SIDE) {
                val scale = MAX_SIDE.toDouble() / maxSide
                decoder.setTargetSize((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
            }
        }
        try {
            for (quality in listOf(85, 70, 55, 40, 25)) {
                val output = ByteArrayOutputStream()
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output))
                if (output.size() <= MAX_JPEG_BYTES) return output.toByteArray()
            }
            val reduced = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width / 2).coerceAtLeast(1),
                (bitmap.height / 2).coerceAtLeast(1),
                true,
            )
            try {
                val output = ByteArrayOutputStream()
                check(reduced.compress(Bitmap.CompressFormat.JPEG, 55, output))
                check(output.size() <= MAX_JPEG_BYTES)
                return output.toByteArray()
            } finally {
                if (reduced !== bitmap) reduced.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        const val MAX_SIDE = 1600
        const val MAX_JPEG_BYTES = 500 * 1024
        const val CAPTURE_MAX_AGE_MILLIS = 24 * 60 * 60 * 1000L
    }
}
