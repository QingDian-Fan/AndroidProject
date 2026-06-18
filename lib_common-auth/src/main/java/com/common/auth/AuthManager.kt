package com.common.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import com.common.auth.hook.LoginHookUtil
import com.common.auth.store.AuthStore
import com.common.auth.store.DataStoreAuthStore
import java.util.concurrent.CopyOnWriteArraySet

object AuthManager : AuthTokenProvider {
    private var config: AuthConfig = AuthConfig()
    private var store: AuthStore = DataStoreAuthStore()
    private val listeners = CopyOnWriteArraySet<AuthStateListener>()

    @JvmStatic
    @JvmOverloads
    fun init(config: AuthConfig = AuthConfig(), store: AuthStore = DataStoreAuthStore()) {
        this.config = config
        this.store = store
    }

    @JvmStatic
    fun getAuthConfig(): AuthConfig = config

    @JvmStatic
    fun hasCustomLoginChecker(): Boolean = config.loginChecker != null

    @JvmStatic
    fun isLogin(): Boolean {
        config.loginChecker?.let { checker ->
            return runCatching { checker.isLogin() }.getOrDefault(false)
        }

        val session = store.getSession() ?: return false
        return session.hasCredential() && !session.isExpired()
    }

    @JvmStatic
    fun saveLogin(session: AuthSession) {
        store.saveSession(session)
        listeners.forEach { it.onLogin(session) }
    }

    @JvmStatic
    fun saveSession(session: AuthSession) {
        saveLogin(session)
    }

    @JvmStatic
    fun getSession(): AuthSession? = store.getSession()

    @JvmStatic
    fun clearSession() {
        store.clearSession()
    }

    @JvmStatic
    @JvmOverloads
    fun logout(reason: LogoutReason = LogoutReason.USER) {
        store.clearSession()
        listeners.forEach { it.onLogout(reason) }
    }

    @JvmStatic
    fun getToken(): String? = getSession()?.token

    @JvmStatic
    fun getRefreshToken(): String? = getSession()?.refreshToken

    @JvmStatic
    fun getUserId(): String? = getSession()?.userId

    @JvmStatic
    fun getUserName(): String? = getSession()?.userName

    override fun getAccessToken(): String? = getToken()

    override fun getTokenHeaderName(): String = config.tokenHeaderName

    override fun getAuthorizationHeaderValue(): String? {
        val token = getToken()?.takeIf { it.isNotBlank() } ?: return null
        return config.tokenPrefix + token
    }

    @JvmStatic
    fun addAuthStateListener(listener: AuthStateListener) {
        listeners.add(listener)
    }

    @JvmStatic
    fun removeAuthStateListener(listener: AuthStateListener) {
        listeners.remove(listener)
    }

    @JvmStatic
    @JvmOverloads
    fun requireLogin(context: Context, targetIntent: Intent? = null): Boolean {
        if (isLogin()) return true

        val loginIntent = createLoginIntent(context, targetIntent) ?: return false
        context.startActivity(loginIntent)
        return false
    }

    @JvmStatic
    fun createLoginIntent(context: Context, targetIntent: Intent?): Intent? {
        config.loginIntentFactory?.create(context, targetIntent)?.let { intent ->
            return prepareLoginIntent(context, intent, targetIntent)
        }

        val loginActivity = config.loginActivity
            ?: LoginHookUtil.getLoginActivityClass(config.generatedHelperClassName)
            ?: return null
        val intent = Intent(context, loginActivity)
        return prepareLoginIntent(context, intent, targetIntent)
    }

    @JvmStatic
    @JvmOverloads
    fun completeLogin(activity: Activity, session: AuthSession, finishLoginPage: Boolean = true): Boolean {
        saveLogin(session)

        val targetIntent = getTargetIntent(activity.intent)
        if (targetIntent != null) {
            activity.startActivity(targetIntent)
        }

        if (finishLoginPage) {
            activity.finish()
        }
        return targetIntent != null
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun getTargetIntent(source: Intent?): Intent? {
        if (source == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            source.getParcelableExtra(config.redirectIntentExtraName, Intent::class.java)
        } else {
            source.getParcelableExtra(config.redirectIntentExtraName)
        }
    }

    private fun prepareLoginIntent(context: Context, intent: Intent, targetIntent: Intent?): Intent {
        if (targetIntent != null && !intent.hasExtra(config.redirectIntentExtraName)) {
            intent.putExtra(config.redirectIntentExtraName, targetIntent)
        }
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return intent
    }
}
