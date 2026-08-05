package com.optimizer.android.presentation

import androidx.lifecycle.ViewModel
import com.optimizer.android.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

data class CameraUiState(
    val isHdrEnabled: Boolean = false,
    val showToastMessage: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun setHdrEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isHdrEnabled = enabled) }
    }

    fun getPhotoFile(): File {
        return mediaRepository.createPhotoFile()
    }

    fun onPhotoSaved() {
        _uiState.update { it.copy(showToastMessage = "Foto berhasil disimpan!") }
    }

    fun onPhotoError(msg: String) {
        _uiState.update { it.copy(showToastMessage = "Gagal: $msg") }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(showToastMessage = null) }
    }
}
