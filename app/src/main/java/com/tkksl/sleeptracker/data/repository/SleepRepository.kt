package com.tkksl.sleeptracker.data.repository

import android.util.Log
import androidx.room.Transaction
import com.tkksl.sleeptracker.data.local.SleepDao
import com.tkksl.sleeptracker.data.model.AudioEventEntity
import com.tkksl.sleeptracker.data.model.SleepRecord
import com.tkksl.sleeptracker.data.model.SleepRecordWithEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.io.File

class SleepRepository(private val sleepDao: SleepDao) {
    companion object {
        private const val TAG = "SleepRepository"
    }

    // 完整插入睡眠记录+声响事件，返回是否插入成功，原子事务：全部成功/全部回滚
    @Transaction
    suspend fun insertFullSleepRecord(
        record: SleepRecord,
        eventList: List<AudioEventEntity>
    ): Boolean {
        return try {
            val recordId = sleepDao.insertSleepRecord(record)
            Log.d(TAG, "insertFullSleepRecord new recordId=$recordId")
            val eventsWithId = eventList.map { it.copy(sleepRecordId = recordId) }
            sleepDao.insertAudioEvents(eventsWithId)
            true
        } catch (e: Exception) {
            Log.e(TAG, "insertFullSleepRecord insert failed", e)
            false
        }
    }

    fun getAllSleepRecordsFlow(): Flow<List<SleepRecord>> = sleepDao.getAllSleepRecordsFlow()

    suspend fun getRecordWithEvents(recordId: Long): SleepRecordWithEvents? =
        sleepDao.getRecordWithAllEvents(recordId)

    @Suppress("unused")
    suspend fun getSingleRecordById(id: Long): SleepRecord? = sleepDao.getSingleSleepRecord(id)

    suspend fun getLatestSleepRecord(): SleepRecord? = sleepDao.getLatestSleepRecord()

    // ------------------- 删除逻辑 -------------------
    private suspend fun deleteEventClipFiles(recordId: Long) {
        val eventList = sleepDao.getRecordWithAllEvents(recordId)?.events ?: return
        eventList.forEach { event ->
            if (event.clipPath.isBlank()) return@forEach
            val clipFile = File(event.clipPath)
            if (clipFile.exists()) {
                val deleted = clipFile.delete()
                Log.d(TAG, "delete clip file ${event.clipPath}, deleted=$deleted")
            }
        }
    }

    // 批量删除选中记录：先删片段文件，再删数据库
    suspend fun deleteSelectedRecords(idList: List<Long>) {
        idList.forEach { recordId ->
            deleteEventClipFiles(recordId)
        }
        sleepDao.batchDeleteRecords(idList)
    }

    // 清空全部记录：先删所有片段，再清空主表
    suspend fun clearAllSleepRecords() {
        val allRecords = getAllSleepRecordsFlow().firstOrNull() ?: emptyList()
        allRecords.forEach { record ->
            deleteEventClipFiles(record.id)
        }
        sleepDao.deleteAllSleepRecords()
    }
}
