package com.tkksl.sleeptracker.data.repository

import com.tkksl.sleeptracker.data.local.SleepDao
import com.tkksl.sleeptracker.data.model.AudioEventEntity
import com.tkksl.sleeptracker.data.model.SleepRecord
import com.tkksl.sleeptracker.data.model.SleepRecordWithEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.io.File

class SleepRepository(private val sleepDao: SleepDao) {
    // 完整插入睡眠记录+声响事件，返回是否插入成功
    suspend fun insertFullSleepRecord(
        record: SleepRecord,
        eventList: List<AudioEventEntity>
    ): Boolean {
        return try {
            val recordId = sleepDao.insertSleepRecord(record)
            val eventsWithId = eventList.map { it.copy(sleepRecordId = recordId) }
            sleepDao.insertAudioEvents(eventsWithId)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getAllSleepRecordsFlow(): Flow<List<SleepRecord>> = sleepDao.getAllSleepRecordsFlow()

    suspend fun getRecordWithEvents(recordId: Long): SleepRecordWithEvents? =
        sleepDao.getRecordWithAllEvents(recordId)

    suspend fun getSingleRecordById(id: Long): SleepRecord? = sleepDao.getSingleSleepRecord(id)

    suspend fun getLatestSleepRecord(): SleepRecord? = sleepDao.getLatestSleepRecord()

    // ------------------- 删除逻辑 -------------------
    // 修复：加上 suspend 修饰符，内部可调用挂起函数
    private suspend fun deleteEventClipFiles(recordId: Long) {
        val eventList = sleepDao.getRecordWithAllEvents(recordId)?.events ?: return
        eventList.forEach { event ->
            if (event.clipPath.isBlank()) return@forEach
            val clipFile = File(event.clipPath)
            if (clipFile.exists()) clipFile.delete()
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