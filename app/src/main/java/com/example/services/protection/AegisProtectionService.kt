package com.example.services.protection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.models.SecurityIncident
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity
import com.example.detection.classifier.ClassificationResult
import com.example.detection.classifier.TFLiteContentClassifier
import com.example.detection.decision.DecisionOutcome
import com.example.detection.decision.ThreatDecisionEngine
import com.example.detection.preprocessing.FramePreprocessor
import com.example.di.AegisContainer
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class AegisProtectionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val container by lazy { AegisContainer.getInstance(this) }
    private val overlayController get() = container.overlayController

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private lateinit var framePreprocessor: FramePreprocessor
    private lateinit var contentClassifier: TFLiteContentClassifier
    private lateinit var decisionEngine: ThreatDecisionEngine

    private var lastInspectionTimeMs: Long = 0L

    private var frameCounter: Long = 0L
    private var fpsCounter: Int = 0
    private var lastFpsSampleTime: Long = 0L
    private var isStopped = false
    private val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as? PowerManager }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AegisProtectionService created.")
        createNotificationChannel()

        contentClassifier = TFLiteContentClassifier(this, container.modelManager)
        framePreprocessor = FramePreprocessor(
            inputWidth = contentClassifier.inputWidth,
            inputHeight = contentClassifier.inputHeight,
            isQuantizedUInt8 = contentClassifier.isQuantizedInput
        )
        decisionEngine = ThreatDecisionEngine()

        val thread = HandlerThread("AegisFrameProcessor").apply { start() }
        backgroundThread = thread
        backgroundHandler = Handler(thread.looper)

        container.refreshModelTelemetry()
        container.protectionRepository.setServiceRunning(true)

        // Automatically reload classifier if user imports or clears a .tflite file while running
        serviceScope.launch {
            container.modelManager.modelVersion.drop(1).collect {
                backgroundHandler?.post {
                    val reloaded = contentClassifier.reloadModel()
                    if (reloaded) {
                        framePreprocessor.configure(
                            width = contentClassifier.inputWidth,
                            height = contentClassifier.inputHeight,
                            quantizedUInt8 = contentClassifier.isQuantizedInput
                        )
                    }
                    decisionEngine.reset()
                    container.refreshModelTelemetry()
                    val statusLabel = if (reloaded) {
                        "Sheildlenz Active • AI Guardian Armed"
                    } else {
                        "Sheildlenz Capture Active • Load .tflite Model"
                    }
                    val nm = getSystemService(NotificationManager::class.java)
                    nm?.notify(NOTIFICATION_ID, buildForegroundNotification(statusLabel))
                    Log.i(TAG, "Service reloaded TFLite model cleanly. Ready=$reloaded")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_START_PROTECTION -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                @Suppress("DEPRECATION")
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA) as? Intent
                }

                if (resultCode != 0 && resultData != null) {
                    container.preferences.setShieldDesiredActive(true)
                    val statusLabel = if (contentClassifier.isModelReady) {
                        "Sheildlenz Active • AI Guardian Armed"
                    } else {
                        "Sheildlenz Capture Active • Load .tflite Model"
                    }
                    val notification = buildForegroundNotification(statusLabel)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    startScreenCapture(resultCode, resultData)
                } else {
                    Log.e(TAG, "Missing MediaProjection consent token data.")
                    stopSelf()
                }
            }

            ACTION_STOP_PROTECTION -> {
                stopProtection()
            }
        }

        return START_STICKY
    }

    private fun startScreenCapture(resultCode: Int, resultData: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            ?: return

        val projection = projectionManager.getMediaProjection(resultCode, resultData)
        if (projection == null) {
            Log.e(TAG, "MediaProjection token retrieval failed.")
            stopSelf()
            return
        }
        mediaProjection = projection

        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.w(TAG, "MediaProjection revoked by system or user.")
                stopProtection()
            }
        }, backgroundHandler)

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val targetWidth = container.preferences.captureWidthPx.value.coerceIn(240, 1080)
        val scale = targetWidth.toFloat() / screenWidth.coerceAtLeast(1)
        val captureWidth = targetWidth
        val captureHeight = (screenHeight * scale).toInt().coerceAtLeast(240)
        val densityDpi = metrics.densityDpi

        val reader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        reader.setOnImageAvailableListener({ ir ->
            val image = try {
                ir.acquireLatestImage()
            } catch (_: Exception) {
                null
            } ?: return@setOnImageAvailableListener

            if (container.preferences.pauseWhenScreenOff.value && powerManager?.isInteractive == false) {
                image.close()
                return@setOnImageAvailableListener
            }

            val currentTime = SystemClock.uptimeMillis()
            val dynamicIntervalMs = container.preferences.scanIntervalMs.value.toLong().coerceIn(100L, 2000L)
            if (currentTime - lastInspectionTimeMs < dynamicIntervalMs) {
                image.close()
                return@setOnImageAvailableListener
            }
            lastInspectionTimeMs = currentTime

            processFrame(image)
        }, backgroundHandler)

        virtualDisplay = projection.createVirtualDisplay(
            "AegisProtectionStream",
            captureWidth,
            captureHeight,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            backgroundHandler
        )

        Log.i(TAG, "VirtualDisplay engaged: ${captureWidth}x${captureHeight} @ ${densityDpi}dpi")
        container.protectionRepository.updateMediaProjectionState(true)
    }

    private fun processFrame(image: Image) {
        try {
            val bitmap = framePreprocessor.extractBitmapFromImage(image)
            image.close()

            if (bitmap == null) return

            if (container.preferences.skipDuplicateFrames.value && framePreprocessor.isFrameDuplicate(bitmap)) {
                bitmap.recycle()
                return
            }

            frameCounter++
            fpsCounter++
            val now = SystemClock.uptimeMillis()
            if (lastFpsSampleTime == 0L) {
                lastFpsSampleTime = now
            } else if (now - lastFpsSampleTime >= 1000L) {
                val currentFps = (fpsCounter * 1000f) / (now - lastFpsSampleTime)
                container.protectionRepository.recordInspectionMetrics(frameCounter, currentFps)
                fpsCounter = 0
                lastFpsSampleTime = now
            }

            // Pause inference while our lockdown overlay covers the screen; Shield stays alive for instant resume on unlock
            if (container.preferences.isLockdownActive() && overlayController.isOverlayVisible) {
                bitmap.recycle()
                return
            }

            // Bypass if active application is whitelisted by user
            val activePkg = container.protectionRepository.telemetry.value.activeMonitoredPackage
            if (!activePkg.isNullOrBlank() && container.preferences.isPackageWhitelisted(activePkg)) {
                bitmap.recycle()
                return
            }

            // Do not attempt inference or fake results when no TFLite model is loaded
            if (!contentClassifier.isModelReady) {
                bitmap.recycle()
                return
            }

            framePreprocessor.configure(
                width = contentClassifier.inputWidth,
                height = contentClassifier.inputHeight,
                quantizedUInt8 = contentClassifier.isQuantizedInput
            )

            val tensorBuffer = framePreprocessor.preprocessBitmapToTensorBuffer(bitmap)
            bitmap.recycle()

            val classification = contentClassifier.classifyTensorBuffer(tensorBuffer)
            if (!classification.isModelLoaded) return

            val prefs = container.preferences
            val sensitivity = prefs.sensitivity.value
            val isStrictEverything = prefs.strictEverything.value
            val isMaintenance = prefs.maintenanceMode.value
            val useCustomThresholds = prefs.customThresholdsEnabled.value
            val explicitOverride = if (useCustomThresholds) prefs.customExplicitThreshold.value else null
            val suggestiveOverride = if (useCustomThresholds) prefs.customSuggestiveThreshold.value else null
            val consecutiveRequired = prefs.consecutiveFramesRequired.value

            if (!isMaintenance) {
                val decision = decisionEngine.evaluate(
                    result = classification,
                    sensitivity = sensitivity,
                    isStrictEverything = isStrictEverything,
                    explicitThresholdOverride = explicitOverride,
                    suggestiveThresholdOverride = suggestiveOverride,
                    consecutiveFramesRequired = consecutiveRequired
                )
                if (decision.shouldBlock) {
                    handleThreatDetected(classification, decision)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing error", e)
        }
    }

    private fun handleThreatDetected(classification: ClassificationResult, decision: DecisionOutcome) {
        val targetPkg = container.protectionRepository.telemetry.value.activeMonitoredPackage ?: "Active Application"
        val targetAppName = container.protectionRepository.activeAppLabel.value
        val rawScores = if (classification.rawClassScores.isNotEmpty()) {
            classification.rawClassScores.entries.joinToString(", ") { (k, v) -> "$k: ${(v * 100).toInt()}%" }
        } else {
            "Safe: ${(classification.safeScore * 100).toInt()}%, Suggestive: ${(classification.suggestiveScore * 100).toInt()}%, Explicit: ${(classification.explicitScore * 100).toInt()}%"
        }
        val fullDetails = "${decision.reason}\nModel Breakdown: $rawScores (Inference ${classification.latencyMs}ms)"

        Log.w(TAG, "THREAT CONFIRMED: ${decision.category.name} in $targetAppName ($targetPkg). Executing eviction & full-device lockdown.")

        // 1. Close offending app from foreground/background and prevent Recents re-entry if enabled
        if (container.preferences.evictAppOnThreat.value) {
            AegisAccessibilityService.closeAndEvictPackage(this, targetPkg)
        } else {
            AegisAccessibilityService.performEmergencyHomeAction()
        }
        val suspended = AegisDeviceAdminReceiver.suspendPackageIfDeviceOwner(this, targetPkg)

        // 2. Engage persistent full-device lockdown and tell everything that was detected
        container.preferences.engageFullDeviceLockdown(
            appName = targetAppName,
            packageName = targetPkg,
            categoryName = decision.category.displayName,
            details = fullDetails
        )
        AegisLockdownService.start(this)
        overlayController.showLockdownOverlay(container.preferences.lockdownIncidentInfo.value)

        val totalMinutes = container.preferences.lockdownDurationMinutes.value
        val durationLabel = com.example.data.preferences.AegisPreferences.formatMinutesLabel(totalMinutes)
        val actionSummary = buildString {
            append(if (container.preferences.evictAppOnThreat.value) "App Evicted & Background Killed" else "Screen Shielded")
            if (suspended) append(" + Package Suspended")
            append(" • $durationLabel Device Lock")
        }

        serviceScope.launch {
            val incident = SecurityIncident(
                timestampMillis = System.currentTimeMillis(),
                packageName = targetPkg,
                appDisplayName = targetAppName,
                category = decision.category,
                severity = decision.severity,
                confidenceScore = decision.confidence,
                actionTaken = actionSummary,
                diagnosticSummary = fullDetails,
                lockdownDurationMinutes = totalMinutes
            )
            container.securityHistoryRepository.logIncident(incident)
            container.protectionRepository.recordDetection(decision.category)
        }
    }

    private fun cleanupResources() {
        if (isStopped) return
        isStopped = true
        Log.i(TAG, "Cleaning up AegisProtectionService resources.")
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        imageReader = null

        try {
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        mediaProjection = null

        if (!container.preferences.isLockdownActive()) {
            overlayController.dismissBlockOverlay()
        }
        contentClassifier.close()
        decisionEngine.reset()
        framePreprocessor.reset()

        container.protectionRepository.updateMediaProjectionState(false)
        container.protectionRepository.setServiceRunning(false)
    }

    private fun stopProtection() {
        cleanupResources()
        @Suppress("DEPRECATION")
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupResources()
        backgroundThread?.quitSafely()
        backgroundThread = null
        serviceScope.cancel()
        Log.i(TAG, "AegisProtectionService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sheildlenz Protection Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors screen frames for inappropriate content and provides protection status"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sheildlenz")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "AegisProtectionService"
        const val CHANNEL_ID = "aegis_protection_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_PROTECTION = "com.example.action.START_PROTECTION"
        const val ACTION_STOP_PROTECTION = "com.example.action.STOP_PROTECTION"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, AegisProtectionService::class.java).apply {
                action = ACTION_START_PROTECTION
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AegisProtectionService::class.java).apply {
                action = ACTION_STOP_PROTECTION
            }
            context.startService(intent)
        }
    }
}
