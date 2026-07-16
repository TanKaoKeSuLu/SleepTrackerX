package com.tkksl.sleeptracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tkksl.sleeptracker.data.model.SleepRecord

@Database(
    entities = [SleepRecord::class],
    version = 1,
    exportSchema = false
)
abstract class SleepDatabase : RoomDatabase() {

    abstract fun sleepDao(): SleepDao

}