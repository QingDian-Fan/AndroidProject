package com.dian.demo.room

import androidx.room.Entity

@Entity(tableName = "download_task", primaryKeys = ["taskId", "threadId"])
data class DownloadTaskEntry( val taskId: String,
                              val threadId: Int,
                              val startPos: Long,
                              val endPos: Long,
                              var downloaded: Long)