package com.common.share.temp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 分享临时文件会话的登记、清理、过期与路径安全测试。
 *
 * 这里测的是 [ShareTempFileStore]——真实分享链路（QQ / 微信 / 系统面板）调用的同一套判定，
 * 与 Android 相关的 Bitmap 落盘、FileProvider 授权部分由仪器测试覆盖。
 */
class ShareTempFileStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    /** 可推进的时间源，用于验证过期与安全读取期 */
    private class FakeTime(var now: Long = 1_000_000L) : ShareTempFileStore.TimeSource {
        override fun nowMillis(): Long = now
    }

    private fun newStore(
        ttl: Long = ShareTempFileStore.DEFAULT_SESSION_TTL_MILLIS,
        time: ShareTempFileStore.TimeSource? = null,
    ): ShareTempFileStore = ShareTempFileStore(folder.newFolder("share_temp"), ttl, time)

    // ---------- 创建与登记 ----------

    @Test
    fun createSession_createsIsolatedDirectoryAndRegistersFile() {
        val store = newStore()
        val session = store.createSession(1)
        assertNotNull(session)
        assertTrue(session.directory.isDirectory)
        assertEquals(ShareSessionState.CREATING, session.state)

        val file = store.createTempFile(session, "png")
        assertNotNull(file)
        assertTrue(file.exists())
        // 文件必须落在会话目录内，且已完成登记
        assertEquals(session.directory, file.parentFile)
        assertEquals(listOf(file), session.files)
    }

    @Test
    fun createTempFile_usesUnpredictableNameAndKeepsExtension() {
        val store = newStore()
        val session = store.createSession(1)
        val first = store.createTempFile(session, "png")
        val second = store.createTempFile(session, ".JPG")

        assertTrue(first.name.endsWith(".png"))
        assertTrue(second.name.endsWith(".jpg"))
        // 同一会话内多次创建不会互相覆盖
        assertFalse(first.name == second.name)
        assertEquals(2, session.files.size)
    }

    @Test
    fun createTempFile_registersEveryGeneratedFile() {
        val store = newStore()
        val session = store.createSession(1)
        val origin = store.createTempFile(session, "png")
        val compressed = store.createTempFile(session, "jpg")
        val thumb = store.createTempFile(session, "jpg")

        assertEquals(3, session.files.size)
        assertTrue(store.finishSession(session.sessionId))
        // 原图、压缩图、缩略图必须一起清理
        assertFalse(origin.exists())
        assertFalse(compressed.exists())
        assertFalse(thumb.exists())
    }

    // ---------- 成功 / 取消 / 失败清理 ----------

    @Test
    fun finishSession_deletesFilesAndEmptySessionDirectory() {
        val store = newStore()
        val session = store.createSession(1)
        val file = store.createTempFile(session, "png")

        assertTrue(store.finishSession(session.sessionId))
        assertFalse(file.exists())
        assertFalse(session.directory.exists())
        assertEquals(ShareSessionState.DELETED, session.state)
        assertEquals(0, store.activeSessionCount)
        // 专用根目录本身不能被删除
        assertTrue(store.root.isDirectory)
    }

    @Test
    fun finishSession_isIdempotent() {
        val store = newStore()
        val session = store.createSession(1)
        store.createTempFile(session, "png")

        // 成功、取消、失败、超时回调重复到达时只执行一次有效删除
        assertTrue(store.finishSession(session.sessionId))
        assertTrue(store.finishSession(session.sessionId))
        assertTrue(store.finishSession(session.sessionId))
        assertEquals(ShareSessionState.DELETED, session.state)
    }

    @Test
    fun finishSession_onUnknownIdIsNoOp() {
        val store = newStore()
        assertTrue(store.finishSession("not-a-session"))
        assertTrue(store.finishSession(null))
    }

    @Test
    fun finishSession_withMissingFileStillCompletes() {
        val store = newStore()
        val session = store.createSession(1)
        val file = store.createTempFile(session, "png")
        // 文件被外部提前删除（写入失败后已清理半成品）
        assertTrue(file.delete())

        assertTrue(store.finishSession(session.sessionId))
        assertFalse(session.directory.exists())
    }

    // ---------- 安全读取期 ----------

    @Test
    fun finishSessionIfSafe_keepsFileBeforeEarliestDeleteTime() {
        val time = FakeTime()
        val store = newStore(time = time)
        val session = store.createSession(1)
        val file = store.createTempFile(session, "png")

        store.markPendingSafeDelete(session.sessionId, 60_000L)
        assertEquals(ShareSessionState.PENDING_SAFE_DELETE, session.state)

        // 安全读取期内不得删除，避免接收方仍在异步读取时文件消失
        assertFalse(store.finishSessionIfSafe(session.sessionId))
        assertTrue(file.exists())

        time.now += 60_000L
        assertTrue(store.finishSessionIfSafe(session.sessionId))
        assertFalse(file.exists())
    }

    @Test
    fun markPendingSafeDelete_clampsDelayToUpperBound() {
        val time = FakeTime()
        val store = newStore(time = time)
        val session = store.createSession(1)

        val earliest = store.markPendingSafeDelete(session.sessionId, Long.MAX_VALUE)
        assertEquals(time.now + ShareTempFileStore.MAX_SAFE_DELETE_DELAY_MILLIS, earliest)
    }

    @Test
    fun markPendingSafeDelete_neverShortensExistingSafeWindow() {
        val time = FakeTime()
        val store = newStore(time = time)
        val session = store.createSession(1)

        val longer = store.markPendingSafeDelete(session.sessionId, 120_000L)
        val shorter = store.markPendingSafeDelete(session.sessionId, 1_000L)
        assertEquals(longer, shorter)
    }

    // ---------- 过期扫描 ----------

    @Test
    fun sweepExpired_removesExpiredSessionOnly() {
        val time = FakeTime()
        val store = newStore(ttl = 10_000L, time = time)
        val expired = store.createSession(1)
        val expiredFile = store.createTempFile(expired, "png")

        time.now += 11_000L
        val active = store.createSession(2)
        val activeFile = store.createTempFile(active, "png")

        store.sweepExpired()

        assertFalse(expiredFile.exists())
        // 未过期且仍活动的会话不得被扫描删除
        assertTrue(activeFile.exists())
        assertEquals(1, store.activeSessionCount)
    }

    @Test
    fun sweepExpired_removesOrphanDirectoryLeftByKilledProcess() {
        val time = FakeTime(now = System.currentTimeMillis())
        val store = newStore(ttl = 10_000L, time = time)
        // 模拟上次进程被杀留下的会话目录：内存中没有任何登记
        val orphanDir = File(store.root, "orphansession")
        assertTrue(orphanDir.mkdirs())
        val orphanFile = File(orphanDir, "leftover.png")
        orphanFile.writeText("x")
        assertTrue(orphanDir.setLastModified(time.now - 60_000L))
        assertTrue(orphanFile.setLastModified(time.now - 60_000L))

        time.now += 11_000L
        store.sweepExpired()

        assertFalse(orphanFile.exists())
        assertFalse(orphanDir.exists())
    }

    @Test
    fun sweepExpired_keepsRecentOrphanDirectory() {
        val time = FakeTime(now = System.currentTimeMillis())
        val store = newStore(ttl = 10 * 60 * 1000L, time = time)
        val recentDir = File(store.root, "recentsession")
        assertTrue(recentDir.mkdirs())
        val recentFile = File(recentDir, "fresh.png")
        recentFile.writeText("x")

        store.sweepExpired()

        // 刚写入的目录可能属于另一个正在进行的分享，不能删
        assertTrue(recentFile.exists())
    }

    @Test
    fun sweepExpired_onEmptyRootIsSafe() {
        val store = newStore()
        assertEquals(0, store.sweepExpired())
    }

    // ---------- 路径安全 ----------

    @Test
    fun deleteRegisteredFile_rejectsPathOutsideRoot() {
        val store = newStore()
        val outside = folder.newFile("user-original.png")
        outside.writeText("original")

        assertFalse(store.deleteRegisteredFile(outside))
        // 调用方传入的原始文件必须完好
        assertTrue(outside.exists())
    }

    @Test
    fun deleteRegisteredFile_rejectsPathTraversal() {
        val store = newStore()
        val outside = folder.newFile("gallery-saved.png")
        outside.writeText("saved to album")

        val traversal = File(store.root, "../" + outside.name)
        assertFalse(store.deleteRegisteredFile(traversal))
        assertTrue(outside.exists())
    }

    @Test
    fun deleteRegisteredFile_rejectsRootItself() {
        val store = newStore()
        assertFalse(store.deleteRegisteredFile(store.root))
        assertTrue(store.root.isDirectory)
    }

    @Test
    fun deleteRegisteredFile_rejectsNull() {
        val store = newStore()
        assertFalse(store.deleteRegisteredFile(null))
    }

    @Test
    fun isInsideRoot_acceptsOnlyManagedPaths() {
        val store = newStore()
        val session = store.createSession(1)
        val managed = store.createTempFile(session, "png")

        assertTrue(store.isInsideRoot(managed))
        assertFalse(store.isInsideRoot(folder.root))
        assertFalse(store.isInsideRoot(File(store.root.parentFile, "other_cache")))
    }

    // ---------- 永久图片与调用方文件保护 ----------

    @Test
    fun savedGalleryImageIsNeverRegisteredOrDeleted() {
        val time = FakeTime()
        val store = newStore(ttl = 10_000L, time = time)
        // 「保存相册」结果与调用方原图都在专用目录之外，不会被登记
        val gallery = folder.newFile("DCIM-share-result.jpg")
        gallery.writeText("permanent")
        val callerFile = folder.newFile("caller-provided.pdf")
        callerFile.writeText("business file")

        val session = store.createSession(1)
        store.createTempFile(session, "png")
        time.now += 11_000L
        store.sweepExpired()
        store.finishSession(session.sessionId)

        assertTrue(gallery.exists())
        assertTrue(callerFile.exists())
    }

    // ---------- 并发 ----------

    @Test
    fun concurrentSessions_areIsolated() {
        val store = newStore()
        val first = store.createSession(1)
        val firstFile = store.createTempFile(first, "png")
        val second = store.createSession(2)
        val secondFile = store.createTempFile(second, "png")

        assertFalse(first.sessionId == second.sessionId)
        assertTrue(store.finishSession(first.sessionId))

        assertFalse(firstFile.exists())
        // 另一个仍在使用的会话不受影响
        assertTrue(secondFile.exists())
        assertTrue(second.directory.isDirectory)
        assertEquals(1, store.activeSessionCount)
    }

    @Test
    fun parallelCreateAndFinish_leavesNoResidue() {
        val store = newStore()
        val count = 32
        val pool = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(count)
        try {
            repeat(count) {
                pool.execute {
                    try {
                        val session = store.createSession(1)
                        store.createTempFile(session, "png")
                        store.createTempFile(session, "jpg")
                        // 同一会话重复结束，验证并发下的幂等性
                        store.finishSession(session.sessionId)
                        store.finishSession(session.sessionId)
                    } finally {
                        latch.countDown()
                    }
                }
            }
            assertTrue(latch.await(30, TimeUnit.SECONDS))
        } finally {
            pool.shutdownNow()
        }

        assertEquals(0, store.activeSessionCount)
        assertEquals(0, store.root.listFiles()?.size ?: -1)
    }

    // ---------- 状态流转 ----------

    @Test
    fun sessionState_advancesAndFreezesAfterDeleted() {
        val store = newStore()
        val session = store.createSession(1)
        assertEquals(ShareSessionState.CREATING, session.state)

        store.markReady(session)
        assertEquals(ShareSessionState.READY, session.state)
        store.markLaunched(session)
        assertEquals(ShareSessionState.LAUNCHED, session.state)
        store.markWaitingResult(session)
        assertEquals(ShareSessionState.WAITING_RESULT, session.state)

        store.finishSession(session.sessionId)
        assertEquals(ShareSessionState.DELETED, session.state)

        // 已删除的会话不再接受迟到的状态变更
        store.markReady(session)
        assertEquals(ShareSessionState.DELETED, session.state)
    }

    @Test
    fun getSession_returnsNullAfterCleanup() {
        val store = newStore()
        val session = store.createSession(1)
        assertNotNull(store.getSession(session.sessionId))

        store.finishSession(session.sessionId)
        assertNull(store.getSession(session.sessionId))
    }

    @Test
    fun registerUri_isTrackedForPermissionRevoke() {
        val store = newStore()
        val session = store.createSession(1)
        store.registerUri(session, "content://com.demo.project.provider/cache/a.png")
        store.registerUri(session, "content://com.demo.project.provider/cache/a.png")

        // 重复登记同一 URI 只保留一份
        assertEquals(1, session.uris.size)
    }
}
