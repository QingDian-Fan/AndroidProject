package com.demo.project.repository.remote

import com.common.http.HttpUtils
import com.common.http.ResponseHolder
import com.dian.annotation.RemoteRepository
import com.squareup.moshi.Types

@RemoteRepository(DataRepo::class)
class DataRepoImpl :DataRepo{
    val map = hashMapOf<String, String>()

    override suspend fun doLogin(
        userName: String,
        password: String
    ): ResponseHolder<Any> {
        map.clear()
        map["username"] = userName
        map["password"] = password

        return HttpUtils.getInstance().post(
            url = "user/login",
            params = map,
            type = Types.newParameterizedType(Result::class.java, Any::class.java)
        )
    }
}