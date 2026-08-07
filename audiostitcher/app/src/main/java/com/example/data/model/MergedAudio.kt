package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merged_audios")
data class MergedAudio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val projectName: String,
    val fileName: String,
    val filePath: String,
    val totalDurationMs: Long,
    val clipCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L
)
