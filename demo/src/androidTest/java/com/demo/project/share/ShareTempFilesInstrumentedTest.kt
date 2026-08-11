package com.demo.project.share

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.common.share.temp.ShareSessionState
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
 * 分享临时图片的仪器测试：验证真实设备上的落盘目录、FileProvider URI 授权、
 * 安全读取期内文件仍可读，以及安全期结束后被删除。
 *
 * 需要连接设备或模拟器运行：`./gradlew :demo:connectedDebugAndroidTest`
 */
@RunWith(AndroidJUnit4::class)
class ShareTempFilesInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val createdFiles = mutableListOf<File>()

    @After
    fun tearDown() {
        // 测试自身产生的文件兜底清理，避免影响后续用例
        createdFiles.forEach { runCatching { it.delete() } }
        createdFiles.clear()
    }

    private fun newBitmap(): Bitmap =
        Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }

    @Test
    fun writeBitmap_landsInAppOwnedShareTempDirectory() {
        val bitmap = newBitmap()
        val session = ShareTempFiles.beginSession(context, CHANNEL_TEST, false)
        assertNotNull(session)
        val file = ShareTempFiles.writeBitmap(session, bitmap, Bitmap.CompressFormat.PNG, 100)
        assertNotNull(file)
        createdFiles.add(file)

        // 必须位于 cacheDir/share_temp/<sessionId>/ 之下，不在公共相册或外部存储根目录
        val expectedRoot = File(context.cacheDir, ShareTempFiles.DIR_NAME)
        assertTrue(file.canonicalPath.startsWith(expectedRoot.canonicalPath + File.separator))
        assertEquals(session.sessionId, file.parentFile?.name)
        assertTrue(file.name.endsWith(".png"))
        assertTrue(file.length() > 0)

        ShareTempFiles.finishNow(session.sessionId)
        bitmap.recycle()
    }

    @Test
    fun shareUri_isContentUriAndReadableBeforeCleanup() {
        val bitmap = newBitmap()
        val session = ShareTempFiles.beginSession(context, CHANNEL_TEST, false)
        val file = ShareTempFiles.writeBitmap(session, bitmap, Bitmap.CompressFormat.PNG, 100)
        assertNotNull(file)
        createdFiles.add(file)

        val uri = ShareTempFiles.shareUri(context, session, file)
        assertNotNull(uri)
        // 必须是 content:// 而不是 file://，否则 Android 7+ 会抛 FileUriExposedException
        assertEquals("content", uri.scheme)
        assertEquals("image/png", ShareTempFiles.mimeTypeOf(Bitmap.CompressFormat.PNG))

        // 调起后、接收方读取前，文件仍然存在且可通过 content URI 读到完整内容
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        assertNotNull(bytes)
        assertEquals(file.length(), bytes!!.size.toLong())

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = ShareTempFiles.mimeTypeOf(Bitmap.CompressFormat.PNG)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)

        ShareTempFiles.finishNow(session.sessionId)
        bitmap.recycle()
    }

    @Test
    fun finishAfterSafeDelay_keepsFileDuringSafeWindowThenDeletes() {
        val bitmap = newBitmap()
        val session = ShareTempFiles.beginSession(context, CHANNEL_TEST, false)
        val file = ShareTempFiles.writeBitmap(session, bitmap, Bitmap.CompressFormat.PNG, 100)
        assertNotNull(file)
        createdFiles.add(file)

        ShareTempFiles.finishAfterSafeDelay(session.sessionId, SAFE_DELAY_MILLIS)
        assertEquals(ShareSessionState.PENDING_SAFE_DELETE, session.state)

        // 安全读取期内文件必须保持存在，避免接收方仍在异步读取时文件消失
        Thread.sleep(SAFE_DELAY_MILLIS / 2)
        assertTrue(file.exists())

        // 安全期结束后由后台清理线程删除
        val deleted = awaitDeleted(file, SAFE_DELAY_MILLIS * 4)
        assertTrue("temp file should be deleted after the safe window", deleted)
        assertFalse(session.directory.exists())
        bitmap.recycle()
    }

    @Test
    fun finishNow_isIdempotentAndKeepsOtherSession() {
        val bitmap = newBitmap()
        val first = ShareTempFiles.beginSession(context, CHANNEL_TEST, false)
        val firstFile = ShareTempFiles.writeBitmap(first, bitmap, Bitmap.CompressFormat.JPEG, 90)
        val second = ShareTempFiles.beginSession(context, CHANNEL_TEST, false)
        val secondFile = ShareTempFiles.writeBitmap(second, bitmap, Bitmap.CompressFormat.JPEG, 90)
        assertNotNull(firstFile)
        assertNotNull(secondFile)
        createdFiles.add(firstFile)
        createdFiles.add(secondFile)

        assertTrue(firstFile.name.endsWith(".jpg"))

        ShareTempFiles.finishNow(first.sessionId)
        ShareTempFiles.finishNow(first.sessionId)
        assertTrue(awaitDeleted(firstFile, 5_000L))

        // 并发的另一个会话不受影响
        assertTrue(secondFile.exists())
        ShareTempFiles.finishNow(second.sessionId)
        bitmap.recycle()
    }

    private fun awaitDeleted(file: File, timeoutMillis: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (!file.exists()) {
                return true
            }
            Thread.sleep(100L)
        }
        return !file.exists()
    }

    private companion object {
        /** 仅作为会话渠道标记，不对应任何真实三方渠道 */
        const val CHANNEL_TEST = -1
        const val SAFE_DELAY_MILLIS = 2_000L
    }
}
