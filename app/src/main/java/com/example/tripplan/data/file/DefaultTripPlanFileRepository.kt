package com.example.tripplan.data.file

import android.content.Context
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.tripplan.data.local.ItineraryItemDao
import com.example.tripplan.data.local.TripPlanDao
import com.example.tripplan.data.local.TripPlanDatabase
import com.example.tripplan.data.local.PackingItemDao
import com.example.tripplan.data.local.PackingItemEntity
import com.example.tripplan.data.local.PlaceDao
import com.example.tripplan.data.local.PlaceEntity
import com.example.tripplan.data.mapper.TripPlanFileMapper
import com.example.tripplan.domain.model.ExportedTripPlanFile
import com.example.tripplan.domain.model.CountryCurrencyCatalog
import com.example.tripplan.domain.model.TripPlanFileFailure
import com.example.tripplan.domain.model.TripPlanFileResult
import com.example.tripplan.domain.model.TripPlanImportPreview
import com.example.tripplan.domain.repository.TripPlanFileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Currency
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

class DefaultTripPlanFileRepository @Inject constructor(
    private val database: TripPlanDatabase,
    private val tripPlanDao: TripPlanDao,
    private val itineraryItemDao: ItineraryItemDao,
    private val packingItemDao: PackingItemDao,
    private val placeDao: PlaceDao,
    private val clock: Clock,
    @ApplicationContext private val context: Context,
) : TripPlanFileRepository {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportTrip(
        tripPlanId: Long,
    ): TripPlanFileResult<ExportedTripPlanFile> = withContext(Dispatchers.IO) {
        val trip = tripPlanDao.getTripPlan(tripPlanId)
            ?: return@withContext TripPlanFileResult.Failure(TripPlanFileFailure.TripNotFound)
        val items = itineraryItemDao.observeItems(tripPlanId).first()
        val packingItems = packingItemDao.observeItems(tripPlanId).first()
            .filter { it.type == "Shared" }
        val dto = TripPlanFileV2Dto(
            formatVersion = TRIP_PLAN_FILE_VERSION_2,
            trip = SharedTripDto(
                title = trip.title,
                destination = trip.destination,
                startDate = trip.startDate,
                endDate = trip.endDate,
                description = trip.description,
                countryCode = trip.countryCode,
                currencyCode = trip.currencyCode,
            ),
            itineraryItems = items.map { item ->
                val place = item.placeId?.let { placeDao.getPlace(it) }
                SharedItineraryItemV2Dto(
                    item.date, item.title, item.place,
                    place?.let { SharedPlaceDetailsDto(it.address, it.latitude, it.longitude) },
                    item.startTime, item.endTime, item.note, item.sortOrder,
                )
            },
            sharedPackingItems = packingItems.map {
                SharedPackingItemDto(it.name, it.category, it.quantity, it.memo, it.sortOrder)
            },
        )
        val safeTitle = trip.title.trim().replace(Regex("[^가-힣A-Za-z0-9._-]+"), "_")
            .trim('_').ifBlank { "trip_plan" }
        TripPlanFileResult.Success(
            ExportedTripPlanFile(
                fileName = "${safeTitle}.tripplan.json",
                content = json.encodeToString(TripPlanFileV2Dto.serializer(), dto),
            )
        )
    }

    override fun previewImport(content: String): TripPlanFileResult<TripPlanImportPreview> {
        return when (val decoded = decodeAndValidate(content)) {
            is TripPlanFileResult.Failure -> decoded
            is TripPlanFileResult.Success -> {
                val dto = decoded.value
                val startDate = LocalDate.parse(dto.trip.startDate)
                val endDate = LocalDate.parse(dto.trip.endDate)
                TripPlanFileResult.Success(
                    TripPlanImportPreview(
                        title = dto.trip.title,
                        destination = dto.trip.destination,
                        startDate = startDate,
                        endDate = endDate,
                        dayCount = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1,
                        itineraryItemCount = dto.items.size,
                    )
                )
            }
        }
    }

    override suspend fun importTrip(content: String): TripPlanFileResult<Long> {
        val dto = when (val decoded = decodeAndValidate(content)) {
            is TripPlanFileResult.Failure -> return decoded
            is TripPlanFileResult.Success -> decoded.value
        }
        return try {
            val tripPlanId = database.withTransaction {
                val now = clock.instant().toEpochMilli()
                val id = tripPlanDao.insertTripPlan(TripPlanFileMapper.toTripEntity(dto.trip, now))
                dto.items.sortedWith(
                    compareBy({ it.date }, { it.order })
                ).forEach { item ->
                    val placeId = item.placeDetails?.let { details ->
                        placeDao.insertPlace(PlaceEntity(name=item.place.trim(), address=details.address.trim(),
                            latitude=details.latitude, longitude=details.longitude, source="Manual",
                            providerName=null, externalId=null, createdAt=now, updatedAt=now))
                    }
                    itineraryItemDao.insertItem(TripPlanFileMapper.toItineraryEntity(
                        SharedItineraryItemDto(item.date,item.title,item.place,item.startTime,item.endTime,item.note,item.order), id
                    ).copy(placeId=placeId))
                }
                dto.sharedPackingItems.sortedBy { it.order }.forEach { item ->
                    packingItemDao.insertItem(PackingItemEntity(tripPlanId=id,name=item.name.trim(),category=item.category.trim(),
                        quantity=item.quantity,memo=item.memo.trim(),type="Shared",checkedAt=null,sortOrder=item.order))
                }
                id
            }
            TripPlanFileResult.Success(tripPlanId)
        } catch (_: Exception) {
            TripPlanFileResult.Failure(TripPlanFileFailure.InvalidData)
        }
    }

    override suspend fun readDocument(uri: String): TripPlanFileResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(android.net.Uri.parse(uri))
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: return@withContext TripPlanFileResult.Failure(TripPlanFileFailure.ReadFailed)
                TripPlanFileResult.Success(content)
            } catch (_: Exception) {
                TripPlanFileResult.Failure(TripPlanFileFailure.ReadFailed)
            }
        }

    override suspend fun writeDocument(uri: String, content: String): TripPlanFileResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(android.net.Uri.parse(uri), "wt")
                    ?.bufferedWriter(Charsets.UTF_8)?.use { it.write(content) }
                    ?: return@withContext TripPlanFileResult.Failure(TripPlanFileFailure.WriteFailed)
                TripPlanFileResult.Success(Unit)
            } catch (_: Exception) {
                TripPlanFileResult.Failure(TripPlanFileFailure.WriteFailed)
            }
        }

    override suspend fun createShareDocument(
        fileName: String,
        content: String,
    ): TripPlanFileResult<String> = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.cacheDir, "shared_trips").apply { mkdirs() }
            val file = File(directory, fileName).apply { writeText(content, Charsets.UTF_8) }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            TripPlanFileResult.Success(uri.toString())
        } catch (_: Exception) {
            TripPlanFileResult.Failure(TripPlanFileFailure.WriteFailed)
        }
    }

    private fun decodeAndValidate(content: String): TripPlanFileResult<DecodedFile> {
        val decoded = try {
            val version = json.parseToJsonElement(content).jsonObject["formatVersion"]?.jsonPrimitive?.intOrNull
            when (version) {
                CURRENT_TRIP_PLAN_FILE_VERSION -> {
                    val dto = json.decodeFromString(TripPlanFileDto.serializer(), content)
                    DecodedFile(dto.trip, dto.itineraryItems.map {
                        DecodedItem(it.date,it.title,it.place,null,it.startTime,it.endTime,it.note,it.order)
                    }, emptyList())
                }
                TRIP_PLAN_FILE_VERSION_2 -> {
                    val dto = json.decodeFromString(TripPlanFileV2Dto.serializer(), content)
                    DecodedFile(dto.trip, dto.itineraryItems.map {
                        DecodedItem(it.date,it.title,it.place,it.placeDetails,it.startTime,it.endTime,it.note,it.order)
                    }, dto.sharedPackingItems)
                }
                else -> return TripPlanFileResult.Failure(TripPlanFileFailure.UnsupportedVersion)
            }
        } catch (_: SerializationException) {
            return TripPlanFileResult.Failure(TripPlanFileFailure.InvalidFormat)
        } catch (_: IllegalArgumentException) {
            return TripPlanFileResult.Failure(TripPlanFileFailure.InvalidFormat)
        }
        val startDate = decoded.trip.startDate.toDateOrNull()
        val endDate = decoded.trip.endDate.toDateOrNull()
        if (decoded.trip.title.isBlank() || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return TripPlanFileResult.Failure(TripPlanFileFailure.InvalidData)
        }
        val validItems = decoded.items.all { item ->
            val date = item.date.toDateOrNull()
            val startTime = item.startTime.toTimeOrNull()
            val endTime = item.endTime.toTimeOrNull()
            item.title.isNotBlank() && item.order >= 0 && date != null &&
                !date.isBefore(startDate) && !date.isAfter(endDate) &&
                (item.startTime == null || startTime != null) &&
                (item.endTime == null || endTime != null) &&
                (startTime == null || endTime == null || !endTime.isBefore(startTime))
        }
        val validPacking = decoded.sharedPackingItems.all { it.name.isNotBlank() && it.quantity > 0 && it.order >= 0 }
        val validCountryAndCurrency = CountryCurrencyCatalog.find(decoded.trip.countryCode) != null &&
            runCatching { Currency.getInstance(decoded.trip.currencyCode) }.isSuccess
        return if (validItems && validPacking && validCountryAndCurrency) TripPlanFileResult.Success(decoded)
        else TripPlanFileResult.Failure(TripPlanFileFailure.InvalidData)
    }

    private data class DecodedFile(
        val trip: SharedTripDto,
        val items: List<DecodedItem>,
        val sharedPackingItems: List<SharedPackingItemDto>,
    )
    private data class DecodedItem(
        val date:String,val title:String,val place:String,val placeDetails:SharedPlaceDetailsDto?,
        val startTime:String?,val endTime:String?,val note:String,val order:Int,
    )
}

private fun String.toDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
private fun String?.toTimeOrNull(): LocalTime? = this?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
