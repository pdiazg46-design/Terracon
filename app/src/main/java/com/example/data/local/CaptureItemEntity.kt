package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CaptureType {
    MEETING,      // Grabar audios en reuniones
    EXPENSE,      // Tomar fotos a boletas/facturas + audio de justificación
    INSTRUCTION   // Recordatorios e instrucciones personales
}

@Entity(tableName = "capture_items")
data class CaptureItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: CaptureType,
    val title: String,
    val projectId: Long,
    val projectName: String,
    val audioPath: String? = null,
    val audioDurationSeconds: Int = 0,
    val photoPath: String? = null,
    val amount: Double = 0.0,
    val expenseMerchant: String = "",
    val expenseCategory: String = "",
    val priority: String = "Normal", // High, Normal, Low
    val isCompleted: Boolean = false,
    val aiSummary: String = "",
    val aiActionItems: String = "",
    val rawNotes: String = "",
    val syncStatus: String = "PENDING_SYNC", // PENDING_SYNC, SYNCED_DRIVE, CLEARED_BY_AGENT
    val driveFileId: String? = null,
    val driveSyncTime: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
