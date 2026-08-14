package com.demo.project.repository.remote

import com.common.aop.CheckNet
import com.common.http.HttpUtils
import com.common.http.ResponseHolder
import com.common.http.Result
import com.demo.project.model.LoginData
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow

class DataRepoImpl : DataRepo {

    companion object {
        private const val REGISTER_URL = "/user/register"
    }

    override suspend fun doLogin(
        userName: String,
        password: String
    ): ResponseHolder<LoginData> {
        val params = mapOf(
            "username" to userName,
            "password" to password
        )

        return HttpUtils.getInstance().post(
            url = "user/login",
            params = params,
            type = Types.newParameterizedType(Result::class.java, LoginData::class.java)
        )
    }

    override fun doRegister(
        userName: String,
        password: String,
        rePassword: String
    ): Flow<ResponseHolder<LoginData>> {
        val params = mapOf(
            "username" to userName,
            "password" to password,
            "repassword" to rePassword
        )
        val responseType = Types.newParameterizedType(Result::class.java, LoginData::class.java)
        return HttpUtils.getInstance().postFlow(
            url = REGISTER_URL,
            params = params,
            type = responseType
        )
    }
}
