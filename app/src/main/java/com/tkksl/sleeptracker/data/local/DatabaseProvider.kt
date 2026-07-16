package com.tkksl.sleeptracker.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var INSTANCE: SleepDatabase? = null

    fun getDatabase(context: Context): SleepDatabase {

        return INSTANCE ?: synchronized(this) {

            val instance = Room.databaseBuilder(
                context.applicationContext,
                SleepDatabase::class.java,
                "sleep_database"
            ).build()

            INSTANCE = instance

            instance
        }
    }
}