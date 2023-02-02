package com.dian.demo.di.repository.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dian.demo.di.model.ArticleEntity

@Dao
interface ArticleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertArticleEntities(articleEntities: List<ArticleEntity>)


    @Query("DELETE FROM articleentity")
    fun deleteAllArticleEntities()

}