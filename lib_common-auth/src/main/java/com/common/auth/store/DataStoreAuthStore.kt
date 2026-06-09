package com.common.auth.store

import com.common.auth.AuthSession
import com.common.utils.datastore.AppDataStore
import org.json.JSONObject

class DataStoreAuthStore : AuthStore {
    override fun saveSession(session: AuthSession) {
        AppDataStore.putData(KEY_AUTH_SESSION, session.toJsonString())
    }

    override fun getSession(): AuthSession? {
        val raw = AppDataStore.getData(KEY_AUTH_SESSION, "")
        if (raw.isBlank()) return null
        return runCatching { raw.toAuthSession() }.getOrNull()
    }

    override fun clearSession() {
        AppDataStore.clearKey(KEY_AUTH_SESSION)
    }

    private fun AuthSession.toJsonString(): String {
        val extrasJson = JSONObject()
        extras.forEach { (key, value) -> extrasJson.put(key, value) }

        return JSONObject()
            .put("token", token)
            .put("refreshToken", refreshToken)
            .put("userId", userId)
            .put("userName", userName)
            .put("cookie", cookie)
            .put("expireAt", expireAt)
            .put("extras", extrasJson)
            .toString()
    }

    private fun String.toAuthSession(): AuthSession {
        val json = JSONObject(this)
        val extrasJson = json.optJSONObject("extras")
        val extras = buildMap {
            if (extrasJson != null) {
                val keys = extrasJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, extrasJson.optString(key))
                }
            }
        }

        return AuthSession(
            token = json.optNullableString("token"),
            refreshToken = json.optNullableString("refreshToken"),
            userId = json.optNullableString("userId"),
            userName = json.optNullableString("userName"),
            cookie = json.optNullableString("cookie"),
            expireAt = json.optLong("expireAt", 0L),
            extras = extras
        )
    }

    private fun JSONObject.optNullableString(name: String): String? {
        if (isNull(name)) return null
        return optString(name).takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val KEY_AUTH_SESSION = "common_auth_session"
    }
}
