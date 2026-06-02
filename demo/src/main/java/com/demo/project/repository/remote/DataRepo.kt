package com.demo.project.repository.remote

import com.common.http.ResponseHolder

interface DataRepo {
    suspend fun doLogin(userName:String,password:String): ResponseHolder<Any>
}