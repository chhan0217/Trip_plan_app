package com.example.tripplan.presentation.itineraryphoto

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val VisiblePhotoCount = 3

@Composable
internal fun ItineraryPhotoStrip(
    photoIds: List<Long>,
    readPhoto: suspend (Long) -> ByteArray?,
    onOpen: (Int) -> Unit,
) {
    if (photoIds.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        photoIds.take(VisiblePhotoCount).forEachIndexed { index, photoId ->
            val description = "일정 사진 ${index + 1}"
            Surface(
                onClick = { onOpen(index) },
                modifier = Modifier.weight(1f).aspectRatio(1f).semantics { contentDescription = description },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                val image = rememberPhotoImage(photoId, readPhoto, thumbnail = true).image
                if (image != null) {
                    Image(image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
        val remaining = photoIds.size - VisiblePhotoCount
        if (remaining > 0) {
            Surface(
                onClick = { onOpen(VisiblePhotoCount) },
                modifier = Modifier.weight(1f).aspectRatio(1f),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+$remaining", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        // Keep a single thumbnail the same size as a full row.
        repeat(4 - photoIds.size.coerceAtMost(4)) {
            Box(Modifier.weight(1f))
        }
    }
}

@Composable
internal fun ItineraryPhotoViewer(
    photoIds: List<Long>,
    initialIndex: Int,
    readPhoto: suspend (Long) -> ByteArray?,
    onDismiss: () -> Unit,
) {
    if (photoIds.isEmpty()) return
    val pager = rememberPagerState(initialPage = initialIndex.coerceIn(photoIds.indices)) { photoIds.size }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black, contentColor = Color.White) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize().padding(top = 52.dp, bottom = 12.dp).testTag("itinerary_photo_pager"),
                    key = { photoIds[it] },
                ) { index ->
                    val photo = rememberPhotoImage(photoIds[index], readPhoto)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val image = photo.image
                        when {
                            image != null -> Image(
                                image,
                                contentDescription = "일정 사진 ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                            )
                            photo.loading -> CircularProgressIndicator(color = Color.White)
                            else -> Text("사진을 불러오지 못했습니다.")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${pager.currentPage + 1} / ${photoIds.size}", modifier = Modifier.padding(start = 8.dp))
                    TextButton(onClick = onDismiss) { Text("닫기", color = Color.White) }
                }
            }
        }
    }
}

private data class PhotoImage(val image: ImageBitmap? = null, val loading: Boolean = true)

@Composable
private fun rememberPhotoImage(
    photoId: Long,
    readPhoto: suspend (Long) -> ByteArray?,
    thumbnail: Boolean = false,
): PhotoImage {
    val photo by produceState(PhotoImage(), photoId, thumbnail) {
        value = PhotoImage()
        val image = try {
            val jpeg = readPhoto(photoId)
            if (jpeg == null) null else withContext(Dispatchers.Default) {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = if (thumbnail) 4 else 1
                }
                BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options)?.asImageBitmap()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        value = PhotoImage(image, loading = false)
    }
    return photo
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ItineraryPhotoStripPreview() {
    MaterialTheme {
        ItineraryPhotoStrip(listOf(1, 2, 3, 4, 5), readPhoto = { null }, onOpen = {})
    }
}
