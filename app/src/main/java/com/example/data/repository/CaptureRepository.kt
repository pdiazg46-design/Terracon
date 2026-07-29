package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.ai.GeminiService
import com.example.data.local.AppDatabase
import com.example.data.local.CaptureItemEntity
import com.example.data.local.CaptureType
import com.example.data.local.ProjectEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

class CaptureRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val projectDao = db.projectDao()
    private val captureDao = db.captureDao()
    private val driveSyncManager = com.example.data.drive.DriveSyncManager(context)

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val allCaptures: Flow<List<CaptureItemEntity>> = captureDao.getAllCaptures()

    fun getCapturesByType(type: CaptureType): Flow<List<CaptureItemEntity>> {
        return captureDao.getCapturesByType(type)
    }

    suspend fun ensureDefaultProjectsExist() {
        val currentProjects = allProjects.first()
        val hasCarreraPinto = currentProjects.any { it.name == "Carrera Pinto" }
        val hasDiegoAlmagro = currentProjects.any { it.name == "Diego de Almagro" }
        
        if (currentProjects.isEmpty() || !hasCarreraPinto || !hasDiegoAlmagro || currentProjects.size > 2) {
            projectDao.deleteAllProjects()
            projectDao.insertProject(
                ProjectEntity(
                    name = "Carrera Pinto",
                    description = "Proyecto Activo AT-SIT - Carrera Pinto",
                    clientName = "AT-SIT",
                    colorHex = "#283593"
                )
            )
            projectDao.insertProject(
                ProjectEntity(
                    name = "Diego de Almagro",
                    description = "Proyecto Activo AT-SIT - Diego de Almagro",
                    clientName = "AT-SIT",
                    colorHex = "#00897B"
                )
            )
        }
    }

    suspend fun createProject(name: String, description: String, clientName: String, colorHex: String): Long {
        return projectDao.insertProject(
            ProjectEntity(
                name = name,
                description = description,
                clientName = clientName,
                colorHex = colorHex
            )
        )
    }

    suspend fun saveAndProcessCapture(
        title: String,
        type: CaptureType,
        projectId: Long,
        projectName: String,
        audioPath: String? = null,
        audioDurationSeconds: Int = 0,
        photoPath: String? = null,
        rawNotes: String = "",
        userEnteredAmount: Double = 0.0
    ): Long {
        var bitmap: Bitmap? = null
        if (!photoPath.isNullOrBlank()) {
            val file = File(photoPath)
            if (file.exists()) {
                bitmap = BitmapFactory.decodeFile(file.absolutePath)
            }
        }

        val aiResult = GeminiService.processCaptureWithAI(
            title = title,
            type = type.name,
            projectName = projectName,
            notes = rawNotes,
            bitmap = bitmap
        )

        val finalAmount = if (userEnteredAmount > 0) userEnteredAmount else aiResult.estimatedAmount

        val entity = CaptureItemEntity(
            type = type,
            title = title,
            projectId = projectId,
            projectName = projectName,
            audioPath = audioPath,
            audioDurationSeconds = audioDurationSeconds,
            photoPath = photoPath,
            amount = finalAmount,
            expenseMerchant = aiResult.merchant,
            expenseCategory = aiResult.category,
            priority = aiResult.priority,
            isCompleted = false,
            aiSummary = aiResult.summary,
            aiActionItems = aiResult.actionItems,
            rawNotes = rawNotes
        )

        val id = captureDao.insertCapture(entity)
        // Auto-intentar sync a Drive si hay conexión
        try {
            driveSyncManager.syncPendingToDrive()
        } catch (_: Exception) {}
        return id
    }

    suspend fun syncAllDriveCaptures(): Int {
        return driveSyncManager.syncPendingToDrive()
    }

    suspend fun markAgentCleared(captureId: Long) {
        captureDao.updateSyncStatus(
            id = captureId,
            status = "CLEARED_BY_AGENT",
            driveFileId = null,
            syncTime = System.currentTimeMillis()
        )
        driveSyncManager.cleanProcessedDriveInbox(captureId)
    }

    suspend fun toggleTaskCompleted(capture: CaptureItemEntity) {
        val updated = capture.copy(isCompleted = !capture.isCompleted)
        captureDao.updateCapture(updated)
    }

    suspend fun deleteCapture(id: Long) {
        captureDao.deleteCaptureById(id)
    }

    suspend fun askProjectAI(query: String): String {
        val projects = allProjects.first()
        val captures = allCaptures.first()
        return GeminiService.askProjectManagerAI(query, projects, captures)
    }
}
