package com.tkksl.sleeptracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tkksl.sleeptracker.data.model.AudioEventEntity
import com.tkksl.sleeptracker.data.model.SleepRecord

@Database(
    entities = [
        SleepRecord::class,
        AudioEventEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class SleepDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
}