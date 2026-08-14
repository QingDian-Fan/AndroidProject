package com.demo.project.repository.remote

import com.common.http.ResponseHolder
import com.demo.project.model.LoginData

interface DataRepo {
    suspend fun doLogin(userName:String,password:String): ResponseHolder<LoginData>
}