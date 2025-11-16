package com.example.lncrawlclient.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.*
import okhttp3.*
import java.io.File
import java.io.IOException
import kotlin.math.min
import kotlin.math.pow

/**
 * Production-ready DownloadRepository
 *
 * Features:
 * - Proper coroutine-scope usage (suspending functions, cooperative cancellation via isActive)
 * - Retries with exponential backoff for failed parts (configurable attempts)
 * - Per-byte progress reporting (more granular)
 * - Supports SAF (destUri) by downloading parts to temporary files then merging into target DocumentFile stream
 * - Cleans up temp files on failure or cancellation
 */
class DownloadRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(java.time.Duration.ofMinutes(10))
        .build(),
    private val context: Context,
    private val maxRetries: Int = 3,
    private val baseRetryDelayMs: Long = 300L
) {

    suspend fun downloadMultithreadedSaf(
        url: String,
        destUri: Uri?,
        parts: Int = 4,
        progressCb: (Float) -> Unit = {}
    ) {
        require(destUri != null) { "destUri required for SAF download" }
        coroutineScope {
            val cacheDir = File(context.cacheDir, "dl_parts")
            if (cacheDir.exists()) cacheDir.deleteRecursively()
            cacheDir.mkdirs()

            val headReq = Request.Builder().url(url).head().build()
            val headResp = safeExecute(headReq) ?: throw IOException("HEAD request failed")
            val length = headResp.header("Content-Length")?.toLongOrNull() ?: -1L
            val supportsRanges = headResp.header("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true

            try {
                if (length <= 0 || !supportsRanges) {
                    // fallback: single-stream to SAF
                    downloadToOutputStreamSaf(url, destUri, progressCb)
                    return@coroutineScope
                }

                val chunkSize = length / parts
                val bytesDownloaded = LongArray(parts) { 0L }
                val partFiles = Array(parts) { i -> File(cacheDir, "part_$i.tmp") }

                // launch workers
                val jobs = (0 until parts).map { partIndex ->
                    async(Dispatchers.IO) {
                        val start = partIndex * chunkSize
                        val end = if (partIndex == parts - 1) length - 1 else ((partIndex + 1) * chunkSize - 1)
                        downloadPartWithRetries(url, start, end, partFiles[partIndex]) { bytesForPart ->
                            bytesDownloaded[partIndex] = bytesForPart
                            val totalDownloaded = bytesDownloaded.sum()
                            val frac = if (length > 0) totalDownloaded.toFloat() / length.toFloat() else 0f
                            progressCb(min(1.0f, frac))
                        }
                    }
                }

                // await completion, propagate cancellation if any
                jobs.awaitAll()

                // merge parts into SAF output
                val resolver: ContentResolver = context.contentResolver
                val parent = DocumentFile.fromTreeUri(context, Uri.parse(Prefs.outputUri ?: ""))
                val fileName = DocumentFile.fromSingleUri(context, destUri)?.name ?: "download.bin"
                val finalDoc = parent?.createFile("application/octet-stream", fileName) ?: DocumentFile.fromSingleUri(context, destUri)
                resolver.openOutputStream(finalDoc!!.uri, "w")?.use { outStream ->
                    for (i in 0 until parts) {
                        partFiles[i].inputStream().use { input ->
                            input.copyTo(outStream)
                        }
                    }
                    outStream.flush()
                }

                // cleanup
                cacheDir.deleteRecursively()
                progressCb(1.0f)
            } catch (e: CancellationException) {
                // cancelled by caller, cleanup partial files
                cacheDir.deleteRecursively()
                throw e
            } catch (e: Exception) {
                cacheDir.deleteRecursively()
                throw e
            }
        }
    }

    private suspend fun downloadPartWithRetries(
        url: String,
        start: Long,
        end: Long,
        outFile: File,
        progressForPart: (Long) -> Unit
    ) {
        var attempt = 0
        var delayMs = baseRetryDelayMs
        while (true) {
            ensureActive()
            try {
                downloadPart(url, start, end, outFile, progressForPart)
                return
            } catch (e: Exception) {
                attempt++
                if (attempt > maxRetries) throw IOException("Part failed after $maxRetries attempts", e)
                // exponential backoff with jitter
                val jitter = (Math.random() * 100).toLong()
                delay(delayMs + jitter)
                delayMs = (delayMs * 2).coerceAtMost(10_000)
            }
        }
    }

    private fun safeExecute(req: Request): Response? {
        return try {
            client.newCall(req).execute()
        } catch (e: Exception) {
            null
        }
    }

    private fun downloadPart(
        url: String,
        start: Long,
        end: Long,
        outFile: File,
        progressForPart: (Long) -> Unit
    ) {
        val req = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=$start-$end")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for range $start-$end")
            val body = resp.body ?: throw IOException("Empty body for range")
            var total = 0L
            body.byteStream().use { input ->
                outFile.outputStream().use { output ->
                    val buf = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        total += read
                        progressForPart(total)
                    }
                    output.flush()
                }
            }
        }
    }

    private suspend fun downloadToOutputStreamSaf(url: String, destUri: Uri, progressCb: (Float) -> Unit) {
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            resolver.openOutputStream(destUri, "w")?.use { out ->
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("Download failed: ${resp.code}")
                    val body = resp.body ?: throw IOException("Empty body")

                    val total = body.contentLength()
                    var readTotal = 0L
                    val input = body.byteStream()
                    val buf = ByteArray(8 * 1024)
                    var r: Int
                    while (input.read(buf).also { r = it } != -1) {
                        ensureActive()
                        out.write(buf, 0, r)
                        readTotal += r
                        if (total > 0) progressCb(readTotal.toFloat() / total.toFloat())
                    }
                    out.flush()
                }
            }
            progressCb(1.0f)
        }
    }
}
