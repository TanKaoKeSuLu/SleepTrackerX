package com.tkksl.sleeptracker.data.repository

import com.tkksl.sleeptracker.data.local.SleepDao
import com.tkksl.sleeptracker.data.model.AudioEventEntity
import com.tkksl.sleeptracker.data.model.SleepRecord
import com.tkksl.sleeptracker.data.model.SleepRecordWithEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.io.File

class SleepRepository(private val sleepDao: SleepDao) {
    // 1. 完整插入一条睡眠记录+对应全部声响事件
    suspend fun insertFullSleepRecord(record: SleepRecord, eventList: List<AudioEventEntity>) {
        // 第一步：插入主记录，获取自增ID
        val recordId = sleepDao.insertSleepRecord(record)
        // 第二步：给所有事件绑定睡眠ID
        val eventsWithId = eventList.map { it.copy(sleepRecordId = recordId) }
        // 第三步：批量插入子事件
        sleepDao.insertAudioEvents(eventsWithId)
    }

    // 2. 查询全部基础记录（历史列表，不带声响）
    fun getAllSleepRecordsFlow(): Flow<List<SleepRecord>> = sleepDao.getAllSleepRecordsFlow()

    // 3. 查询单条完整记录（含所有声响，仅返回原生Room实体包装，不做转换）
    suspend fun getRecordWithEvents(recordId: Long): SleepRecordWithEvents? = sleepDao.getRecordWithAllEvents(recordId)

    // 4. 仅查询基础记录（不带事件）
    suspend fun getSingleRecordById(id: Long): SleepRecord? = sleepDao.getSingleSleepRecord(id)

    // 5. 获取最新单条记录（首页）
    suspend fun getLatestSleepRecord(): SleepRecord? = sleepDao.getLatestSleepRecord()

    // ------------------- 删除逻辑（自动级联子表事件，无需手动处理事件） -------------------
    /** 删除单条记录对应的pcm/wav音频文件 */
    private fun deleteAudioFiles(record: SleepRecord) {
        val audioPath = record.audioPath ?: return
        val wavFile = File(audioPath)
        val pcmPath = audioPath.replace(".wav", ".pcm")
        val pcmFile = File(pcmPath)

        if (wavFile.exists()) wavFile.delete()
        if (pcmFile.exists()) pcmFile.delete()
    }

    // 批量删除选中记录：先删本地音频，再删数据库主表（子表自动级联清除）
    suspend fun deleteSelectedRecords(idList: List<Long>) {
        val deleteRecords = idList.mapNotNull { getSingleRecordById(it) }
        deleteRecords.forEach { deleteAudioFiles(it) }
        sleepDao.batchDeleteRecords(idList)
    }

    // 清空全部记录：删除所有音频文件，再清空主表（自动清空所有声响事件）
    suspend fun clearAllSleepRecords() {
        val allRecords = getAllSleepRecordsFlow().firstOrNull() ?: emptyList()
        allRecords.forEach { deleteAudioFiles(it) }
        sleepDao.deleteAllSleepRecords()
    }
}