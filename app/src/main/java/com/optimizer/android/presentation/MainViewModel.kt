package com.optimizer.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optimizer.android.domain.model.BatteryStatus
import com.optimizer.android.domain.model.RamStatus
import com.optimizer.android.domain.model.StorageStatus
import com.optimizer.android.domain.model.BatteryHealth
import com.optimizer.android.domain.repository.SystemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class DialogType { NONE, VPN, HIBERNATION, JUNK, RAM, BLACKHOLE }

data class MainUiState(
    val storageStat: StorageStatus = StorageStatus(0, 0),
    val ramStat: RamStatus = RamStatus(0, 0),
    val batteryStat: BatteryStatus = BatteryStatus(0f, BatteryHealth.UNKNOWN),
    val logs: List<String> = listOf("BOOT SISTEM BERHASIL."),
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val activeDialog: DialogType = DialogType.NONE,
    val vpnActive: Boolean = false,
    val vaultContent: String = "BRANKAS KOSONG."
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refreshSystemStatus()
    }

    fun log(message: String) {
        _uiState.update { it.copy(logs = it.logs + message) }
    }

    fun refreshSystemStatus() {
        _uiState.update {
            it.copy(
                storageStat = systemRepository.getStorageStatus(),
                ramStat = systemRepository.getRamStatus(),
                batteryStat = systemRepository.getBatteryStatus()
            )
        }
    }

    fun toggleDialog(dialog: DialogType) {
        _uiState.update { it.copy(activeDialog = if (it.activeDialog == dialog) DialogType.NONE else dialog) }
    }

    fun loadVaultContent() {
        viewModelScope.launch {
            val content = systemRepository.getVaultContent()
            _uiState.update { it.copy(vaultContent = content) }
        }
    }

    fun startJunkScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanProgress = 0f, activeDialog = DialogType.NONE) }
            log("MEMULAI SCANNING SAMPAH & CACHE...")

            systemRepository.scanJunk().collect { msg ->
                log(msg)
                _uiState.update { it.copy(scanProgress = (it.scanProgress + 0.05f).coerceAtMost(0.95f)) }
            }
            val found = systemRepository.getJunkFiles()

            if (found.isNotEmpty()) {
                log("MEMBERSIHKAN ${found.size} BERKAS SAMPAH...")
                systemRepository.deleteJunkFiles(found).collect { msg ->
                    log(msg)
                    _uiState.update { it.copy(scanProgress = (it.scanProgress + 0.05f).coerceAtMost(0.95f)) }
                }
                refreshSystemStatus()
                log("CLEANUP SELESAI! PEMBERSIHAN SUKSES.")
            } else {
                log("PENYIMPANAN SUDAH BERSIH.")
            }
            _uiState.update { it.copy(scanProgress = 1f, isScanning = false) }
        }
    }

    fun boostRam() {
        viewModelScope.launch {
            _uiState.update { it.copy(activeDialog = DialogType.NONE) }
            log("MEMULAI OPTIMISASI RAM & GC...")
            System.gc()
            
            systemRepository.killBackgroundProcesses().collect { msg -> log(msg) }
            
            refreshSystemStatus()
            log("RAM OPTIMAL! RAM BEBAS: ${_uiState.value.ramStat.freeMb} MB")
        }
    }
}
