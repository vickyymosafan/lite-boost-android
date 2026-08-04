package com.optimizer.android.domain.model

data class StorageStatus(val freeMb: Long, val totalMb: Long)

data class RamStatus(val freeMb: Long, val totalMb: Long)

enum class BatteryHealth {
    GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, FAILURE, COLD, UNKNOWN
}

data class BatteryStatus(val tempCelsius: Float, val health: BatteryHealth)

data class JunkFile(val file: java.io.File, val size: Long)
