package com.common.share.temp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FileProvider authority 生成与一致性校验测试。
 *
 * QQ OpenSDK 初始化、Manifest `${applicationId}.provider` 与
 * `FileProvider.getUriForFile()` 必须使用同一个 authority，否则 QQ 侧表现为「图片加载失败」。
 * 这里覆盖 release、debug（applicationIdSuffix）和渠道包三种运行时包名。
 */
class ShareFileProvidersTest {

    @Test
    fun authorityOf_releaseApplicationId() {
        assertEquals(
            "com.demo.project.provider",
            ShareFileProviders.authorityOf("com.demo.project")
        )
    }

    @Test
    fun authorityOf_debugApplicationIdSuffix() {
        // demo 的 debug 变体配置了 applicationIdSuffix '.debug'
        assertEquals(
            "com.demo.project.debug.provider",
            ShareFileProviders.authorityOf("com.demo.project.debug")
        )
    }

    @Test
    fun authorityOf_channelApplicationId() {
        assertEquals(
            "com.demo.project.huawei.provider",
            ShareFileProviders.authorityOf("com.demo.project.huawei")
        )
    }

    @Test
    fun authorityOf_trimsWhitespace() {
        assertEquals(
            "com.demo.project.provider",
            ShareFileProviders.authorityOf("  com.demo.project  ")
        )
    }

    @Test
    fun authorityOf_returnsNullForMissingPackageName() {
        // authority 拿不到时调用方必须中止分享，而不是拼出 ".provider"
        assertNull(ShareFileProviders.authorityOf(null as String?))
        assertNull(ShareFileProviders.authorityOf(""))
        assertNull(ShareFileProviders.authorityOf("   "))
    }

    @Test
    fun authorityOf_neverHardcodesReleasePackageForDebugBuild() {
        val debugAuthority = ShareFileProviders.authorityOf("com.demo.project.debug")
        val releaseAuthority = ShareFileProviders.authorityOf("com.demo.project")
        assertFalse(debugAuthority == releaseAuthority)
    }

    @Test
    fun matchesApplicationId_acceptsGeneratedAuthority() {
        listOf("com.demo.project", "com.demo.project.debug", "com.demo.project.huawei")
            .forEach { packageName ->
                val authority = ShareFileProviders.authorityOf(packageName)
                assertTrue(
                    "authority=$authority should match packageName=$packageName",
                    ShareFileProviders.matchesApplicationId(authority, packageName)
                )
            }
    }

    @Test
    fun matchesApplicationId_rejectsCrossVariantAuthority() {
        // 把 release authority 硬编码进 debug 包是最常见的失败方式
        assertFalse(
            ShareFileProviders.matchesApplicationId(
                "com.demo.project.provider",
                "com.demo.project.debug"
            )
        )
        assertFalse(
            ShareFileProviders.matchesApplicationId(
                "com.demo.project.debug.provider",
                "com.demo.project"
            )
        )
    }

    @Test
    fun matchesApplicationId_rejectsWrongSuffix() {
        assertFalse(
            ShareFileProviders.matchesApplicationId(
                "com.demo.project.fileprovider",
                "com.demo.project"
            )
        )
        assertFalse(ShareFileProviders.matchesApplicationId(null, "com.demo.project"))
        assertFalse(ShareFileProviders.matchesApplicationId("com.demo.project.provider", null))
    }

    @Test
    fun authoritySuffix_matchesManifestDeclaration() {
        // Manifest: android:authorities="${applicationId}.provider"
        assertEquals(".provider", ShareFileProviders.AUTHORITY_SUFFIX)
    }
}
