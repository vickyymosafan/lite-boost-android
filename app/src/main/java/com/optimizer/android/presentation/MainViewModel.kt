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

data class MainUiState(
    val storageStat: StorageStatus = StorageStatus(0, 0),
    val ramStat: RamStatus = RamStatus(0, 0),
    val batteryStat: BatteryStatus = BatteryStatus(0f, BatteryHealth.UNKNOWN),
    val logs: List<String> = listOf("SYSTEM BOOT OK."),
    val isScanning: Boolean = false,
    val showVpnDialog: Boolean = false,
    val showHibernationDialog: Boolean = false,
    val showBlackholeDialog: Boolean = false,
    val showJunkDialog: Boolean = false,
    val showRamDialog: Boolean = false,
    val vaultContent: String = "VAULT IS EMPTY."
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

    fun toggleDialog(dialog: String, show: Boolean) {
        _uiState.update { state ->
            when (dialog) {
                "vpn" -> state.copy(showVpnDialog = show)
                "hibernation" -> state.copy(showHibernationDialog = show)
                "blackhole" -> state.copy(showBlackholeDialog = show)
                "junk" -> state.copy(showJunkDialog = show)
                "ram" -> state.copy(showRamDialog = show)
                else -> state
            }
        }
    }

    fun loadVaultContent(filesDir: File) {
        viewModelScope.launch {
            val file = File(filesDir, "vault.txt")
            val content = if (file.exists()) file.readText() else "VAULT IS EMPTY."
            _uiState.update { it.copy(vaultContent = content) }
        }
    }

    fun startJunkScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, showJunkDialog = false) }
            log("MEMULAI SCANNING SAMPAH & CACHE...")
            
            systemRepository.scanJunk().collect { msg -> log(msg) }
            val found = systemRepository.getJunkFiles()
            
            if (found.isNotEmpty()) {
                log("MEMBERSIHKAN ${found.size} BERKAS SAMPAH...")
                systemRepository.deleteJunkFiles(found).collect { msg -> log(msg) }
                refreshSystemStatus()
                log("CLEANUP SELESAI! PEMERSIHAN SUKSES.")
            } else {
                log("PENYIMPANAN SUDAH BERSIH.")
            }
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun boostRam() {
        viewModelScope.launch {
            _uiState.update { it.copy(showRamDialog = false) }
            log("TRIGGERING RAM OPTIMIZATION & GC...")
            System.gc()
            
            systemRepository.killBackgroundProcesses().collect { msg -> log(msg) }
            
            refreshSystemStatus()
            log("RAM BOOSTED! FREE RAM: ${_uiState.value.ramStat.freeMb} MB")
        }
    }
}
