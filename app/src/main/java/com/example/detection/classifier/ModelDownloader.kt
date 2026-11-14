package com.example.detection.classifier

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

sealed class ModelDownloadState {
    data object Idle : ModelDownloadState()
    data class Downloading(
        val linkIndex: Int,
        val progress: Float?,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : ModelDownloadState()
    data object Extracting : ModelDownloadState()
    data object Validating : ModelDownloadState()
    data class Success(val info: ActiveModelInfo) : ModelDownloadState()
    data class Failed(val linkIndex: Int, val message: String) : ModelDownloadState()
}

object ModelDownloader {
    private const val TAG = "ModelDownloader"

    const val PRIMARY_MODEL_ZIP_URL =
        "https://github.com/Ahsanpcin/Sheild-Lenz/releases/download/v1.0.0/nsfw_mobilenet_v2_140_224.1.zip"

    const val FALLBACK_MODEL_ZIP_URL =
        "https://www.dropbox.com/scl/fi/zwcxynldkwwo5t41qhbm5/nsfw_mobilenet_v2_140_224-1.zip?rlkey=s6lc435o0p5zporzfx4g7vn8x&st=7u23fjj4&dl=1"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun downloadAndInstallZipModel(
        context: Context,
        modelManager: ModelManager,
        linkIndex: Int = 1,
        onStateChanged: (ModelDownloadState) -> Unit
    ): ModelImportResult = withContext(Dispatchers.IO) {
        val targetUrl = if (linkIndex == 1) PRIMARY_MODEL_ZIP_URL else FALLBACK_MODEL_ZIP_URL
        val cacheDir = File(context.cacheDir, "model_downloads").apply {
            if (!exists()) mkdirs()
        }
        val zipFile = File(cacheDir, "model_bundle_link$linkIndex.zip")
        val extractedFile = File(cacheDir, "extracted_classifier.tflite")

        try {
            onStateChanged(
                ModelDownloadState.Downloading(
                    linkIndex = linkIndex,
                    progress = 0f,
                    bytesDownloaded = 0L,
                    totalBytes = -1L
                )
            )

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Sheildlenz-Android-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = "Download failed (HTTP ${response.code}) on Link $linkIndex."
                    onStateChanged(ModelDownloadState.Failed(linkIndex, msg))
                    return@withContext ModelImportResult.Failure(msg)
                }

                val body = response.body ?: run {
                    val msg = "Empty response from Link $linkIndex."
                    onStateChanged(ModelDownloadState.Failed(linkIndex, msg))
                    return@withContext ModelImportResult.Failure(msg)
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                val buffer = ByteArray(16 * 1024)
                var lastEmitTime = 0L

                body.byteStream().use { input ->
                    FileOutputStream(zipFile).use { output ->
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 80L || (totalBytes > 0 && downloadedBytes >= totalBytes)) {
                                lastEmitTime = now
                                val fraction = if (totalBytes > 0L) {
                                    (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    null
                                }
                                onStateChanged(
                                    ModelDownloadState.Downloading(
                                        linkIndex = linkIndex,
                                        progress = fraction,
                                        bytesDownloaded = downloadedBytes,
                                        totalBytes = totalBytes
                                    )
                                )
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (!zipFile.exists() || zipFile.length() < 1024L) {
                val msg = "Downloaded archive from Link $linkIndex was empty or invalid."
                onStateChanged(ModelDownloadState.Failed(linkIndex, msg))
                return@withContext ModelImportResult.Failure(msg)
            }

            onStateChanged(ModelDownloadState.Extracting)
            val extractedEntryName = extractTfliteFromZip(zipFile, extractedFile)
            if (extractedEntryName == null || !extractedFile.exists() || extractedFile.length() < 1024L) {
                val msg = "No valid .tflite model file was found inside the zip archive."
                onStateChanged(ModelDownloadState.Failed(linkIndex, msg))
                return@withContext ModelImportResult.Failure(msg)
            }

            onStateChanged(ModelDownloadState.Validating)
            val importResult = modelManager.importModelFromFile(
                sourceFile = extractedFile,
                displayName = extractedEntryName
            )

            when (importResult) {
                is ModelImportResult.Success -> {
                    onStateChanged(ModelDownloadState.Success(importResult.info))
                }
                is ModelImportResult.Failure -> {
                    onStateChanged(ModelDownloadState.Failed(linkIndex, importResult.reason))
                }
            }
            return@withContext importResult
        } catch (ce: CancellationException) {
            onStateChanged(ModelDownloadState.Idle)
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model from link $linkIndex", e)
            val msg = "Link $linkIndex failed: ${e.message ?: "Network error"}"
            onStateChanged(ModelDownloadState.Failed(linkIndex, msg))
            return@withContext ModelImportResult.Failure(msg)
        } finally {
            try {
                if (zipFile.exists()) zipFile.delete()
                if (extractedFile.exists()) extractedFile.delete()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Extracts the first `.tflite` entry inside [zipFile] into [destinationFile].
     * Protects against Zip Slip path traversal and nested directories inside zips.
     * Returns the clean file name (e.g. "nsfw_mobilenet_v2_140_224.tflite") on success, or null.
     */
    internal fun extractTfliteFromZip(zipFile: File, destinationFile: File): String? {
        val destCanonicalDir = destinationFile.parentFile?.canonicalPath ?: return null
        val targetCanonicalPath = destinationFile.canonicalPath
        if (!targetCanonicalPath.startsWith(destCanonicalDir)) {
            return null
        }

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val rawName = entry.name
                val cleanName = rawName.substringAfterLast('/').substringAfterLast('\\')
                if (!entry.isDirectory &&
                    !rawName.contains("..") &&
                    !cleanName.startsWith("._") &&
                    cleanName.endsWith(".tflite", ignoreCase = true)
                ) {
                    FileOutputStream(destinationFile).use { out ->
                        val buffer = ByteArray(16 * 1024)
                        var len: Int
                        while (zis.read(buffer).also { len = it } != -1) {
                            out.write(buffer, 0, len)
                        }
                        out.flush()
                    }
                    zis.closeEntry()
                    return cleanName
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return null
    }

    fun formatBytesMb(bytes: Long): String {
        if (bytes <= 0L) return "0.0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}
