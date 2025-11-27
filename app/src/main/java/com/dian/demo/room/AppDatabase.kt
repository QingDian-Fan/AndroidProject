package com.dian.demo.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DownloadTaskEntry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadProgressDao(): DownloadTaskDao
}