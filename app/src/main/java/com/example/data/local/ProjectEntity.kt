package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val clientName: String = "",
    val colorHex: String = "#3F51B5",
    val createdAt: Long = System.currentTimeMillis()
)
