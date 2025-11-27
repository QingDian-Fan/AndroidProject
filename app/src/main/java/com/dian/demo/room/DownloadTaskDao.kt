package com.dian.demo.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_progress WHERE taskId = :taskId")
    suspend fun getTaskProgress(taskId: String): List<DownloadTaskEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: DownloadTaskEntry)

    @Query("DELETE FROM download_progress WHERE taskId = :taskId")
    suspend fun clearTask(taskId: String)
}