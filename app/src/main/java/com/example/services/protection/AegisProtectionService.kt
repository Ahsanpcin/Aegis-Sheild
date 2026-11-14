package com.example.services.protection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.models.SecurityIncident
import com.example.data.preferences.AegisPreferences
import com.example.data.repository.ProtectionRepository
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
    private var lastTelemetryEmitMs: Long = 0L

    private var frameCounter: Long = 0L
    private var skippedCounter: Long = 0L
    private var fpsCounter: Int = 0
    private var lastFpsSampleTime: Long = 0L
    private var consecutiveIdleOrSafeTicks: Int = 0
    private var seenContextChangeUptimeMs: Long = 0L
    private var lastPublishedEffectiveIntervalMs: Int = 350

    @Volatile
    private var isStopped = false

    @Volatile
    private var isScreenInteractive: Boolean = true

    @Volatile
    private var batteryPercent: Int = 100

    @Volatile
    private var isCharging: Boolean = false

    @Volatile
    private var isPowerSaveMode: Boolean = false

    @Volatile
    private var isLowBatteryThrottled: Boolean = false

    private var isPowerReceiverRegistered = false
    private val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as? PowerManager }

    private val powerAndScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenInteractive = false
                    if (container.preferences.pauseWhenScreenOff.value) {
                        container.protectionRepository.recordInspectionMetrics(
                            frameCount = frameCounter,
                            fps = 0.0f,
                            skippedCount = skippedCounter,
                            effectiveIntervalMs = lastPublishedEffectiveIntervalMs
                        )
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenInteractive = true
                    consecutiveIdleOrSafeTicks = 0
                    lastInspectionTimeMs = 0L
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                    if (level >= 0) {
                        batteryPercent = ((level * 100) / scale).coerceIn(0, 100)
                    }
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                    syncPowerStateToRepo()
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    isPowerSaveMode = powerManager?.isPowerSaveMode == true
                    syncPowerStateToRepo()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AegisProtectionService created (Eco Engine active).")
        createNotificationChannel()

        contentClassifier = TFLiteContentClassifier(this, container.modelManager)
        framePreprocessor = FramePreprocessor(
            inputWidth = contentClassifier.inputWidth,
            inputHeight = contentClassifier.inputHeight,
            isQuantizedUInt8 = contentClassifier.isQuantizedInput
        )
        decisionEngine = ThreatDecisionEngine()

        // Run frame scanning at background efficiency priority to favor LITTLE CPU cores
        val thread = HandlerThread("AegisFrameProcessor", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
        }
        backgroundThread = thread
        backgroundHandler = Handler(thread.looper)

        isScreenInteractive = powerManager?.isInteractive != false
        isPowerSaveMode = powerManager?.isPowerSaveMode == true
        val (pct, charging) = ProtectionRepository.readBatterySnapshot(this)
        batteryPercent = pct
        isCharging = charging
        registerPowerReceiver()
        syncPowerStateToRepo()

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
                    consecutiveIdleOrSafeTicks = 0
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

        // Dynamically resize VirtualDisplay when user toggles compact capture width in Edits
        serviceScope.launch {
            container.preferences.captureWidthPx.drop(1).collect { newWidth ->
                backgroundHandler?.post {
                    if (!isStopped && mediaProjection != null) {
                        resizeVirtualDisplayCapture(newWidth.coerceIn(224, 1080))
                    }
                }
            }
        }
    }

    private fun registerPowerReceiver() {
        if (isPowerReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        registerReceiver(powerAndScreenReceiver, filter)
        isPowerReceiverRegistered = true
    }

    private fun syncPowerStateToRepo() {
        val prefs = container.preferences
        val (effectiveMs, lowBatt) = AegisPreferences.computeEffectiveScanIntervalMs(
            baseIntervalMs = prefs.scanIntervalMs.value,
            adaptiveEnabled = prefs.adaptiveCadenceEnabled.value,
            consecutiveIdleOrSafeCount = consecutiveIdleOrSafeTicks,
            autoThrottleLowBattery = prefs.autoThrottleLowBattery.value,
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            wasLowBatteryThrottled = isLowBatteryThrottled
        )
        isLowBatteryThrottled = lowBatt
        lastPublishedEffectiveIntervalMs = effectiveMs
        container.protectionRepository.updateBatteryPowerState(
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            isLowBatteryThrottled = lowBatt,
            effectiveIntervalMs = effectiveMs
        )
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

    private fun computeCaptureDimensions(targetWidthPx: Int): Triple<Int, Int, Int> {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val screenWidth = metrics.widthPixels.coerceAtLeast(1)
        val screenHeight = metrics.heightPixels.coerceAtLeast(1)
        val captureWidth = targetWidthPx.coerceIn(224, 1080)
        val scale = captureWidth.toFloat() / screenWidth
        val captureHeight = (screenHeight * scale).toInt().coerceAtLeast(224)
        return Triple(captureWidth, captureHeight, metrics.densityDpi)
    }

    private fun attachImageReaderListener(reader: ImageReader) {
        reader.setOnImageAvailableListener({ ir ->
            val image = try {
                ir.acquireLatestImage()
            } catch (_: Exception) {
                null
            } ?: return@setOnImageAvailableListener

            handleIncomingImage(image)
        }, backgroundHandler)
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

        val (captureWidth, captureHeight, densityDpi) =
            computeCaptureDimensions(container.preferences.captureWidthPx.value)

        val reader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        attachImageReaderListener(reader)

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

    private fun resizeVirtualDisplayCapture(newWidthPx: Int) {
        val vd = virtualDisplay ?: return
        val oldReader = imageReader
        try {
            val (captureWidth, captureHeight, densityDpi) = computeCaptureDimensions(newWidthPx)
            if (oldReader != null && oldReader.width == captureWidth && oldReader.height == captureHeight) {
                return
            }
            val newReader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
            attachImageReaderListener(newReader)
            vd.resize(captureWidth, captureHeight, densityDpi)
            vd.surface = newReader.surface
            imageReader = newReader
            try {
                oldReader?.close()
            } catch (_: Exception) {
            }
            Log.i(TAG, "Resized VirtualDisplay capture cleanly to ${captureWidth}x${captureHeight}")
        } catch (e: Exception) {
            Log.w(TAG, "VirtualDisplay resize skipped: ${e.message}")
        }
    }

    private fun handleIncomingImage(image: Image) {
        val prefs = container.preferences

        // Stage 1: Zero-Copy Gate 1 — Screen-Off true sleep
        if (prefs.pauseWhenScreenOff.value && (!isScreenInteractive || powerManager?.isInteractive == false)) {
            image.close()
            return
        }

        // Stage 1: Zero-Copy Gate 2 — Lockdown overlay active
        if (prefs.isLockdownActive() && overlayController.isOverlayVisible) {
            image.close()
            return
        }

        // Stage 1: Zero-Copy Gate 3 — Wake cadence immediately when Accessibility sees app/window change
        val latestContextChange = container.protectionRepository.lastContextChangeUptimeMs
        if (latestContextChange != seenContextChangeUptimeMs) {
            seenContextChangeUptimeMs = latestContextChange
            consecutiveIdleOrSafeTicks = 0
        }

        // Compute adaptive + <20% low-battery throttled interval
        val (effectiveIntervalMs, lowBatt) = AegisPreferences.computeEffectiveScanIntervalMs(
            baseIntervalMs = prefs.scanIntervalMs.value,
            adaptiveEnabled = prefs.adaptiveCadenceEnabled.value,
            consecutiveIdleOrSafeCount = consecutiveIdleOrSafeTicks,
            autoThrottleLowBattery = prefs.autoThrottleLowBattery.value,
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            wasLowBatteryThrottled = isLowBatteryThrottled
        )
        if (lowBatt != isLowBatteryThrottled) {
            isLowBatteryThrottled = lowBatt
            container.protectionRepository.updateBatteryPowerState(
                batteryPercent = batteryPercent,
                isCharging = isCharging,
                isPowerSaveMode = isPowerSaveMode,
                isLowBatteryThrottled = lowBatt,
                effectiveIntervalMs = effectiveIntervalMs
            )
        }
        lastPublishedEffectiveIntervalMs = effectiveIntervalMs

        val currentTime = SystemClock.uptimeMillis()
        if (currentTime - lastInspectionTimeMs < effectiveIntervalMs.toLong()) {
            image.close()
            return
        }
        lastInspectionTimeMs = currentTime

        // Stage 1: Zero-Copy Gate 4 — Skip pixel copy & AI completely if model isn't ready
        if (!contentClassifier.isModelReady) {
            image.close()
            return
        }

        // Stage 1: Zero-Copy Gate 5 — Whitelisted apps or safe system/home screens skip BEFORE bitmap extraction
        val activePkg = container.protectionRepository.telemetry.value.activeMonitoredPackage
        if ((!activePkg.isNullOrBlank() && prefs.isPackageWhitelisted(activePkg)) ||
            isZeroCostSkipPackage(activePkg, packageName)
        ) {
            image.close()
            skippedCounter++
            consecutiveIdleOrSafeTicks = (consecutiveIdleOrSafeTicks + 1).coerceAtMost(30)
            emitThrottledTelemetry(currentTime, currentFpsOverride = 0.0f)
            return
        }

        processFrame(image, currentTime)
    }

    private fun processFrame(image: Image, nowUptime: Long) {
        try {
            val bitmap = framePreprocessor.extractBitmapFromImage(image)
            image.close()

            if (bitmap == null) return

            // Stage 2: Perceptual block-luma duplicate skip (avoids inference on static/near-static frames)
            if (container.preferences.skipDuplicateFrames.value && framePreprocessor.isFrameDuplicate(bitmap)) {
                skippedCounter++
                consecutiveIdleOrSafeTicks = (consecutiveIdleOrSafeTicks + 1).coerceAtMost(30)
                emitThrottledTelemetry(nowUptime)
                return
            }

            // Visual change detected -> snap adaptive cadence back to responsive speed
            consecutiveIdleOrSafeTicks = 0
            frameCounter++
            fpsCounter++
            emitThrottledTelemetry(nowUptime)

            framePreprocessor.configure(
                width = contentClassifier.inputWidth,
                height = contentClassifier.inputHeight,
                quantizedUInt8 = contentClassifier.isQuantizedInput
            )

            val tensorBuffer = framePreprocessor.preprocessBitmapToTensorBuffer(bitmap)
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

    private fun emitThrottledTelemetry(nowUptime: Long, currentFpsOverride: Float? = null) {
        if (lastFpsSampleTime == 0L) {
            lastFpsSampleTime = nowUptime
        }
        if (nowUptime - lastTelemetryEmitMs < 1000L) return

        val elapsedMs = (nowUptime - lastFpsSampleTime).coerceAtLeast(1L)
        val computedFps = currentFpsOverride ?: ((fpsCounter * 1000f) / elapsedMs)
        fpsCounter = 0
        lastFpsSampleTime = nowUptime
        lastTelemetryEmitMs = nowUptime

        container.protectionRepository.recordInspectionMetrics(
            frameCount = frameCounter,
            fps = computedFps,
            skippedCount = skippedCounter,
            effectiveIntervalMs = lastPublishedEffectiveIntervalMs
        )
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

        if (container.preferences.evictAppOnThreat.value) {
            AegisAccessibilityService.closeAndEvictPackage(this, targetPkg)
        } else {
            AegisAccessibilityService.performEmergencyHomeAction()
        }
        val suspended = AegisDeviceAdminReceiver.suspendPackageIfDeviceOwner(this, targetPkg)

        container.preferences.engageFullDeviceLockdown(
            appName = targetAppName,
            packageName = targetPkg,
            categoryName = decision.category.displayName,
            details = fullDetails
        )
        AegisLockdownService.start(this)
        overlayController.showLockdownOverlay(container.preferences.lockdownIncidentInfo.value)

        val totalMinutes = container.preferences.lockdownDurationMinutes.value
        val durationLabel = AegisPreferences.formatMinutesLabel(totalMinutes)
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

        if (isPowerReceiverRegistered) {
            try {
                unregisterReceiver(powerAndScreenReceiver)
            } catch (_: Exception) {
            }
            isPowerReceiverRegistered = false
        }

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
        framePreprocessor.recyclePools()

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

        private val SAFE_SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.phone",
            "com.android.incallui",
            "com.android.emergency",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.oneplus.launcher",
            "com.oppo.launcher",
            "com.android.calculator2",
            "com.google.android.calculator",
            "com.android.deskclock",
            "com.google.android.deskclock"
        )

        internal fun isZeroCostSkipPackage(activePackage: String?, ownPackage: String): Boolean {
            if (activePackage.isNullOrBlank()) return false
            val pkg = activePackage.lowercase()
            return pkg == ownPackage.lowercase() || pkg in SAFE_SYSTEM_PACKAGES
        }

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
