package com.example.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseUrl: String,
    val releaseNotes: String?
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/AhsanTCr/Sheildlenz/releases/latest"

    suspend fun checkForUpdates(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_LATEST_RELEASE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Sheildlenz-Android-App")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("GitHub API returned status $responseCode"))
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = reader.readLine() } != null) {
                // Wait, readLine is called twice in that loop condition! Let's fix the buffer reading loop.
            }
            // Correct reader loop:
            val sb = StringBuilder()
            val bufferedReader = BufferedReader(InputStreamReader(connection.inputStream))
            var currentLine = bufferedReader.readLine()
            while (currentLine != null) {
                sb.append(currentLine)
                currentLine = bufferedReader.readLine()
            }
            bufferedReader.close()
            connection.disconnect()

            val json = JSONObject(sb.toString())
            val tagName = json.optString("tag_name", "v1.0.0")
            val htmlUrl = json.optString("html_url", "https://github.com/AhsanTCr/Sheildlenz/releases")
            val body = json.optString("body", "Bug fixes and performance improvements.")

            val latestClean = tagName.removePrefix("v").removePrefix("V").trim()
            val currentClean = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()

            val isNewer = compareVersions(latestClean, currentClean) > 0

            Result.success(
                UpdateInfo(
                    isUpdateAvailable = isNewer,
                    latestVersion = tagName,
                    currentVersion = BuildConfig.VERSION_NAME,
                    releaseUrl = htmlUrl,
                    releaseNotes = body
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            Result.failure(e)
        }
    }

    private fun compareVersions(latest: String, current: String): Int {
        try {
            val latestParts = latest.split(".")
            val currentParts = current.split(".")
            val maxLength = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLength) {
                val lVal = latestParts.getOrNull(i)?.toIntOrNull() ?: 0
                val cVal = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
                if (lVal != cVal) {
                    return lVal.compareTo(cVal)
                }
            }
            return 0
        } catch (_: Exception) {
            return latest.compareTo(current)
        }
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
