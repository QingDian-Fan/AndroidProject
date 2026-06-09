package com.common.auth

import android.content.Context
import android.content.Intent

fun interface LoginChecker {
    fun isLogin(): Boolean
}

fun interface LoginIntentFactory {
    fun create(context: Context, targetIntent: Intent?): Intent
}

data class AuthConfig @JvmOverloads constructor(
    val loginActivity: Class<*>? = null,
    val loginIntentFactory: LoginIntentFactory? = null,
    val loginChecker: LoginChecker? = null,
    val tokenHeaderName: String = AuthConstants.DEFAULT_TOKEN_HEADER_NAME,
    val tokenPrefix: String = AuthConstants.DEFAULT_TOKEN_PREFIX,
    val generatedHelperClassName: String = AuthConstants.DEFAULT_GENERATED_HELPER,
    val redirectIntentExtraName: String = AuthConstants.EXTRA_TARGET_INTENT
)
