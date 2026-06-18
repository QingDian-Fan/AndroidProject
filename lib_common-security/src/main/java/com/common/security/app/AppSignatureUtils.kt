package com.common.security.app

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.common.security.codec.HexCodec
import java.security.MessageDigest

object AppSignatureUtils {
    @JvmStatic
    @JvmOverloads
    fun getMd5(context: Context, packageName: String = context.packageName): List<String> {
        return getFingerprints(context, packageName, "MD5")
    }

    @JvmStatic
    @JvmOverloads
    fun getSha1(context: Context, packageName: String = context.packageName): List<String> {
        return getFingerprints(context, packageName, "SHA-1")
    }

    @JvmStatic
    @JvmOverloads
    fun getSha256(context: Context, packageName: String = context.packageName): List<String> {
        return getFingerprints(context, packageName, "SHA-256")
    }

    @JvmStatic
    @JvmOverloads
    fun verifySha256(
        context: Context,
        expectedFingerprint: String,
        packageName: String = context.packageName
    ): Boolean {
        val expected = expectedFingerprint.normalizeFingerprint()
        return getSha256(context, packageName).any { it.normalizeFingerprint() == expected }
    }

    @JvmStatic
    @JvmOverloads
    fun getFingerprints(
        context: Context,
        packageName: String = context.packageName,
        algorithm: String = "SHA-256"
    ): List<String> {
        return getSignatures(context, packageName).map { signature ->
            val digest = MessageDigest.getInstance(algorithm).digest(signature.toByteArray())
            HexCodec.fingerprint(digest)
        }
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    @JvmOverloads
    fun getSignatures(context: Context, packageName: String = context.packageName): List<Signature> {
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = packageInfo.signingInfo ?: return emptyList()
            val signatures = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
            signatures?.toList().orEmpty()
        } else {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            packageInfo.signatures?.toList().orEmpty()
        }
    }

    private fun String.normalizeFingerprint(): String {
        return replace(":", "").replace(" ", "").uppercase()
    }
}
