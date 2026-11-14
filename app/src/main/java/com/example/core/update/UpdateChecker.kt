package com.example.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseUrl: String,
    val releaseNotes: String?,
    val apkDownloadUrl: String? = null,
    val apkSizeBytes: Long = 0L
)

sealed class ApkDownloadState {
    data object Idle : ApkDownloadState()
    data class Downloading(
        val progress: Float?,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : ApkDownloadState()
    data class ReadyToInstall(val apkFile: File) : ApkDownloadState()
    data class Failed(val message: String) : ApkDownloadState()
}

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    const val GITHUB_OWNER = "Ahsanpcin"
    const val GITHUB_REPO = "Sheild-Lenz"
    const val GITHUB_RELEASES_WEB_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val GITHUB_ALL_RELEASES_URL =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=10"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun checkForUpdates(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val latestResult = fetchReleaseJson(GITHUB_LATEST_RELEASE_URL)
            val releaseObj: JSONObject = if (latestResult.code == 200 && latestResult.body != null) {
                JSONObject(latestResult.body)
            } else if (latestResult.code == 404) {
                // Fallback to all releases list in case latest release is tagged as pre-release or /latest isn't indexed yet
                val allResult = fetchReleaseJson(GITHUB_ALL_RELEASES_URL)
                if (allResult.code != 200 || allResult.body == null) {
                    return@withContext Result.failure(
                        Exception("No releases found on GitHub ($GITHUB_OWNER/$GITHUB_REPO) • HTTP ${allResult.code}")
                    )
                }
                val releasesArray = JSONArray(allResult.body)
                var firstValid: JSONObject? = null
                for (i in 0 until releasesArray.length()) {
                    val candidate = releasesArray.optJSONObject(i) ?: continue
                    if (!candidate.optBoolean("draft", false)) {
                        firstValid = candidate
                        break
                    }
                }
                firstValid ?: return@withContext Result.success(
                    UpdateInfo(
                        isUpdateAvailable = false,
                        latestVersion = BuildConfig.VERSION_NAME,
                        currentVersion = BuildConfig.VERSION_NAME,
                        releaseUrl = GITHUB_RELEASES_WEB_URL,
                        releaseNotes = null
                    )
                )
            } else {
                return@withContext Result.failure(
                    Exception("GitHub API returned status ${latestResult.code}")
                )
            }

            val info = parseReleaseJson(releaseObj, BuildConfig.VERSION_NAME)
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            Result.failure(e)
        }
    }

    internal fun parseReleaseJson(json: JSONObject, currentVersionName: String): UpdateInfo {
        val tagName = json.optString("tag_name", "v$currentVersionName")
        val htmlUrl = json.optString("html_url", GITHUB_RELEASES_WEB_URL)
        val body = json.optString("body", "").ifBlank {
            "Bug fixes and performance improvements."
        }

        var apkUrl: String? = null
        var apkSize = 0L
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val assetName = asset.optString("name", "")
                val downloadUrl = asset.optString("browser_download_url", "")
                if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                    apkUrl = downloadUrl
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }
        }

        val latestClean = tagName.removePrefix("v").removePrefix("V").trim()
        val currentClean = currentVersionName.removePrefix("v").removePrefix("V").trim()
        val isNewer = compareVersions(latestClean, currentClean) > 0

        return UpdateInfo(
            isUpdateAvailable = isNewer,
            latestVersion = tagName,
            currentVersion = currentVersionName,
            releaseUrl = htmlUrl,
            releaseNotes = body,
            apkDownloadUrl = apkUrl,
            apkSizeBytes = apkSize
        )
    }

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onStateChanged: (ApkDownloadState) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
        }
        val apkFile = File(updatesDir, "sheildlenz-update.apk")

        try {
            onStateChanged(ApkDownloadState.Downloading(0f, 0L, -1L))

            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "Sheildlenz-Android-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = "APK download failed (HTTP ${response.code})"
                    onStateChanged(ApkDownloadState.Failed(msg))
                    return@withContext Result.failure(Exception(msg))
                }

                val body = response.body ?: run {
                    val msg = "Empty APK download response"
                    onStateChanged(ApkDownloadState.Failed(msg))
                    return@withContext Result.failure(Exception(msg))
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                val buffer = ByteArray(16 * 1024)
                var lastEmit = 0L

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            val now = System.currentTimeMillis()
                            if (now - lastEmit >= 80L || (totalBytes > 0 && downloadedBytes >= totalBytes)) {
                                lastEmit = now
                                val fraction = if (totalBytes > 0L) {
                                    (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    null
                                }
                                onStateChanged(ApkDownloadState.Downloading(fraction, downloadedBytes, totalBytes))
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (!apkFile.exists() || apkFile.length() < 10_000L) {
                val msg = "Downloaded APK appears incomplete or corrupted."
                onStateChanged(ApkDownloadState.Failed(msg))
                return@withContext Result.failure(Exception(msg))
            }

            onStateChanged(ApkDownloadState.ReadyToInstall(apkFile))
            Result.success(apkFile)
        } catch (ce: CancellationException) {
            onStateChanged(ApkDownloadState.Idle)
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download APK", e)
            val msg = e.message ?: "APK download failed"
            onStateChanged(ApkDownloadState.Failed(msg))
            Result.failure(e)
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(permIntent)
                return false
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch APK installer", e)
            false
        }
    }

    private data class HttpResponseSnapshot(val code: Int, val body: String?)

    private fun fetchReleaseJson(urlString: String): HttpResponseSnapshot {
        val request = Request.Builder()
            .url(urlString)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Sheildlenz-Android-App")
            .build()

        client.newCall(request).execute().use { response ->
            return HttpResponseSnapshot(
                code = response.code,
                body = response.body?.string()
            )
        }
    }

    internal fun compareVersions(latest: String, current: String): Int {
        return try {
            val cleanLatest = latest.substringBefore("-").trim()
            val cleanCurrent = current.substringBefore("-").trim()
            val latestParts = cleanLatest.split(".")
            val currentParts = cleanCurrent.split(".")
            val maxLength = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLength) {
                val lVal = latestParts.getOrNull(i)?.toIntOrNull() ?: 0
                val cVal = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
                if (lVal != cVal) {
                    return lVal.compareTo(cVal)
                }
            }
            0
        } catch (_: Exception) {
            latest.compareTo(current)
        }
    }

    fun formatBytesMb(bytes: Long): String {
        if (bytes <= 0L) return "0.0 MB"
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    fun openReleaseUrl(context: Context, urlString: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open release URL", e)
        }
    }
}
