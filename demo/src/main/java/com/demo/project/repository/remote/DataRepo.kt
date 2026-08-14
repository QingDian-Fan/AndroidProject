package com.demo.project.repository.remote

import com.common.http.ResponseHolder
import com.demo.project.model.LoginData
import kotlinx.coroutines.flow.Flow

interface DataRepo {

    suspend fun doLogin(userName: String, password: String): ResponseHolder<LoginData>


    fun doRegister(userName: String, password: String, rePassword: String): Flow<ResponseHolder<LoginData>>
}
