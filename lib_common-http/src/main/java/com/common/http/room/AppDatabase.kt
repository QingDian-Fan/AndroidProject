package com.common.http.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DownloadTaskEntry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadTaskDao(): DownloadTaskDao
}