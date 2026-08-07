package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ProjectWithClips(
    @Embedded val project: AudioProject,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val clips: List<AudioClip>
) {
    val sortedClips: List<AudioClip>
        get() = clips.sortedBy { it.orderIndex }

    val totalDurationMs: Long
        get() = clips.sumOf { it.durationMs }

    val totalSizeBytes: Long
        get() = clips.sumOf { it.fileSizeBytes }
}
