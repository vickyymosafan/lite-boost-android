package com.optimizer.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optimizer.android.domain.model.AppItem
import com.optimizer.android.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppEraserUiState(
    val apps: List<AppItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AppEraserViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppEraserUiState())
    val uiState: StateFlow<AppEraserUiState> = _uiState.asStateFlow()

    fun loadApps(currentPackageName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val appsList = appRepository.getInstalledApps(currentPackageName)
            _uiState.update { it.copy(apps = appsList, isLoading = false) }
        }
    }
}
