package com.demo.project.share

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.common.share.channel.Channel
import com.common.share.temp.ShareFileProviders
import com.common.share.temp.ShareImageValidator
import com.common.share.temp.ShareTempFiles
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * QQ 分享图片的 FileProvider 授权链路仪器测试。
 *
 * 验证运行时 authority 与 Manifest 合并结果一致、临时图片能生成 `content://` URI
 * 并被完整读取、以及授权只针对本次 URI。
 *
 * 需要连接设备或模拟器运行：`./gradlew :demo:connectedDebugAndroidTest`
 */
@RunWith(AndroidJUnit4::class)
class QqShareFileProviderInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val createdSessions = mutableListOf<String>()

    @After
    fun tearDown() {
        createdSessions.forEach { ShareTempFiles.finishNow(it) }
        createdSessions.clear()
    }

    private fun newBitmap(): Bitmap =
        Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }

    /** Manifest 合并后实际注册的 FileProvider authority */
    private fun manifestAuthority(): String? {
        val providers = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS)
            .providers
            ?: return null
        return providers.firstOrNull { it: ProviderInfo ->
            it.name == "androidx.core.content.FileProvider"
        }?.authority
    }

    @Test
    fun runtimeAuthority_matchesMergedManifestAuthority() {
        val runtimeAuthority = ShareFileProviders.authorityOf(context)
        assertNotNull(runtimeAuthority)
        // QQ SDK 初始化、Manifest 与 getUriForFile 必须是同一个 authority
        assertEquals(manifestAuthority(), runtimeAuthority)
        assertTrue(
            ShareFileProviders.matchesApplicationId(runtimeAuthority, context.packageName)
        )
    }

    @Test
    fun fileProvider_isNotExportedAndGrantsUriPermissions() {
        val provider = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS)
            .providers
            ?.firstOrNull { it.name == "androidx.core.content.FileProvider" }
        assertNotNull(provider)
        assertFalse("provider 必须保持 exported=false", provider!!.exported)
        assertTrue("provider 必须开启 grantUriPermissions", provider.grantUriPermissions)
    }

    @Test
    fun qqTempImage_generatesReadableContentUri() {
        val bitmap = newBitmap()
        // QQ 只认本地路径，会话使用外部缓存目录
        val session = ShareTempFiles.beginSession(context, Channel.QQ, true)
        assertNotNull(session)
        createdSessions.add(session.sessionId)

        val file = ShareTempFiles.writeBitmap(session, bitmap, Bitmap.CompressFormat.JPEG, 90)
        assertNotNull(file)

        // 校验必须通过，否则 QQ 侧会直接显示图片加载失败
        assertEquals(
            ShareImageValidator.Result.OK,
            ShareTempFiles.validateImage(session, file, ShareImageValidator.QQ_MAX_IMAGE_BYTES)
        )

        // file_provider_paths.xml 必须覆盖 externalCacheDir/share_temp，
        // 否则这里会抛 IllegalArgumentException: Failed to find configured root
        val uri = ShareTempFiles.shareUri(context, session, file)
        assertNotNull("share_temp 未被 file_provider_paths.xml 覆盖", uri)
        assertEquals("content", uri.scheme)
        assertEquals(ShareFileProviders.authorityOf(context), uri.authority)

        // 接收端能通过 URI 读到完整图片字节
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        assertNotNull(bytes)
        assertEquals(file.length(), bytes!!.size.toLong())
        assertEquals(0xFF, bytes[0].toInt() and 0xFF)
        assertEquals(0xD8, bytes[1].toInt() and 0xFF)

        assertTrue(ShareTempFiles.grantRead(context, uri, Channel.PACKAGE_QQ))
        bitmap.recycle()
    }

    @Test
    fun internalCacheTempImage_alsoResolvesThroughProvider() {
        val bitmap = newBitmap()
        val session = ShareTempFiles.beginSession(context, Channel.QQ, false)
        assertNotNull(session)
        createdSessions.add(session.sessionId)

        val file = ShareTempFiles.writeBitmap(session, bitmap, Bitmap.CompressFormat.PNG, 100)
        assertNotNull(file)
        assertTrue(
            file.canonicalPath.startsWith(
                File(context.cacheDir, ShareTempFiles.DIR_NAME).canonicalPath + File.separator
            )
        )

        val uri = ShareTempFiles.shareUri(context, session, file)
        assertNotNull("cacheDir/share_temp 未被 file_provider_paths.xml 覆盖", uri)
        assertEquals("content", uri.scheme)
        bitmap.recycle()
    }

    @Test
    fun qqSdkCopiedImage_resolvesThroughProvider() {
        val bitmap = newBitmap()
        val externalFilesDir = context.getExternalFilesDir(null)
        assertNotNull(externalFilesDir)
        val sdkImageDir = File(externalFilesDir, "Images/tmp")
        assertTrue(sdkImageDir.mkdirs() || sdkImageDir.isDirectory)
        val imageFile = File(sdkImageDir, "provider_test.jpg")

        try {
            imageFile.outputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it))
            }
            val uri = FileProvider.getUriForFile(
                context,
                ShareFileProviders.authorityOf(context)!!,
                imageFile
            )
            assertEquals("content", uri.scheme)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            assertNotNull(bytes)
            assertTrue(bytes!!.isNotEmpty())
        } finally {
            imageFile.delete()
            bitmap.recycle()
        }
    }

    @Test
    fun shareIntent_carriesReadPermissionFlagOnly() {
        val bitmap = newBitmap()
        val session = ShareTempFiles.beginSession(context, Channel.QQ, true)
        createdSessions.add(session.sessionId)
        val file = ShareTempFiles.writeBitmap(session, bitmap, Bitmap.CompressFormat.JPEG, 90)
        val uri = ShareTempFiles.shareUri(context, session, file)
        assertNotNull(uri)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = ShareTempFiles.mimeTypeOf(Bitmap.CompressFormat.JPEG)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        // 不得授予写权限
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        bitmap.recycle()
    }
}
