package com.telefam.company.desktop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.telefam.company.desktop.ui.theme.TelefamCrimson
import com.telefam.company.shared.TelefamStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skia.Image as SkiaImage
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DesktopImageCache {
    private const val MAX_ENTRIES = 64
    private val map = Collections.synchronizedMap(
        object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean =
                size > MAX_ENTRIES
        }
    )

    fun get(url: String): ImageBitmap? = map[url]
    fun put(url: String, bitmap: ImageBitmap) {
        map[url] = bitmap
    }
}

@Composable
fun DesktopAsyncImage(
    url: String,
    httpClient: OkHttpClient,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var bitmap by remember(url) { mutableStateOf(DesktopImageCache.get(url)) }

    LaunchedEffect(url) {
        if (url.isNotBlank() && bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val req = Request.Builder().url(url).get().build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@runCatching null
                        val bytes = resp.body?.bytes() ?: return@runCatching null
                        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                    }
                }.getOrNull()
            }
            if (loaded != null) {
                DesktopImageCache.put(url, loaded)
                bitmap = loaded
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(Color(0xFF1F2026)))
    }
}

@Composable
fun DesktopPhotoCropDialog(
    imageBytes: ByteArray,
    aspectRatio: Float = 1f,
    isCircularOverlay: Boolean = true,
    onDismiss: () -> Unit,
    onApplyCrop: (Double, Double, Double, Double) -> Unit
) {
    val previewBitmap = remember(imageBytes) {
        runCatching { SkiaImage.makeFromEncoded(imageBytes).toComposeImageBitmap() }.getOrNull()
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = TelefamStrings.CROP_PHOTO,
                    color = Color(0xFF1A1C1E),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF121214))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val maxShift = 120f * (scale - 1f) + 60f
                                offsetX = (offsetX + dragAmount.x).coerceIn(-maxShift, maxShift)
                                offsetY = (offsetY + dragAmount.y).coerceIn(-maxShift, maxShift)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap,
                            contentDescription = TelefamStrings.CROP_PHOTO,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        )
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (isCircularOverlay) {
                            drawCircle(
                                color = Color.White,
                                radius = size.minDimension * 0.44f,
                                center = Offset(size.width / 2f, size.height / 2f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        } else {
                            val padX = size.width * 0.04f
                            val padY = size.height * 0.04f
                            drawRect(
                                color = Color.White.copy(alpha = 0.9f),
                                topLeft = Offset(padX, padY),
                                size = Size(size.width - padX * 2f, size.height - padY * 2f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Slider(
                    value = scale,
                    onValueChange = { scale = it },
                    valueRange = 1f..3f,
                    colors = SliderDefaults.colors(
                        thumbColor = TelefamCrimson,
                        activeTrackColor = TelefamCrimson
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(TelefamStrings.CANCEL, color = Color(0xFF1A1C1E))
                    }
                    Button(
                        onClick = {
                            val cropW = (1.0 / scale.toDouble()).coerceIn(0.25, 1.0)
                            val cropH = if (aspectRatio > 1f) {
                                (cropW / aspectRatio.toDouble()).coerceIn(0.25, 1.0)
                            } else {
                                cropW
                            }
                            val cx = (0.5 - cropW / 2.0 - (offsetX / 480.0)).coerceIn(0.0, 1.0 - cropW)
                            val cy = (0.5 - cropH / 2.0 - (offsetY / 480.0)).coerceIn(0.0, 1.0 - cropH)
                            onApplyCrop(cx, cy, cropW, cropH)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("apply_crop_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson)
                    ) {
                        Text(TelefamStrings.APPLY_CROP, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopDatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        onDateSelected(sdf.format(Date(ms)))
                    }
                    onDismiss()
                }
            ) {
                Text(TelefamStrings.APPLY_CROP, color = TelefamCrimson)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(TelefamStrings.CANCEL)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
