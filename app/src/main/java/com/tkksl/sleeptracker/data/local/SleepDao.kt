package com.tkksl.sleeptracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tkksl.sleeptracker.data.model.SleepRecord

@Dao
interface SleepDao {
    @Insert
    suspend fun insert(record: SleepRecord)

    @Query("SELECT * FROM sleep_record ORDER BY startTime DESC")
    suspend fun getAllRecords(): List<SleepRecord>

    @Query("SELECT * FROM sleep_record ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestRecord(): SleepRecord?

    // 根据id查询单条记录（详情页使用）
    @Query("SELECT * FROM sleep_record WHERE id = :id")
    suspend fun getSleepRecordById(id: Long): SleepRecord?

    @Query("DELETE FROM sleep_record")
    suspend fun deleteAll()

    // 批量删除传入id集合的记录
    @Query("DELETE FROM sleep_record WHERE id IN (:idList)")
    suspend fun deleteByIdList(idList: List<Long>)
}