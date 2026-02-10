package com.common.http.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_task WHERE taskId = :taskId")
    suspend fun getTask(taskId: String): List<DownloadTaskEntry>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<DownloadTaskEntry>)

    @Update
    suspend fun update(info: DownloadTaskEntry)

    @Query("DELETE FROM download_task WHERE taskId = :taskId")
    suspend fun deleteTask(taskId: String)
}