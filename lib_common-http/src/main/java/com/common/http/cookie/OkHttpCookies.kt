package com.common.http.cookie

import okhttp3.Cookie
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable


class OkHttpCookies : Serializable {

    private lateinit var cookies: Cookie
    private var clientCookies: Cookie? = null

    private constructor()

    constructor(cookies: Cookie?) {
        this.cookies = requireNotNull(cookies) { "cookie == null" }
    }

    fun getCookies(): Cookie {
        return clientCookies ?: cookies
    }

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.writeObject(cookies.name)
        out.writeObject(cookies.value)
        out.writeLong(cookies.expiresAt)
        out.writeObject(cookies.domain)
        out.writeObject(cookies.path)
        out.writeBoolean(cookies.secure)
        out.writeBoolean(cookies.httpOnly)
        out.writeBoolean(cookies.hostOnly)
        out.writeBoolean(cookies.persistent)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(ois: ObjectInputStream) {
        val name = ois.readObject() as String
        val value = ois.readObject() as String
        val expiresAt = ois.readLong()
        val domain = ois.readObject() as String
        val path = ois.readObject() as String
        val secure = ois.readBoolean()
        val httpOnly = ois.readBoolean()
        val hostOnly = ois.readBoolean()
        var builder = Cookie.Builder()
        builder = builder.name(name)
        builder = builder.value(value)
        builder = builder.expiresAt(expiresAt)
        builder = if (hostOnly) builder.hostOnlyDomain(domain) else builder.domain(domain)
        builder = builder.path(path)
        builder = if (secure) builder.secure() else builder
        builder = if (httpOnly) builder.httpOnly() else builder
        clientCookies = builder.build()
    }
}
