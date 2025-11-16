package com.example.lncrawlclient.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class DownloadRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var ctx: Context

    @Before
    fun setup() {
        server = MockWebServer()
        ctx = ApplicationProvider.getApplicationContext()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun testSingleStreamDownloadToTempFile() = runBlocking {
        // prepare server to serve a simple payload (no Accept-Ranges)
        server.enqueue(MockResponse().setResponseCode(200).setBody("Hello world"))
        server.start()

        val url = server.url("/file").toString()
        val repo = DownloadRepository(context = ctx)
        val tmp = File(ctx.cacheDir, "out_single.tmp")
        if (tmp.exists()) tmp.delete()

        // We will call internal method downloadToOutputStreamSaf via reflection for test simplicity
        // But here we test downloadPartWithRetries indirectly by calling downloadMultithreadedSaf with parts=1
        val fakeUri = Uri.parse("content://com.example/fake/output.bin")
        var progress = 0f
        try {
            repo.downloadMultithreadedSaf(url, fakeUri, parts = 1) { p -> progress = p }
        } catch (e: Exception) {
            // we expect failure because fakeUri isn't writable in test environment, but server should have responded
        }
        // ensure server received at least one request
        val req = server.takeRequest()
        assertTrue(req.path!!.contains("/file"))
    }

    @Test
    fun testRangeRequests() = runBlocking {
        val content = "0123456789ABCDEFGHIJ" // 20 bytes
        // dispatcher that supports range header
        val dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val range = request.getHeader("Range")
                if (range == null) {
                    return MockResponse().setResponseCode(200).setBody(content)
                } else {
                    // parse range like bytes=0-4
                    val m = Regex("bytes=(\\d+)-(\\d+)").find(range) ?: return MockResponse().setResponseCode(416)
                    val start = m.groupValues[1].toInt()
                    val end = m.groupValues[2].toInt()
                    val slice = content.substring(start, end + 1)
                    return MockResponse().setResponseCode(206).setBody(slice).addHeader("Content-Range", "bytes $start-$end/${content.length}").addHeader("Accept-Ranges", "bytes")
                }
            }
        }
        server.dispatcher = dispatcher
        server.start()

        val url = server.url("/range").toString()
        val repo = DownloadRepository(context = ctx)
        val tmpFolder = File(ctx.cacheDir, "test_parts")
        if (tmpFolder.exists()) tmpFolder.deleteRecursively()
        tmpFolder.mkdirs()

        // call downloadPartWithRetries via reflection? Instead call downloadMultithreadedSaf parts=2 with fake Uri
        val fakeUri = Uri.parse("content://com.example/fake/output.bin")
        try {
            repo.downloadMultithreadedSaf(url, fakeUri, parts = 2) { _ -> }
        } catch (e: Exception) {
            // expected due to fakeUri not writable. But server should have received requests for ranges
        }

        // verify that at least one range request occurred
        var sawRange = false
        for (i in 0 until 5) {
            val r = server.takeRequest(100)
            if (r == null) break
            if (r.getHeader("Range") != null) sawRange = true
        }
        assertTrue(sawRange)
    }
}
