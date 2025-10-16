package com.dian.demo.di.model

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val article_id: Long = 0,
    val id: String?,
    val link: String?,
    val title: String?,
    val superChapterName: String?
)
