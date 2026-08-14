package com.demo.project.repository.remote

import com.common.http.HttpUtils
import com.common.http.ResponseHolder
import com.squareup.moshi.Types
import com.common.http.Result
import com.demo.project.model.LoginData

class DataRepoImpl :DataRepo{
    val map = hashMapOf<String, String>()

    override suspend fun doLogin(
        userName: String,
        password: String
    ): ResponseHolder<LoginData> {
        map.clear()
        map["username"] = userName
        map["password"] = password

        return HttpUtils.getInstance().post(
            url = "user/login",
            params = map,
            type = Types.newParameterizedType(Result::class.java, LoginData::class.java)
        )
    }
}
