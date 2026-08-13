package com.common.share.temp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 交给 QQ SDK 之前的图片自查测试。
 *
 * QQ 对本地路径执行 `new File(path)` + 存在性 / 大小检查，任何一项不满足都表现为
 * 「图片加载失败」且没有可区分的回调，所以必须在调起前把问题变成明确失败。
 */
class ShareImageValidatorTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val jpegHeader = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
        0x00, 0x10, 0x4A, 0x46
    )
    private val pngHeader = byteArrayOf(
        0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(),
        0x0D, 0x0A, 0x1A, 0x0A
    )

    private fun newStore(): ShareTempFileStore =
        ShareTempFileStore(folder.newFolder("share_temp"))

    private fun writeJpeg(store: ShareTempFileStore, session: ShareTempSession): File {
        val file = store.createTempFile(session, "jpg")
        file.writeBytes(jpegHeader + ByteArray(64))
        return file
    }

    // ---------- 扩展名与实际格式一致性 ----------

    @Test
    fun matchesExtension_acceptsRealJpeg() {
        assertTrue(ShareImageValidator.matchesExtension("jpg", jpegHeader))
        assertTrue(ShareImageValidator.matchesExtension("jpeg", jpegHeader))
        assertTrue(ShareImageValidator.matchesExtension("JPG", jpegHeader))
    }

    @Test
    fun matchesExtension_acceptsRealPng() {
        assertTrue(ShareImageValidator.matchesExtension("png", pngHeader))
    }

    @Test
    fun matchesExtension_rejectsPngBytesNamedJpg() {
        // 扩展名与 MIME 写成 JPEG 但内容其实是 PNG，QQ 解码会失败
        assertFalse(ShareImageValidator.matchesExtension("jpg", pngHeader))
        assertFalse(ShareImageValidator.matchesExtension("png", jpegHeader))
    }

    @Test
    fun matchesExtension_rejectsTruncatedOrUnknownInput() {
        assertFalse(ShareImageValidator.matchesExtension("jpg", byteArrayOf(0xFF.toByte())))
        assertFalse(ShareImageValidator.matchesExtension("png", ByteArray(0)))
        assertFalse(ShareImageValidator.matchesExtension("bmp", jpegHeader))
        assertFalse(ShareImageValidator.matchesExtension(null, jpegHeader))
        assertFalse(ShareImageValidator.matchesExtension("jpg", null))
    }

    @Test
    fun extensionOf_readsLastSegment() {
        assertEquals("jpg", ShareImageValidator.extensionOf("abc.JPG"))
        assertEquals("png", ShareImageValidator.extensionOf("a.b.png"))
        assertEquals("", ShareImageValidator.extensionOf("noextension"))
        assertEquals("", ShareImageValidator.extensionOf("trailingdot."))
        assertEquals("", ShareImageValidator.extensionOf(null))
    }

    // ---------- 本地路径 / 网络 URL 判别 ----------

    @Test
    fun isLocalFilePath_rejectsUriAndNetworkForms() {
        // SHARE_TO_QQ_IMAGE_LOCAL_URL 必须收到本地路径，不能是 content:// 或 http(s)://
        assertFalse(ShareImageValidator.isLocalFilePath("content://com.demo.project.provider/a.jpg"))
        assertFalse(ShareImageValidator.isLocalFilePath("http://example.com/a.jpg"))
        assertFalse(ShareImageValidator.isLocalFilePath("https://example.com/a.jpg"))
        assertFalse(ShareImageValidator.isLocalFilePath("file:///data/a.jpg"))
        assertFalse(ShareImageValidator.isLocalFilePath(null))
        assertFalse(ShareImageValidator.isLocalFilePath("  "))
    }

    @Test
    fun isLocalFilePath_acceptsAbsolutePath() {
        assertTrue(ShareImageValidator.isLocalFilePath("/data/user/0/pkg/cache/share_temp/a/b.jpg"))
    }

    // ---------- 完整校验 ----------

    @Test
    fun validate_acceptsRegisteredJpegInsideSession() {
        val store = newStore()
        val session = store.createSession(1)
        val file = writeJpeg(store, session)

        assertEquals(
            ShareImageValidator.Result.OK,
            ShareImageValidator.validate(store, session, file, ShareImageValidator.QQ_MAX_IMAGE_BYTES)
        )
    }

    @Test
    fun validate_rejectsMissingFile() {
        val store = newStore()
        val session = store.createSession(1)
        val file = writeJpeg(store, session)
        assertTrue(file.delete())

        assertEquals(
            ShareImageValidator.Result.MISSING,
            ShareImageValidator.validate(store, session, file, ShareImageValidator.QQ_MAX_IMAGE_BYTES)
        )
    }

    @Test
    fun validate_rejectsEmptyFile() {
        val store = newStore()
        val session = store.createSession(1)
        // 压缩返回失败时文件长度为 0，QQ 会显示加载失败
        val file = store.createTempFile(session, "jpg")

        assertEquals(
            ShareImageValidator.Result.EMPTY,
            ShareImageValidator.validate(store, session, file, ShareImageValidator.QQ_MAX_IMAGE_BYTES)
        )
    }

    @Test
    fun validate_rejectsDirectory() {
        val store = newStore()
        val session = store.createSession(1)

        assertEquals(
            ShareImageValidator.Result.NOT_A_FILE,
            ShareImageValidator.validate(store, session, session.directory, 0L)
        )
    }

    @Test
    fun validate_rejectsOversizedFile() {
        val store = newStore()
        val session = store.createSession(1)
        val file = writeJpeg(store, session)

        assertEquals(
            ShareImageValidator.Result.TOO_LARGE,
            ShareImageValidator.validate(store, session, file, 8L)
        )
    }

    @Test
    fun validate_skipsSizeLimitWhenNotConfigured() {
        val store = newStore()
        val session = store.createSession(1)
        val file = writeJpeg(store, session)

        assertEquals(
            ShareImageValidator.Result.OK,
            ShareImageValidator.validate(store, session, file, 0L)
        )
    }

    @Test
    fun validate_rejectsFileOutsideSessionDirectory() {
        val store = newStore()
        val session = store.createSession(1)
        // 用户原图 / 其他任务的文件，不属于本次会话，绝不能交给 QQ 也绝不能被清理
        val outside = folder.newFile("user-original.jpg")
        outside.writeBytes(jpegHeader + ByteArray(64))

        assertEquals(
            ShareImageValidator.Result.OUTSIDE_SESSION,
            ShareImageValidator.validate(store, session, outside, ShareImageValidator.QQ_MAX_IMAGE_BYTES)
        )
        assertTrue(outside.exists())
    }

    @Test
    fun validate_rejectsFileFromAnotherSession() {
        val store = newStore()
        val first = store.createSession(1)
        val second = store.createSession(2)
        val fileOfFirst = writeJpeg(store, first)

        // 在专用目录内，但不属于当前会话
        assertEquals(
            ShareImageValidator.Result.OUTSIDE_SESSION,
            ShareImageValidator.validate(store, second, fileOfFirst, ShareImageValidator.QQ_MAX_IMAGE_BYTES)
        )
    }

    @Test
    fun validate_rejectsExtensionFormatMismatch() {
        val store = newStore()
        val session = store.createSession(1)
        val file = store.createTempFile(session, "jpg")
        // 扩展名是 jpg，内容却是 PNG
        file.writeBytes(pngHeader + ByteArray(64))

        assertEquals(
            ShareImageValidator.Result.FORMAT_MISMATCH,
            ShareImageValidator.validate(store, session, file, ShareImageValidator.QQ_MAX_IMAGE_BYTES)
        )
    }

    @Test
    fun validate_rejectsNullArguments() {
        val store = newStore()
        val session = store.createSession(1)

        assertEquals(
            ShareImageValidator.Result.INVALID_ARGUMENT,
            ShareImageValidator.validate(null, session, folder.newFile("a.jpg"), 0L)
        )
        assertEquals(
            ShareImageValidator.Result.INVALID_ARGUMENT,
            ShareImageValidator.validate(store, null, folder.newFile("b.jpg"), 0L)
        )
        assertEquals(
            ShareImageValidator.Result.INVALID_ARGUMENT,
            ShareImageValidator.validate(store, session, null, 0L)
        )
    }

    @Test
    fun qqSizeLimit_isFiveMegabytes() {
        assertEquals(5L * 1024 * 1024, ShareImageValidator.QQ_MAX_IMAGE_BYTES)
    }
}
