package com.example.core.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.di.AegisContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
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
    data object Installing : ApkDownloadState()
    data class ReadyToInstall(
        val apkFile: File,
        val needsInstallPermission: Boolean = false
    ) : ApkDownloadState()
    data object Installed : ApkDownloadState()
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
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=20"

    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _checkError = MutableStateFlow<String?>(null)
    val checkError: StateFlow<String?> = _checkError.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _showUpToDateDialog = MutableStateFlow(false)
    val showUpToDateDialog: StateFlow<Boolean> = _showUpToDateDialog.asStateFlow()

    private val _apkDownloadState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)
    val apkDownloadState: StateFlow<ApkDownloadState> = _apkDownloadState.asStateFlow()

    @Volatile
    private var activeCheckJob: Job? = null

    @Volatile
    private var activeApkJob: Job? = null

    @Volatile
    private var pendingInstallApk: File? = null

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun startCheckForUpdates(showUpToDatePopup: Boolean = true) {
        if (_isChecking.value) return
        // Once an update is already found and an APK download/install is in flight, keep it undisturbed
        if (_apkDownloadState.value is ApkDownloadState.Downloading ||
            _apkDownloadState.value is ApkDownloadState.Installing
        ) {
            return
        }
        activeCheckJob?.cancel()
        _isChecking.value = true
        _checkError.value = null
        if (!showUpToDatePopup) {
            _showUpToDateDialog.value = false
        }
        activeCheckJob = updateScope.launch {
            val result = checkForUpdates()
            _isChecking.value = false
            result.onSuccess { info ->
                _updateInfo.value = info
                if (!info.isUpdateAvailable && showUpToDatePopup) {
                    _showUpToDateDialog.value = true
                }
            }.onFailure { err ->
                if (showUpToDatePopup) {
                    _checkError.value = err.message ?: "Failed to check for updates"
                }
            }
        }
    }

    fun startApkDownload(context: Context, apkUrl: String) {
        if (_apkDownloadState.value is ApkDownloadState.Downloading ||
            _apkDownloadState.value is ApkDownloadState.Installing
        ) {
            return
        }
        val appContext = context.applicationContext
        activeApkJob?.cancel()
        activeApkJob = updateScope.launch {
            AegisBackgroundDownloadService.notifyProgress(
                context = appContext,
                title = "Downloading Sheildlenz Update",
                subtitle = "Connecting...",
                progressPct = -1,
                force = true
            )
            val result = downloadApk(appContext, apkUrl) { state ->
                _apkDownloadState.value = state
                when (state) {
                    is ApkDownloadState.Downloading -> {
                        val pct = state.progress?.let { (it * 100).toInt().coerceIn(0, 100) } ?: -1
                        val label = if (state.totalBytes > 0L) {
                            "${formatBytesMb(state.bytesDownloaded)} / ${formatBytesMb(state.totalBytes)}"
                        } else {
                            formatBytesMb(state.bytesDownloaded)
                        }
                        AegisBackgroundDownloadService.notifyProgress(
                            context = appContext,
                            title = "Downloading Sheildlenz Update",
                            subtitle = if (pct >= 0) "$pct% • $label" else label,
                            progressPct = pct
                        )
                    }
                    else -> {}
                }
            }
            result.onSuccess { file ->
                AegisBackgroundDownloadService.notifyProgress(
                    context = appContext,
                    title = "Installing Sheildlenz Update",
                    subtitle = "Finalizing update...",
                    progressPct = -1,
                    force = true
                )
                autoInstallApk(appContext, file)
            }.onFailure {
                AegisBackgroundDownloadService.stop(appContext)
            }
        }
    }

    fun cancelApkDownload() {
        activeApkJob?.cancel()
        activeApkJob = null
        _apkDownloadState.value = ApkDownloadState.Idle
    }

    fun dismissUpToDateDialog() {
        _showUpToDateDialog.value = false
    }

    fun dismissUpdatePrompt() {
        if (_apkDownloadState.value !is ApkDownloadState.Downloading &&
            _apkDownloadState.value !is ApkDownloadState.Installing
        ) {
            _apkDownloadState.value = ApkDownloadState.Idle
        }
    }

    suspend fun checkForUpdates(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val candidates = mutableListOf<JSONObject>()

            val allResult = fetchReleaseJson(GITHUB_ALL_RELEASES_URL)
            if (allResult.code == 200 && allResult.body != null) {
                val releasesArray = JSONArray(allResult.body)
                for (i in 0 until releasesArray.length()) {
                    val item = releasesArray.optJSONObject(i) ?: continue
                    if (!item.optBoolean("draft", false)) {
                        candidates.add(item)
                    }
                }
            }

            if (candidates.isEmpty()) {
                val latestResult = fetchReleaseJson(GITHUB_LATEST_RELEASE_URL)
                if (latestResult.code == 200 && latestResult.body != null) {
                    val obj = JSONObject(latestResult.body)
                    if (!obj.optBoolean("draft", false)) {
                        candidates.add(obj)
                    }
                } else if (allResult.code != 200) {
                    val errCode = if (latestResult.code == 404) allResult.code else latestResult.code
                    return@withContext Result.failure(
                        Exception("No releases found on GitHub ($GITHUB_OWNER/$GITHUB_REPO) • HTTP $errCode")
                    )
                }
            }

            if (candidates.isEmpty()) {
                return@withContext Result.success(
                    UpdateInfo(
                        isUpdateAvailable = false,
                        latestVersion = "v${BuildConfig.VERSION_NAME}",
                        currentVersion = BuildConfig.VERSION_NAME,
                        releaseUrl = GITHUB_RELEASES_WEB_URL,
                        releaseNotes = null
                    )
                )
            }

            // Prefer APK-bearing releases so model-only releases (like v1.0.0) never mask newer app versions,
            // and compare strictly by version number to identify the true latest release.
            val apkCandidates = candidates.filter { hasApkAsset(it) }
            val pool = if (apkCandidates.isNotEmpty()) apkCandidates else candidates
            val bestRelease = pool.maxWithOrNull { a, b ->
                val verA = cleanVersionTag(a.optString("tag_name", ""))
                val verB = cleanVersionTag(b.optString("tag_name", ""))
                compareVersions(verA, verB)
            } ?: candidates.first()

            val info = parseReleaseJson(bestRelease, BuildConfig.VERSION_NAME)
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            Result.failure(e)
        }
    }

    private fun hasApkAsset(json: JSONObject): Boolean {
        val assets = json.optJSONArray("assets") ?: return false
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val assetName = asset.optString("name", "")
            val downloadUrl = asset.optString("browser_download_url", "")
            if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                return true
            }
        }
        return false
    }

    internal fun cleanVersionTag(raw: String): String {
        val trimmed = raw.trim()
        val match = Regex("(\\d+(?:\\.\\d+)*)").find(trimmed)
        return match?.value ?: trimmed.removePrefix("v").removePrefix("V").trim()
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

        val latestClean = cleanVersionTag(tagName)
        val currentClean = cleanVersionTag(currentVersionName)
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
        val updatesDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.apply { if (!exists()) mkdirs() }
            ?: File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
        val apkFile = File(updatesDir, "Sheildlenz-update.apk")

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

            pendingInstallApk = apkFile
            onStateChanged(ApkDownloadState.ReadyToInstall(apkFile, needsInstallPermission = false))
            Result.success(apkFile)
        } catch (ce: CancellationException) {
            AegisBackgroundDownloadService.stop(context)
            onStateChanged(ApkDownloadState.Idle)
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download APK", e)
            AegisBackgroundDownloadService.stop(context)
            val msg = e.message ?: "APK download failed"
            onStateChanged(ApkDownloadState.Failed(msg))
            Result.failure(e)
        }
    }

    /**
     * Automatically installs the downloaded APK using PackageInstaller.Session (silent in-place
     * self-update on Android 12+ once unknown-app permission is enabled) without triggering
     * Sheildlenz's Anti-Tamper password lock or deleting the file prematurely.
     */
    fun autoInstallApk(context: Context, apkFile: File): Boolean {
        val appContext = context.applicationContext
        pendingInstallApk = apkFile
        return try {
            AegisContainer.getInstance(appContext).preferences.grantTemporarySettingsAuthorization()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !appContext.packageManager.canRequestPackageInstalls()) {
                _apkDownloadState.value = ApkDownloadState.ReadyToInstall(apkFile, needsInstallPermission = true)
                AegisBackgroundDownloadService.stop(appContext)
                val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${appContext.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(permIntent)
                return false
            }

            _apkDownloadState.value = ApkDownloadState.Installing
            val installer = appContext.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(appContext.packageName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                FileInputStream(apkFile).use { input ->
                    session.openWrite("sheildlenz_update.apk", 0, apkFile.length()).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                        session.fsync(output)
                    }
                }

                val callbackIntent = Intent(appContext, ApkInstallResultReceiver::class.java)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val pendingIntent = PendingIntent.getBroadcast(appContext, sessionId, callbackIntent, flags)
                session.commit(pendingIntent.intentSender)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "PackageInstaller session failed; falling back to ACTION_VIEW installer", e)
            installApk(appContext, apkFile)
        }
    }

    fun retryPendingInstallIfAuthorized(context: Context) {
        val state = _apkDownloadState.value
        val file = pendingInstallApk
            ?: (state as? ApkDownloadState.ReadyToInstall)?.apkFile
            ?: return
        if (!file.exists()) return
        if (state is ApkDownloadState.ReadyToInstall && state.needsInstallPermission) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
                autoInstallApk(context, file)
            }
        }
    }

    fun fallbackViewInstallIfPending(context: Context): Boolean {
        val file = pendingInstallApk ?: return false
        if (!file.exists()) return false
        return installApk(context, file)
    }

    fun onPackageInstallSucceeded(context: Context) {
        pendingInstallApk = null
        AegisBackgroundDownloadService.stop(context)
        _apkDownloadState.value = ApkDownloadState.Installed
    }

    fun onPackageInstallFailed(message: String) {
        _apkDownloadState.value = ApkDownloadState.Failed(message)
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        val appContext = context.applicationContext
        pendingInstallApk = apkFile
        return try {
            AegisContainer.getInstance(appContext).preferences.grantTemporarySettingsAuthorization()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !appContext.packageManager.canRequestPackageInstalls()) {
                _apkDownloadState.value = ApkDownloadState.ReadyToInstall(apkFile, needsInstallPermission = true)
                AegisBackgroundDownloadService.stop(appContext)
                val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${appContext.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(permIntent)
                return false
            }

            val apkUri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            appContext.startActivity(installIntent)
            AegisBackgroundDownloadService.stop(appContext)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch APK installer", e)
            AegisBackgroundDownloadService.stop(appContext)
            false
        }
    }

    private data class HttpResponseSnapshot(val code: Int, val body: String?)

    private fun fetchReleaseJson(urlString: String): HttpResponseSnapshot {
        val request = Request.Builder()
            .url(urlString)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Sheildlenz-Android-App")
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
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
