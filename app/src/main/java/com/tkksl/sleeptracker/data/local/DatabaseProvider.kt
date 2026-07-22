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
            )
                // 开发阶段：版本变更直接重建数据库，不用写迁移脚本
                .fallbackToDestructiveMigration()
                .build()

            INSTANCE = instance

            instance
        }
    }
}