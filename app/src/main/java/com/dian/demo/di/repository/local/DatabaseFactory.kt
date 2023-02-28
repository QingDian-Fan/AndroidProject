package com.dian.demo.di.repository.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dian.demo.di.model.ArticleEntity



@Database(entities = [ArticleEntity::class], version = 1, exportSchema = false)
abstract class DatabaseFactory: RoomDatabase() {

    companion object{
        @Volatile
        private var dataBase: DatabaseFactory? = null

        fun getInstance(mContext: Context): DatabaseFactory {
            if (dataBase == null) {
                synchronized(DatabaseFactory::class.java) {
                    if (dataBase == null) {
                        dataBase = Room.databaseBuilder(mContext.applicationContext, DatabaseFactory::class.java, "db_article_entity")
                            .build()
                    }
                }
            }
            return dataBase!!
        }
    }


    abstract fun getArticle(): ArticleDao

}