package com.tkksl.sleeptracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.tkksl.sleeptracker.data.model.AudioEventEntity
import com.tkksl.sleeptracker.data.model.SleepRecord
import com.tkksl.sleeptracker.data.model.SleepRecordWithEvents
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    /**
     * 插入单条睡眠主记录，返回自增主键id（用于后续绑定子事件）
     */
    @Insert
    suspend fun insertSleepRecord(record: SleepRecord): Long

    /**
     * 批量插入多条声响子事件
     */
    @Insert
    suspend fun insertAudioEvents(events: List<AudioEventEntity>)

    /**
     * 根据记录ID，一次性查询【睡眠记录+全部关联声响】（详情页专用）
     * Transaction保证主表子表查询原子性
     */
    @Transaction
    @Query("SELECT * FROM sleep_records WHERE id = :recordId")
    suspend fun getRecordWithAllEvents(recordId: Long): SleepRecordWithEvents?

    /**
     * 历史列表仅加载基础睡眠元数据，不携带事件，提升列表滑动性能
     * 倒序：最新记录在最上方
     */
    @Query("SELECT * FROM sleep_records ORDER BY startTime DESC")
    fun getAllSleepRecordsFlow(): Flow<List<SleepRecord>>

    /**
     * 根据ID仅查询单条睡眠主记录（不带事件）
     */
    @Query("SELECT * FROM sleep_records WHERE id = :recordId")
    suspend fun getSingleSleepRecord(recordId: Long): SleepRecord?

    /**
     * 获取最新一条睡眠记录（首页展示）
     */
    @Query("SELECT * FROM sleep_records ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSleepRecord(): SleepRecord?

    /**
     * 批量删除多条睡眠记录
     * 外键配置CASCADE，会自动同步删除对应audio_events子表数据，无需手动操作事件
     */
    @Query("DELETE FROM sleep_records WHERE id IN (:idList)")
    suspend fun batchDeleteRecords(idList: List<Long>)

    /**
     * 清空全部睡眠记录，自动级联清空所有声响事件
     */
    @Query("DELETE FROM sleep_records")
    suspend fun deleteAllSleepRecords()
}