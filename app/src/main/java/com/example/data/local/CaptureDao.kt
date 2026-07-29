package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Query("SELECT * FROM capture_items ORDER BY timestamp DESC")
    fun getAllCaptures(): Flow<List<CaptureItemEntity>>

    @Query("SELECT * FROM capture_items WHERE type = :type ORDER BY timestamp DESC")
    fun getCapturesByType(type: CaptureType): Flow<List<CaptureItemEntity>>

    @Query("SELECT * FROM capture_items WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getCapturesByProject(projectId: Long): Flow<List<CaptureItemEntity>>

    @Query("SELECT * FROM capture_items WHERE id = :id")
    suspend fun getCaptureById(id: Long): CaptureItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: CaptureItemEntity): Long

    @Update
    suspend fun updateCapture(capture: CaptureItemEntity)

    @Delete
    suspend fun deleteCapture(capture: CaptureItemEntity)

    @Query("DELETE FROM capture_items WHERE id = :id")
    suspend fun deleteCaptureById(id: Long)

    @Query("SELECT * FROM capture_items WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncCaptures(): List<CaptureItemEntity>

    @Query("UPDATE capture_items SET syncStatus = :status, driveFileId = :driveFileId, driveSyncTime = :syncTime WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, driveFileId: String?, syncTime: Long = System.currentTimeMillis())
}
