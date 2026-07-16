package com.tkksl.sleeptracker.data.repository

import com.tkksl.sleeptracker.data.local.SleepDao
import com.tkksl.sleeptracker.data.model.SleepRecord

class SleepRepository(private val sleepDao: SleepDao) {
    suspend fun insertSleepRecord(record: SleepRecord) = sleepDao.insert(record)
    suspend fun getAllSleepRecords(): List<SleepRecord> = sleepDao.getAllRecords()
    suspend fun getLatestSleepRecord(): SleepRecord? = sleepDao.getLatestRecord()
    suspend fun clearAllSleepRecords() = sleepDao.deleteAll()

    // 新增批量删除
    suspend fun deleteSelectedRecords(idList: List<Long>) = sleepDao.deleteByIdList(idList)

    suspend fun getRecordById(id: Long): SleepRecord? { return sleepDao.getSleepRecordById(id) }
}