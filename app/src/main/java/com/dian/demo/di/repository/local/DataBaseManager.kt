package com.dian.demo.di.repository.local

import android.content.Context
import com.dian.demo.ProjectApplication
import com.dian.demo.di.model.ArticleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DataBaseManager{

    suspend fun insertArticleEntities(articleEntities: List<ArticleEntity>) {
       withContext(Dispatchers.IO){
            DatabaseFactory.getInstance(ProjectApplication.getAppContext()).getArticle().insertArticleEntities(articleEntities)
        }
    }

    suspend fun deleteArticleEntities() {
        withContext(Dispatchers.IO){
            DatabaseFactory.getInstance(ProjectApplication.getAppContext()).getArticle().deleteAllArticleEntities()
        }
    }

}