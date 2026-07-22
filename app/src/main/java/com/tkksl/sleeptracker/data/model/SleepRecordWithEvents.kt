package com.tkksl.sleeptracker.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class SleepRecordWithEvents(
    @Embedded val record: SleepRecord,
    @Relation(
        parentColumn = "id",
        entityColumn = "sleepRecordId",
        entity = AudioEventEntity::class
    )
    val events: List<AudioEventEntity>
)