package com.emoji.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.emoji.app.domain.AppConfig
import com.emoji.app.domain.model.MediaType
import com.emoji.app.domain.model.PhotoText
import com.emoji.app.domain.model.SampleMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * SharedViewModel — central state holder shared across all screens.
 *
 * mediaType: VIDEO | PHOTO
 * Photo path: count=1 → 3 steps / count≥2 → 4 steps
 * Video path: always 4 steps
 */
class SharedViewModel : ViewModel() {

    // ========== Core state ==========
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // ========== Convenience accessors ==========
    val mediaType: MediaType get() = _state.value.mediaType
    val currentStep: Int get() = _state.value.currentStep
    val photos: List<Uri> get() = _state.value.photos
    val isSinglePhoto: Boolean get() = mediaType == MediaType.PHOTO && photos.size == 1
    val isMultiPhoto: Boolean get() = mediaType == MediaType.PHOTO && photos.size >= 2
    val isExporting: Boolean get() = _state.value.exporting

    // ========== Step navigation ==========
    fun setStep(step: Int) {
        _state.update { it.copy(currentStep = step) }
    }

    fun nextStep() {
        _state.update { it.copy(currentStep = it.currentStep + 1) }
    }

    fun prevStep() {
        _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    // ========== Media type ==========
    fun setMediaType(type: MediaType) {
        _state.update { it.copy(mediaType = type) }
    }

    // ========== Photo operations ==========
    fun setPhotos(uris: List<Uri>) {
        val clamped = if (uris.size > AppConfig.PHOTO_MAX_COUNT) {
            uris.take(AppConfig.PHOTO_MAX_COUNT)
        } else uris
        _state.update {
            it.copy(
                photos = clamped,
                photoTexts = MutableList(clamped.size) { null },
                currentPhotoIdx = 0,
                frameDelay = AppConfig.FRAME_DELAY_DEFAULT_MS
            )
        }
    }

    fun reorderPhotos(fromIdx: Int, toIdx: Int) {
        _state.update { s ->
            val newPhotos = s.photos.toMutableList()
            val item = newPhotos.removeAt(fromIdx)
            newPhotos.add(toIdx, item)
            val newTexts = s.photoTexts.toMutableList()
            val textItem = newTexts.removeAt(fromIdx)
            newTexts.add(toIdx, textItem)
            s.copy(photos = newPhotos, photoTexts = newTexts)
        }
    }

    fun swapPhotos(idxA: Int, idxB: Int) {
        if (idxA == idxB) return
        _state.update { s ->
            val newPhotos = s.photos.toMutableList()
            val tmp = newPhotos[idxA]
            newPhotos[idxA] = newPhotos[idxB]
            newPhotos[idxB] = tmp
            val newTexts = s.photoTexts.toMutableList()
            val tmpT = newTexts[idxA]
            newTexts[idxA] = newTexts[idxB]
            newTexts[idxB] = tmpT
            s.copy(photos = newPhotos, photoTexts = newTexts)
        }
    }

    fun removePhoto(idx: Int) {
        _state.update { s ->
            val newPhotos = s.photos.toMutableList().also { it.removeAt(idx) }
            val newTexts = s.photoTexts.toMutableList().also { it.removeAt(idx) }
            s.copy(
                photos = newPhotos,
                photoTexts = newTexts,
                currentPhotoIdx = s.currentPhotoIdx.coerceAtMost(newPhotos.size - 1)
            )
        }
    }

    fun addPhotos(newUris: List<Uri>) {
        _state.update { s ->
            val available = AppConfig.PHOTO_MAX_COUNT - s.photos.size
            val toAdd = if (newUris.size > available) newUris.take(available) else newUris
            s.copy(
                photos = s.photos + toAdd,
                photoTexts = s.photoTexts + MutableList(toAdd.size) { null }
            )
        }
    }

    fun setFrameDelay(delayMs: Int) {
        _state.update { it.copy(frameDelay = delayMs) }
    }

    // ========== Photo text (per-frame captions) ==========
    fun getPhotoText(idx: Int): PhotoText? {
        val texts = _state.value.photoTexts
        if (texts.getOrNull(idx) != null) return texts[idx]
        // Inheritance rule: look backward for most recent non-null
        for (i in idx - 1 downTo 0) {
            if (texts.getOrNull(i) != null) return texts[i]
        }
        return null
    }

    fun savePhotoText(idx: Int, text: PhotoText) {
        _state.update { s ->
            val newTexts = s.photoTexts.toMutableList()
            newTexts[idx] = text
            s.copy(photoTexts = newTexts)
        }
    }

    fun setCurrentPhotoIdx(idx: Int) {
        _state.update { it.copy(currentPhotoIdx = idx) }
    }

    // ========== Video operations ==========
    fun setVideo(uri: Uri, durationSec: Double) {
        _state.update {
            val safeDur = if (durationSec <= 0.0) AppConfig.TRIM_MAX_DURATION_SEC.toDouble() else durationSec
            it.copy(
                videoUri = uri,
                videoDurationSec = safeDur,
                trimStartSec = 0.0,
                trimEndSec = minOf(AppConfig.TRIM_MAX_DURATION_SEC.toDouble(), safeDur)
            )
        }
    }

    fun setTrim(startSec: Double, endSec: Double) {
        _state.update { it.copy(trimStartSec = startSec, trimEndSec = endSec) }
    }

    fun setSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
    }

    fun setSampleMode(mode: SampleMode) {
        _state.update { it.copy(sampleMode = mode) }
    }

    // ========== Text (shared between video and photo) ==========
    fun setTextContent(content: String) {
        _state.update { it.copy(textContent = content.take(AppConfig.TEXT_MAX_LENGTH)) }
    }

    fun setTextFont(fontName: String) {
        _state.update { it.copy(textFont = fontName) }
    }

    fun setTextColor(color: Int) {
        _state.update { it.copy(textColor = color) }
    }

    fun setTextSize(sizePx: Int) {
        _state.update { it.copy(textSizePx = sizePx) }
    }

    fun setTextPosition(x: Float, y: Float) {
        _state.update { it.copy(textX = x, textY = y) }
    }

    fun setTextWeight(weight: String) {
        _state.update { it.copy(textWeight = weight) }
    }

    // ========== Quality ==========
    fun setQuality(quality: AppConfig.Quality) {
        _state.update { it.copy(quality = quality) }
    }

    // ========== Export state ==========
    fun setExporting(exporting: Boolean) {
        _state.update { it.copy(exporting = exporting) }
    }

    fun setExportProgress(text: String, fraction: Float) {
        _state.update { it.copy(exportProgressText = text, exportProgressFraction = fraction) }
    }

    fun setGifBlob(data: ByteArray?) {
        _state.update { it.copy(gifBlob = data) }
    }

    // ========== Pre-rendered frames (④ BurstPreRenderer) ==========
    fun setPreRenderedFrames(frames: List<android.graphics.Bitmap>) {
        _state.update { it.copy(preRenderedFrames = frames) }
    }

    // ========== Full reset ==========
    fun reset() {
        val s = _state.value
        s.preRenderedFrames.forEach { it.recycle() }
        _state.value = AppState()
    }
}

// ========== Centralized state ==========
data class AppState(
    val mediaType: MediaType = MediaType.VIDEO,
    val currentStep: Int = 0,

    // Video
    val videoUri: Uri? = null,
    val videoDurationSec: Double = 0.0,
    val trimStartSec: Double = 0.0,
    val trimEndSec: Double = AppConfig.TRIM_MAX_DURATION_SEC.toDouble(),
    val speed: Float = AppConfig.DEFAULT_SPEED,
    val sampleMode: SampleMode = SampleMode.TRUNCATE,

    // Photo
    val photos: List<Uri> = emptyList(),
    val photoTexts: List<PhotoText?> = emptyList(),
    val frameDelay: Int = AppConfig.FRAME_DELAY_DEFAULT_MS,
    val currentPhotoIdx: Int = 0,

    // Text (shared)
    val textContent: String = "",
    val textFont: String = AppConfig.DEFAULT_FONT,
    val textColor: Int = AppConfig.DEFAULT_COLOR,
    val textWeight: String = "700",
    val textX: Float = AppConfig.DEFAULT_TEXT_X,
    val textY: Float = AppConfig.DEFAULT_TEXT_Y,
    val textSizePx: Int = AppConfig.FONT_SIZE_DEFAULT_PX,

    // Quality
    val quality: AppConfig.Quality = AppConfig.DEFAULT_QUALITY,

    // Export
    val exporting: Boolean = false,
    val exportProgressText: String = "",
    val exportProgressFraction: Float = 0f,
    val gifBlob: ByteArray? = null,

    // ④ Burst pre-rendered frames
    val preRenderedFrames: List<android.graphics.Bitmap> = emptyList(),
)
