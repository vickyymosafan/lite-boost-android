package com.optimizer.android.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class StudioUiState(
    val statusLog: String = "PILIH VIDEO ATAU FOTO DARI GALERI",
    val mediaType: String = "none",
    val isPlaying: Boolean = false,
    val currentTab: Int = 0,
    
    // Color Grading
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val exposure: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val fade: Float = 0f,
    val sharpness: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    
    // Filter
    val activeFilterIdx: Int = 0,
    val filterIntensity: Float = 1f,
    
    // Speed & Trim
    val playbackSpeed: Float = 1f,
    val keepPitch: Boolean = true,
    val trimStartRatio: Float = 0f,
    val trimEndRatio: Float = 1f
)

@HiltViewModel
class StudioViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    fun updateStatus(log: String) {
        _uiState.update { it.copy(statusLog = log) }
    }
    
    fun setMediaType(type: String) {
        _uiState.update { it.copy(mediaType = type) }
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun setTab(tab: Int) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun updateColorGrading(
        brightness: Float? = null, contrast: Float? = null, saturation: Float? = null,
        temperature: Float? = null, tint: Float? = null, exposure: Float? = null,
        highlights: Float? = null, shadows: Float? = null, fade: Float? = null,
        sharpness: Float? = null, vignette: Float? = null, grain: Float? = null
    ) {
        _uiState.update { 
            it.copy(
                brightness = brightness ?: it.brightness,
                contrast = contrast ?: it.contrast,
                saturation = saturation ?: it.saturation,
                temperature = temperature ?: it.temperature,
                tint = tint ?: it.tint,
                exposure = exposure ?: it.exposure,
                highlights = highlights ?: it.highlights,
                shadows = shadows ?: it.shadows,
                fade = fade ?: it.fade,
                sharpness = sharpness ?: it.sharpness,
                vignette = vignette ?: it.vignette,
                grain = grain ?: it.grain
            )
        }
    }
    
    fun setFilter(idx: Int, intensity: Float? = null) {
        _uiState.update { 
            it.copy(
                activeFilterIdx = idx,
                filterIntensity = intensity ?: it.filterIntensity
            ) 
        }
    }

    fun resetColorGrading() {
        _uiState.update { 
            it.copy(
                brightness = 0f, contrast = 1f, saturation = 1f, temperature = 0f,
                tint = 0f, exposure = 0f, highlights = 0f, shadows = 0f, fade = 0f,
                sharpness = 0f, vignette = 0f, grain = 0f, activeFilterIdx = 0
            )
        }
    }
}
