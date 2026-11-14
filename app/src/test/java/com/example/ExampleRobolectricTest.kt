package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.AHSAN_TCR_CONTACT_DOMAIN
import com.example.ui.components.AHSAN_TCR_CONTACT_URL
import com.example.ui.components.MasterKeyGateState
import com.example.ui.components.openAhsanTcrContact
import com.example.core.models.EngineTelemetry
import com.example.core.models.SecurityIncident
import com.example.core.models.SensitivityLevel
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity
import com.example.ui.components.SecurityHealthReport
import com.example.core.update.UpdateChecker
import com.example.data.local.AegisDatabase
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.PowerMode
import com.example.data.repository.ProtectionRepository
import com.example.data.repository.SecurityHistoryRepository
import com.example.detection.classifier.ActiveModelInfo
import com.example.detection.classifier.ClassificationResult
import com.example.detection.classifier.ModelDownloader
import com.example.detection.classifier.ModelManager
import com.example.detection.decision.ThreatDecisionEngine
import com.example.detection.preprocessing.FramePreprocessor
import com.example.monitoring.accessibility.AegisAccessibilityService
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.services.protection.AegisProtectionService
import com.example.ui.history.buildAuditReportText
import com.example.ui.navigation.isStartupGuardianAuthorized
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context validates Sheildlenz app name and package`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Sheildlenz", appName)
        assertEquals("com.ahsan.tcr.sheildlenz", context.packageName)
    }

    @Test
    fun `preferences stores and verifies guardian PIN with SHA-256 hash`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)

        prefs.removeGuardianPin()
        assertFalse(prefs.hasGuardianPin.value)
        assertFalse(prefs.verifyGuardianPin(""))
        assertFalse(prefs.verifyGuardianPin("1234"))

        prefs.setGuardianPin("1234")
        assertTrue(prefs.hasGuardianPin.value)
        assertTrue(prefs.verifyGuardianPin("1234"))
        assertFalse(prefs.verifyGuardianPin(""))
        assertFalse(prefs.verifyGuardianPin("9999"))
    }

    @Test
    fun `preferences updates sensitivity level correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)

        prefs.setSensitivity(SensitivityLevel.STRICT)
        assertEquals(SensitivityLevel.STRICT, prefs.sensitivity.value)

        prefs.setSensitivity(SensitivityLevel.PERMISSIVE)
        assertEquals(SensitivityLevel.PERMISSIVE, prefs.sensitivity.value)
    }

    @Test
    fun `protection repository starts with honest disengaged standby state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ProtectionRepository(context)

        val telemetry = repo.telemetry.value
        assertFalse(telemetry.isProtectionEnabled)
        assertEquals(0L, telemetry.framesProcessedCount)
        assertEquals(0.0f, telemetry.currentInspectionFps)
    }

    @Test
    fun `decision engine allows safe content`() {
        val engine = ThreatDecisionEngine()
        val safeResult = ClassificationResult(
            safeScore = 0.95f,
            suggestiveScore = 0.03f,
            explicitScore = 0.02f,
            topCategory = ThreatCategory.BENIGN_SAFE,
            confidence = 0.95f,
            isModelLoaded = true,
            latencyMs = 12L,
            diagnosticInfo = "Test Safe"
        )

        val decision = engine.evaluate(safeResult, SensitivityLevel.BALANCED, isStrictEverything = false)
        assertFalse(decision.shouldBlock)
        assertEquals(ThreatCategory.BENIGN_SAFE, decision.category)
    }

    @Test
    fun `decision engine blocks explicit adult content immediately`() {
        val engine = ThreatDecisionEngine()
        val explicitResult = ClassificationResult(
            safeScore = 0.05f,
            suggestiveScore = 0.05f,
            explicitScore = 0.90f,
            topCategory = ThreatCategory.EXPLICIT_ADULT,
            confidence = 0.90f,
            isModelLoaded = true,
            latencyMs = 15L,
            diagnosticInfo = "Test Explicit"
        )

        val decision = engine.evaluate(explicitResult, SensitivityLevel.BALANCED, isStrictEverything = false)
        assertTrue(decision.shouldBlock)
        assertEquals(ThreatCategory.EXPLICIT_ADULT, decision.category)
        assertEquals(ThreatSeverity.CRITICAL, decision.severity)
    }

    @Test
    fun `decision engine blocks high confidence suggestive content`() {
        val engine = ThreatDecisionEngine()
        val suggestiveResult = ClassificationResult(
            safeScore = 0.10f,
            suggestiveScore = 0.88f,
            explicitScore = 0.02f,
            topCategory = ThreatCategory.SUGGESTIVE_CONTENT,
            confidence = 0.88f,
            isModelLoaded = true,
            latencyMs = 14L,
            diagnosticInfo = "Test Suggestive"
        )

        val decision = engine.evaluate(suggestiveResult, SensitivityLevel.BALANCED, isStrictEverything = false)
        assertTrue(decision.shouldBlock)
        assertEquals(ThreatCategory.SUGGESTIVE_CONTENT, decision.category)
    }

    @Test
    fun `decision engine requires consecutive confirmation for borderline suggestive cases`() {
        val engine = ThreatDecisionEngine()
        val borderlineResult = ClassificationResult(
            safeScore = 0.18f,
            suggestiveScore = 0.81f, // Just over balanced threshold 0.80, but below immediate 0.85
            explicitScore = 0.01f,
            topCategory = ThreatCategory.SUGGESTIVE_CONTENT,
            confidence = 0.81f,
            isModelLoaded = true,
            latencyMs = 14L,
            diagnosticInfo = "Test Borderline"
        )

        // First frame: held for consecutive confirmation
        val firstDecision = engine.evaluate(borderlineResult, SensitivityLevel.BALANCED, isStrictEverything = false)
        assertFalse(firstDecision.shouldBlock)

        // Second consecutive frame: confirmed violation
        val secondDecision = engine.evaluate(borderlineResult, SensitivityLevel.BALANCED, isStrictEverything = false)
        assertTrue(secondDecision.shouldBlock)
        assertEquals(ThreatCategory.SUGGESTIVE_CONTENT, secondDecision.category)
    }

    @Test
    fun `decision engine blocks immediately under strict everything policy`() {
        val engine = ThreatDecisionEngine()
        val borderlineResult = ClassificationResult(
            safeScore = 0.35f,
            suggestiveScore = 0.62f,
            explicitScore = 0.03f,
            topCategory = ThreatCategory.SUGGESTIVE_CONTENT,
            confidence = 0.62f,
            isModelLoaded = true,
            latencyMs = 14L,
            diagnosticInfo = "Test Strict Everything"
        )

        val decision = engine.evaluate(borderlineResult, SensitivityLevel.BALANCED, isStrictEverything = true)
        assertTrue(decision.shouldBlock)
    }

    @Test
    fun `frame preprocessor detects duplicate frames to conserve battery`() {
        val preprocessor = FramePreprocessor(224, 224)
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // First frame: stored as baseline
        val isFirstDuplicate = preprocessor.isFrameDuplicate(bitmap)
        assertFalse(isFirstDuplicate)

        // Second identical frame: flagged as duplicate
        val isSecondDuplicate = preprocessor.isFrameDuplicate(bitmap)
        assertTrue(isSecondDuplicate)
    }

    @Test
    fun `device admin receiver protects critical system packages from suspension`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // System UI exemption
        val suspendedSystemUi = AegisDeviceAdminReceiver.suspendPackageIfDeviceOwner(context, "com.android.systemui")
        assertFalse(suspendedSystemUi)

        // Settings exemption
        val suspendedSettings = AegisDeviceAdminReceiver.suspendPackageIfDeviceOwner(context, "com.android.settings")
        assertFalse(suspendedSettings)

        // Own app exemption
        val suspendedSelf = AegisDeviceAdminReceiver.suspendPackageIfDeviceOwner(context, context.packageName)
        assertFalse(suspendedSelf)
    }

    @Test
    fun `security history repository inserts incidents and marks false positives in Room database`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AegisDatabase.getInstance(context)
        val repo = SecurityHistoryRepository(db.incidentDao())

        repo.clearHistory()
        assertEquals(0, repo.getTotalDetectionsCount().first())

        val incident = SecurityIncident(
            timestampMillis = 1700000000000L,
            packageName = "org.videolan.vlc",
            appDisplayName = "VLC Media Player",
            category = ThreatCategory.EXPLICIT_ADULT,
            severity = ThreatSeverity.CRITICAL,
            confidenceScore = 0.94f,
            actionTaken = "Display Shielded & Home Invoked"
        )

        val id = repo.logIncident(incident)
        assertTrue(id > 0)
        assertEquals(1, repo.getTotalDetectionsCount().first())

        val list = repo.getIncidents().first()
        assertEquals(1, list.size)
        assertFalse(list[0].isReviewedFalsePositive)

        // Mark false positive
        repo.markFalsePositive(id, true)
        val updatedList = repo.getIncidents().first()
        assertTrue(updatedList[0].isReviewedFalsePositive)

        // Clean up
        repo.clearHistory()
        assertEquals(0, repo.getTotalDetectionsCount().first())
    }

    @Test
    fun `model manager reports missing status cleanly when no tflite exists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        val manager = ModelManager(context, prefs)
        manager.clearImportedModel()

        val info = manager.refreshStatus()
        assertFalse(info.isLoaded)
        assertEquals("No .tflite model loaded", info.statusMessage)
    }

    @Test
    fun `protection repository reflects model state honestly even when capture is enabled`() {
        ShadowSettings.setCanDrawOverlays(true)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ProtectionRepository(context)

        repo.setProtectionEnabled(true)
        repo.updateModelInfo(
            ActiveModelInfo(
                isLoaded = false,
                displayName = null,
                sourceLabel = "None",
                statusMessage = "No .tflite model loaded"
            )
        )

        val missingTelemetry = repo.telemetry.value
        assertTrue(missingTelemetry.isProtectionEnabled)
        assertFalse(missingTelemetry.isModelLoaded)
        assertEquals("Screen Capture Active • Model Missing (.tflite)", missingTelemetry.statusMessage)
        assertEquals("No .tflite model loaded", missingTelemetry.modelStatusMessage)

        repo.updateModelInfo(
            ActiveModelInfo(
                isLoaded = true,
                displayName = "nsfw_mobilenet.tflite",
                sourceLabel = "Phone Storage",
                inputWidth = 224,
                inputHeight = 224,
                isQuantizedInput = false,
                outputClassCount = 5,
                inputSpecLabel = "224x224 Float32",
                statusMessage = "Model Ready (5-Class)"
            )
        )

        val loadedTelemetry = repo.telemetry.value
        assertTrue(loadedTelemetry.isModelLoaded)
        assertEquals("Visual Guardian Armed • AI Scanning Active", loadedTelemetry.statusMessage)
        assertEquals("nsfw_mobilenet.tflite", loadedTelemetry.loadedModelName)
        assertEquals(5, loadedTelemetry.modelOutputClasses)
        assertEquals("224x224 Float32", loadedTelemetry.modelInputSpec)
    }

    @Test
    fun `frame preprocessor reconfigures buffer capacity for uint8 and float32 models`() {
        val preprocessor = FramePreprocessor(224, 224, isQuantizedUInt8 = false)
        val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)

        val floatBuffer = preprocessor.preprocessBitmapToTensorBuffer(bitmap)
        assertEquals(224 * 224 * 3 * 4, floatBuffer.capacity())

        preprocessor.configure(width = 299, height = 299, quantizedUInt8 = true)
        val uint8Buffer = preprocessor.preprocessBitmapToTensorBuffer(bitmap)
        assertEquals(299 * 299 * 3, uint8Buffer.capacity())
        assertTrue(preprocessor.isQuantizedUInt8)
    }

    @Test
    fun `preferences enforces 10 hour lockdown cooldown before password and stores forensics`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.clearLockdown()

        assertEquals(10, prefs.lockdownHours.value)
        assertTrue(prefs.waitCooldownBeforePassword.value)
        assertFalse(prefs.isLockdownActive())

        val until = prefs.engageFullDeviceLockdown(
            appName = "Chrome Browser",
            packageName = "com.android.chrome",
            categoryName = "Explicit Adult Content",
            details = "Explicit: 94% (Porn: 90%, Hentai: 4%)"
        )

        assertTrue(until > System.currentTimeMillis())
        assertTrue(prefs.isLockdownActive())
        assertTrue(prefs.getRemainingLockdownMillis() > 9 * 60 * 60 * 1000L)
        assertFalse(prefs.canAcceptPasswordForLockdown())

        val info = prefs.lockdownIncidentInfo.value
        assertEquals("Chrome Browser", info.appName)
        assertEquals("com.android.chrome", info.packageName)
        assertTrue(info.details.contains("94%"))

        prefs.clearLockdown()
        assertFalse(prefs.isLockdownActive())
    }

    @Test
    fun `guardian session locks on leave and blocks system settings without pin authorization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.clearLockdown()
        prefs.setGuardianPin("4321")

        assertTrue(prefs.isGuardianSessionUnlocked.value)
        assertTrue(prefs.isGuardianAuthorizedForSystemSettings())

        prefs.lockGuardianSession()
        assertFalse(prefs.isGuardianSessionUnlocked.value)
        assertFalse(prefs.isGuardianAuthorizedForSystemSettings())

        prefs.unlockGuardianSession()
        assertTrue(prefs.isGuardianSessionUnlocked.value)
        assertTrue(prefs.isGuardianAuthorizedForSystemSettings())
    }

    @Test
    fun `device admin disable requested returns anti-tamper guardian message`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.setGuardianPin("4321")
        prefs.lockGuardianSession()

        val receiver = AegisDeviceAdminReceiver()
        val msg = receiver.onDisableRequested(context, android.content.Intent())
        assertTrue(msg.toString().contains("Guardian Password", ignoreCase = true))
    }

    @Test
    fun `lockdown completes stage 1 awaiting master code or escalates to double time and keeps shield active after both`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.clearLockdown()
        prefs.setShieldDesiredActive(false)
        prefs.setLockdownHours(10)
        prefs.setWaitCooldownBeforePassword(true)
        prefs.setGuardianPin("7890")

        // Stage 1 begins
        prefs.engageFullDeviceLockdown(
            appName = "Browser",
            packageName = "com.android.chrome",
            categoryName = "Explicit Adult Content",
            details = "Explicit detected"
        )
        assertEquals(AegisPreferences.LOCKDOWN_STAGE_INITIAL, prefs.lockdownStage.value)
        assertTrue(prefs.isShieldDesiredActive.value)
        assertFalse(prefs.canAcceptPasswordForLockdown())

        // Simulate Stage 1 timer completion (00:00:00)
        prefs.setLockdownUntilForTesting(System.currentTimeMillis() - 1_000L)
        assertTrue(prefs.isLockdownActive()) // Still locked until Master Code OR Double-Time wait
        assertTrue(prefs.canAcceptPasswordForLockdown())

        // Path A: User does NOT know Master Code -> escalate to Double Time (20h)
        val doubleUntil = prefs.engageDoubleTimeLockdown()
        assertEquals(AegisPreferences.LOCKDOWN_STAGE_DOUBLE_TIME, prefs.lockdownStage.value)
        assertTrue(prefs.isInDoubleTimeStage())
        assertTrue(doubleUntil > System.currentTimeMillis() + 19 * 60 * 60 * 1000L)
        assertTrue(prefs.isLockdownActive())
        assertEquals(20, prefs.getDoubleTimeHours())

        // Simulate Double-Time completion (00:00:00) -> auto-unlocks AND Shield remains ON
        prefs.setLockdownUntilForTesting(System.currentTimeMillis() - 1_000L)
        assertTrue(prefs.completeDoubleTimeIfExpired())
        assertFalse(prefs.isLockdownActive())
        assertEquals(AegisPreferences.LOCKDOWN_STAGE_NONE, prefs.lockdownStage.value)
        assertTrue(prefs.isShieldDesiredActive.value)

        // Path B: Unlock with Master Code -> clears lockdown AND Shield remains ON
        prefs.engageFullDeviceLockdown(
            appName = "Browser",
            packageName = "com.android.chrome",
            categoryName = "Suggestive",
            details = "Suggestive detected"
        )
        prefs.setLockdownUntilForTesting(System.currentTimeMillis() - 1_000L)
        assertTrue(prefs.verifyGuardianPin("7890"))
        prefs.clearLockdown()
        assertFalse(prefs.isLockdownActive())
        assertTrue(prefs.isShieldDesiredActive.value)
    }

    @Test
    fun `advanced precision lab supports exact minute specificity, double-time multiplier, and reset`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.clearLockdown()
        prefs.resetAdvancedCustomizations()

        assertFalse(prefs.advancedOptionsUnlocked.value)
        prefs.setAdvancedOptionsUnlocked(true)
        assertTrue(prefs.advancedOptionsUnlocked.value)

        // Exact minute-level lockdown: 15 minutes with 3x double-time multiplier = 45m
        prefs.setLockdownDurationMinutes(15)
        prefs.setDoubleTimeMultiplier(3.0f)
        assertEquals(15, prefs.lockdownDurationMinutes.value)
        assertEquals(45, prefs.getDoubleTimeDurationMinutes())
        assertEquals("15m", AegisPreferences.formatMinutesLabel(15))
        assertEquals("2h 30m", AegisPreferences.formatMinutesLabel(150))

        val untilStage1 = prefs.engageFullDeviceLockdown("VLC", "org.videolan.vlc", "Suggestive", "Custom timer test")
        val remainingStage1 = untilStage1 - System.currentTimeMillis()
        assertTrue(remainingStage1 in (14 * 60_000L)..(15 * 60_000L + 5_000L))

        val untilDouble = prefs.engageDoubleTimeLockdown()
        val remainingDouble = untilDouble - System.currentTimeMillis()
        assertTrue(remainingDouble in (44 * 60_000L)..(45 * 60_000L + 5_000L))

        prefs.clearLockdown()
        prefs.resetAdvancedCustomizations()
        assertEquals(600, prefs.lockdownDurationMinutes.value)
        assertEquals(2.0f, prefs.doubleTimeMultiplier.value)
        assertEquals(350, prefs.scanIntervalMs.value)
        assertEquals(PowerMode.ADAPTIVE_ECO, prefs.powerMode.value)
    }

    @Test
    fun `decision engine honors custom explicit threshold and consecutive frame count overrides`() {
        val engine = ThreatDecisionEngine()
        val borderlineSuggestive = ClassificationResult(
            safeScore = 0.25f,
            suggestiveScore = 0.72f,
            explicitScore = 0.03f,
            topCategory = ThreatCategory.SUGGESTIVE_CONTENT,
            confidence = 0.72f,
            isModelLoaded = true,
            latencyMs = 10L,
            diagnosticInfo = "Custom threshold test"
        )

        // Custom suggestive threshold 0.70f + 3 consecutive frames required
        val f1 = engine.evaluate(borderlineSuggestive, SensitivityLevel.BALANCED, false, 0.75f, 0.70f, 3)
        val f2 = engine.evaluate(borderlineSuggestive, SensitivityLevel.BALANCED, false, 0.75f, 0.70f, 3)
        val f3 = engine.evaluate(borderlineSuggestive, SensitivityLevel.BALANCED, false, 0.75f, 0.70f, 3)
        assertFalse(f1.shouldBlock)
        assertFalse(f2.shouldBlock)
        assertTrue(f3.shouldBlock)
    }

    @Test
    fun `ahsantcr branding opens external contact url cleanly`() {
        assertEquals("https://ahsantcr.lovable.app", AHSAN_TCR_CONTACT_URL)
        assertEquals("ahsantcr.lovable.app", AHSAN_TCR_CONTACT_DOMAIN)

        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        openAhsanTcrContact(app)

        val startedIntent = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        assertEquals("https://ahsantcr.lovable.app", startedIntent.dataString)
        assertTrue((startedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun `master key gate blocks invalid keys, authorizes valid keys, creates missing key, and handles cancellation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.removeGuardianPin()

        val gate = MasterKeyGateState(prefs)

        // 1. When no Master Key exists yet, enforce 4-8 digit creation first
        var executedCount = 0
        gate.request("turn Shield ON") { executedCount++ }
        assertEquals("turn Shield ON", gate.pendingActionLabel)

        val shortErr = gate.submitKey("12")
        assertNotNull(shortErr)
        assertEquals(0, executedCount)
        assertFalse(prefs.hasGuardianPin.value)

        val createErr = gate.submitKey("2468")
        assertEquals(null, createErr)
        assertEquals(1, executedCount)
        assertTrue(prefs.hasGuardianPin.value)
        assertEquals(null, gate.pendingActionLabel)

        // 2. When Master Key exists, reject wrong key and accept right key
        gate.request("change setting") { executedCount++ }
        val wrongErr = gate.submitKey("0000")
        assertEquals("Incorrect Master Key", wrongErr)
        assertEquals(1, executedCount)
        assertEquals("change setting", gate.pendingActionLabel)

        val okErr = gate.submitKey("2468")
        assertEquals(null, okErr)
        assertEquals(2, executedCount)
        assertEquals(null, gate.pendingActionLabel)

        // 3. Cancellation invokes onCancel and does not authorize action
        var cancelledCount = 0
        gate.request(
            actionLabel = "update threshold",
            onCancel = { cancelledCount++ }
        ) { executedCount++ }
        gate.dismiss()
        assertEquals(1, cancelledCount)
        assertEquals(2, executedCount)
        assertEquals(null, gate.pendingActionLabel)
    }

    @Test
    fun `anti-tamper guard allows deleting or managing other apps and only locks Sheildlenz`() {
        // Deleting another app in Google/Samsung/AOSP Package Installer must NOT ask for password
        assertFalse(
            AegisAccessibilityService.shouldTriggerAntiTamper(
                packageName = "com.google.android.packageinstaller",
                className = "com.android.packageinstaller.UninstallerActivity",
                screenText = "WhatsApp Do you want to uninstall this app? Cancel OK"
            )
        )
        assertFalse(
            AegisAccessibilityService.shouldTriggerAntiTamper(
                packageName = "com.samsung.android.packageinstaller",
                className = "com.android.packageinstaller.UninstallerActivity",
                screenText = "YouTube Uninstall this application?"
            )
        )

        // Opening App Info / Force Stop / Uninstall for another app in Settings must NOT ask for password
        assertFalse(
            AegisAccessibilityService.shouldTriggerAntiTamper(
                packageName = "com.android.settings",
                className = "com.android.settings.applications.InstalledAppDetailsTop",
                screenText = "App info Instagram Uninstall Force stop Storage & cache"
            )
        )

        // Merely scrolling past Sheildlenz in Settings -> All apps list must NOT trigger password
        assertFalse(
            AegisAccessibilityService.shouldTriggerAntiTamper(
                packageName = "com.android.settings",
                className = "com.android.settings.SubSettings",
                screenText = "All apps Chrome Sheildlenz Spotify Telegram"
            )
        )

        // Uninstalling Sheildlenz MUST trigger password
        assertTrue(
            AegisAccessibilityService.shouldTriggerAntiTamper(
                packageName = "com.google.android.packageinstaller",
                className = "com.android.packageinstaller.UninstallerActivity",
                screenText = "Sheildlenz Do you want to uninstall this app?"
            )
        )

        // Opening Sheildlenz App Info / Force Stop / Device Admin / Accessibility MUST trigger password
        assertTrue(
            AegisAccessibilityService.shouldTriggerAntiTamper(
                packageName = "com.android.settings",
                className = "com.android.settings.applications.InstalledAppDetailsTop",
                screenText = "App info Sheildlenz Uninstall Force stop"
            )
        )
        assertTrue(
            AegisAccessibilityService.shouldTriggerAntiTamper(
                packageName = "com.android.settings",
                className = "com.android.settings.applications.specialaccess.deviceadmin.DeviceAdminAdd",
                screenText = "Sheildlenz Deactivate this device admin app"
            )
        )
        assertTrue(
            AegisAccessibilityService.shouldTriggerAntiTamper(
                packageName = "com.android.settings",
                className = "com.android.settings.SubSettings",
                screenText = "Sheildlenz Use Sheildlenz Sheildlenz shortcut"
            )
        )
    }

    @Test
    fun `startup gate always requires guardian password setup or unlock and re-locks on background`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.removeGuardianPin()

        // Fresh start without PIN: must NOT skip startup password gate
        assertFalse(
            isStartupGuardianAuthorized(
                hasGuardianPin = prefs.hasGuardianPin.value,
                isSessionUnlocked = prefs.isGuardianSessionUnlocked.value
            )
        )

        // Setting initial Guardian Password unlocks the startup session
        prefs.setGuardianPin("2580")
        assertTrue(
            isStartupGuardianAuthorized(
                hasGuardianPin = prefs.hasGuardianPin.value,
                isSessionUnlocked = prefs.isGuardianSessionUnlocked.value
            )
        )

        // Leaving app / next start resets session unlock so password is required again
        prefs.onAppBackgrounded()
        assertFalse(
            isStartupGuardianAuthorized(
                hasGuardianPin = prefs.hasGuardianPin.value,
                isSessionUnlocked = prefs.isGuardianSessionUnlocked.value
            )
        )

        // Wrong password fails; valid password unlocks startup session
        assertFalse(prefs.verifyGuardianPin("1111"))
        assertTrue(prefs.verifyGuardianPin("2580"))
        prefs.unlockGuardianSession()
        assertTrue(
            isStartupGuardianAuthorized(
                hasGuardianPin = prefs.hasGuardianPin.value,
                isSessionUnlocked = prefs.isGuardianSessionUnlocked.value
            )
        )
    }

    @Test
    fun `authorized external picker flow preserves unlock across single background hop then relocks normally`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)
        prefs.setGuardianPin("2580")
        assertTrue(prefs.isGuardianSessionUnlocked.value)

        // Launching .tflite file picker after Master Key verification keeps session unlocked across picker hop
        prefs.beginAuthorizedExternalFlow()
        prefs.onAppBackgrounded()
        assertTrue(prefs.isGuardianSessionUnlocked.value)

        // Subsequent normal app backgrounding (after returning from picker) re-locks Guardian session
        prefs.onAppBackgrounded()
        assertFalse(prefs.isGuardianSessionUnlocked.value)
    }

    @Test
    fun `power mode presets configure scan interval, capture width, duplicate skipping, and screen-off pause`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)

        prefs.setPowerMode(PowerMode.POWER_SAVING)
        assertEquals(PowerMode.POWER_SAVING, prefs.powerMode.value)
        assertEquals(750, prefs.scanIntervalMs.value)
        assertEquals(256, prefs.captureWidthPx.value)
        assertTrue(prefs.skipDuplicateFrames.value)
        assertTrue(prefs.pauseWhenScreenOff.value)
        assertTrue(prefs.adaptiveCadenceEnabled.value)

        prefs.setPowerMode(PowerMode.EXTREME)
        assertEquals(PowerMode.EXTREME, prefs.powerMode.value)
        assertEquals(120, prefs.scanIntervalMs.value)
        assertEquals(640, prefs.captureWidthPx.value)
        assertFalse(prefs.skipDuplicateFrames.value)
        assertFalse(prefs.pauseWhenScreenOff.value)
        assertFalse(prefs.adaptiveCadenceEnabled.value)

        // Granular power-saving options toggle cleanly
        prefs.setCompactPowerResolution(true)
        assertEquals(256, prefs.captureWidthPx.value)
        prefs.setPauseWhenScreenOff(true)
        assertTrue(prefs.pauseWhenScreenOff.value)
        prefs.setAutoThrottleLowBattery(false)
        assertFalse(prefs.autoThrottleLowBattery.value)

        // Reset restores Adaptive Eco defaults
        prefs.resetAdvancedCustomizations()
        assertEquals(PowerMode.ADAPTIVE_ECO, prefs.powerMode.value)
        assertEquals(350, prefs.scanIntervalMs.value)
        assertEquals(256, prefs.captureWidthPx.value)
        assertTrue(prefs.skipDuplicateFrames.value)
        assertTrue(prefs.pauseWhenScreenOff.value)
        assertTrue(prefs.adaptiveCadenceEnabled.value)
        assertTrue(prefs.autoThrottleLowBattery.value)
    }

    @Test
    fun `whitelisted packages add and remove rules correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)

        // Clear first
        prefs.setWhitelistedPackages(emptySet())
        assertTrue(prefs.whitelistedPackages.value.isEmpty())

        prefs.addWhitelistedPackage("com.whatsapp")
        assertTrue(prefs.isPackageWhitelisted("com.whatsapp"))
        assertTrue(prefs.whitelistedPackages.value.contains("com.whatsapp"))

        prefs.addWhitelistedPackage("com.google.android.youtube")
        assertTrue(prefs.isPackageWhitelisted("com.google.android.youtube"))
        assertEquals(2, prefs.whitelistedPackages.value.size)

        prefs.removeWhitelistedPackage("com.whatsapp")
        assertFalse(prefs.isPackageWhitelisted("com.whatsapp"))
        assertTrue(prefs.isPackageWhitelisted("com.google.android.youtube"))
        assertEquals(1, prefs.whitelistedPackages.value.size)

        prefs.setWhitelistedPackages(emptySet())
        assertFalse(prefs.isPackageWhitelisted("com.google.android.youtube"))
    }

    @Test
    fun `security health score computes correct scores and status descriptions`() {
        // 1. All layers active = 100% Immune
        val immuneReport = SecurityHealthReport.compute(
            isShieldRunning = true,
            hasOverlay = true,
            hasAccessibility = true,
            isDeviceAdmin = true,
            hasGuardianPin = true,
            hasBatteryExemption = true,
            isModelLoaded = true
        )
        assertEquals(100, immuneReport.score)
        assertTrue(immuneReport.statusTitle.contains("Immune"))

        // 2. Core shield & accessibility only = 55%
        val partialReport = SecurityHealthReport.compute(
            isShieldRunning = true,
            hasOverlay = true,
            hasAccessibility = true,
            isDeviceAdmin = false,
            hasGuardianPin = false,
            hasBatteryExemption = false,
            isModelLoaded = false
        )
        assertEquals(55, partialReport.score)
        assertTrue(partialReport.statusTitle.contains("Partially"))

        // 3. Everything off = 0%
        val vulnerableReport = SecurityHealthReport.compute(
            isShieldRunning = false,
            hasOverlay = false,
            hasAccessibility = false,
            isDeviceAdmin = false,
            hasGuardianPin = false,
            hasBatteryExemption = false,
            isModelLoaded = false
        )
        assertEquals(0, vulnerableReport.score)
        assertTrue(vulnerableReport.statusTitle.contains("Vulnerable"))
    }

    @Test
    fun `threat analytics calculates weekly trend, peak window, and category distribution`() {
        val now = System.currentTimeMillis()
        val incidents = listOf(
            SecurityIncident(
                id = 1L,
                timestampMillis = now,
                packageName = "com.sample.browser",
                appDisplayName = "Sample Browser",
                category = ThreatCategory.EXPLICIT_ADULT,
                severity = ThreatSeverity.CRITICAL,
                confidenceScore = 0.95f,
                actionTaken = "Block"
            ),
            SecurityIncident(
                id = 2L,
                timestampMillis = now,
                packageName = "com.sample.browser",
                appDisplayName = "Sample Browser",
                category = ThreatCategory.SUGGESTIVE_CONTENT,
                severity = ThreatSeverity.HIGH,
                confidenceScore = 0.85f,
                actionTaken = "Block"
            )
        )

        val analytics = SecurityHistoryRepository.computeAnalytics(incidents)
        assertEquals(2, analytics.totalDetections)
        assertEquals(1, analytics.explicitAdultCount)
        assertEquals(1, analytics.suggestiveCount)
        assertEquals(7, analytics.weeklyTrend.size)
        assertEquals(2, analytics.weeklyTrend.last().count)
        assertTrue(analytics.weeklyTrend.last().isToday)
        assertEquals("Sample Browser", analytics.peakAppDisplayName)
        assertNotNull(analytics.peakHourWindow)

        val report = buildAuditReportText(analytics = analytics, totalDetections = 2, blockedAppCount = 1)
        assertTrue(report.contains("Sheildlenz Offline Security Audit Report"))
        assertTrue(report.contains("Total Blocked Detections: 2"))
        assertTrue(report.contains("Explicit Adult: 1"))
        assertTrue(report.contains("Most Flagged App: Sample Browser"))
    }

    @Test
    fun `update checker targets Ahsanpcin Sheild-Lenz and extracts APK release asset accurately`() {
        assertEquals("Ahsanpcin", UpdateChecker.GITHUB_OWNER)
        assertEquals("Sheild-Lenz", UpdateChecker.GITHUB_REPO)

        assertTrue(UpdateChecker.compareVersions("5.1.0", "4.68") > 0)
        assertTrue(UpdateChecker.compareVersions("4.69", "4.68") > 0)
        assertEquals(0, UpdateChecker.compareVersions("4.68", "4.68"))
        assertTrue(UpdateChecker.compareVersions("4.67", "4.68") < 0)

        val fakeRelease = JSONObject().apply {
            put("tag_name", "v5.0.0")
            put("html_url", "https://github.com/Ahsanpcin/Sheild-Lenz/releases/tag/v5.0.0")
            put("body", "In-app APK updater & model zip downloader")
            put(
                "assets",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("name", "Sheildlenz-v5.0.0.apk")
                            put(
                                "browser_download_url",
                                "https://github.com/Ahsanpcin/Sheild-Lenz/releases/download/v5.0.0/Sheildlenz-v5.0.0.apk"
                            )
                            put("size", 18_500_000L)
                        }
                    )
                }
            )
        }

        val info = UpdateChecker.parseReleaseJson(fakeRelease, currentVersionName = "4.68")
        assertTrue(info.isUpdateAvailable)
        assertEquals("v5.0.0", info.latestVersion)
        assertEquals(
            "https://github.com/Ahsanpcin/Sheild-Lenz/releases/download/v5.0.0/Sheildlenz-v5.0.0.apk",
            info.apkDownloadUrl
        )
        assertEquals(18_500_000L, info.apkSizeBytes)
    }

    @Test
    fun `model downloader extracts nested tflite safely and rejects path traversal entries`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val zipFile = File(context.cacheDir, "test_model_bundle.zip")
        val destFile = File(context.cacheDir, "test_extracted.tflite")
        if (zipFile.exists()) zipFile.delete()
        if (destFile.exists()) destFile.delete()

        val fakeTfliteBytes = ByteArray(2048) { (it % 127).toByte() }
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // Malicious traversal entry must be skipped
            zos.putNextEntry(ZipEntry("../../evil.tflite"))
            zos.write("bad".toByteArray())
            zos.closeEntry()

            // macOS resource fork metadata entry must be skipped
            zos.putNextEntry(ZipEntry("__MACOSX/._nsfw_mobilenet_v2_140_224.tflite"))
            zos.write("meta".toByteArray())
            zos.closeEntry()

            // Valid nested .tflite model entry
            zos.putNextEntry(ZipEntry("models/nsfw_mobilenet_v2_140_224.tflite"))
            zos.write(fakeTfliteBytes)
            zos.closeEntry()
        }

        val extractedName = ModelDownloader.extractTfliteFromZip(zipFile, destFile)
        assertEquals("nsfw_mobilenet_v2_140_224.tflite", extractedName)
        assertTrue(destFile.exists())
        assertEquals(2048L, destFile.length())

        zipFile.delete()
        destFile.delete()
    }

    @Test
    fun `scheduled protection hours evaluates same-day and overnight windows accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AegisPreferences(context)

        // Overnight window: 9:00 PM (1260) -> 7:00 AM (420)
        prefs.setScheduleWindow(21 * 60, 7 * 60)
        prefs.setScheduleEnabled(true)

        assertTrue(prefs.isWithinScheduledWindow(22 * 60)) // 10:00 PM
        assertTrue(prefs.isWithinScheduledWindow(2 * 60))  // 2:00 AM
        assertFalse(prefs.isWithinScheduledWindow(12 * 60)) // 12:00 PM

        // Same-day window: 2:00 PM (840) -> 6:00 PM (1080)
        prefs.setScheduleWindow(14 * 60, 18 * 60)
        assertTrue(prefs.isWithinScheduledWindow(15 * 60))  // 3:00 PM
        assertFalse(prefs.isWithinScheduledWindow(20 * 60)) // 8:00 PM

        prefs.setScheduleEnabled(false)
        assertFalse(prefs.isWithinScheduledWindow(15 * 60))

        assertEquals("9:00 PM", AegisPreferences.formatClockTime(21 * 60))
        assertEquals("7:00 AM", AegisPreferences.formatClockTime(7 * 60))
        assertEquals("12:00 AM", AegisPreferences.formatClockTime(0))
    }

    @Test
    fun `adaptive eco cadence steps down on idle screens and auto-throttles below 20 percent battery`() {
        // Active frames keep responsive base interval (350ms)
        val (activeMs, lowActive) = AegisPreferences.computeEffectiveScanIntervalMs(
            baseIntervalMs = 350,
            adaptiveEnabled = true,
            consecutiveIdleOrSafeCount = 1,
            autoThrottleLowBattery = true,
            batteryPercent = 80,
            isCharging = false,
            isPowerSaveMode = false,
            wasLowBatteryThrottled = false
        )
        assertEquals(350, activeMs)
        assertFalse(lowActive)

        // Consecutive unchanged screens step interval down cleanly
        val (idleMs, _) = AegisPreferences.computeEffectiveScanIntervalMs(
            baseIntervalMs = 350,
            adaptiveEnabled = true,
            consecutiveIdleOrSafeCount = 15,
            autoThrottleLowBattery = true,
            batteryPercent = 80,
            isCharging = false,
            isPowerSaveMode = false,
            wasLowBatteryThrottled = false
        )
        assertEquals(1575, idleMs)

        // Battery drops to 19% unplugged -> low-battery throttle activates (>=1000ms floor)
        val (throttledMs, isThrottled19) = AegisPreferences.computeEffectiveScanIntervalMs(
            baseIntervalMs = 350,
            adaptiveEnabled = true,
            consecutiveIdleOrSafeCount = 0,
            autoThrottleLowBattery = true,
            batteryPercent = 19,
            isCharging = false,
            isPowerSaveMode = false,
            wasLowBatteryThrottled = false
        )
        assertTrue(isThrottled19)
        assertTrue(throttledMs >= 1000)

        // Hysteresis keeps throttle engaged at 21%, releases at 22%, and immediately releases when charging
        val (_, stillThrottled21) = AegisPreferences.computeEffectiveScanIntervalMs(
            baseIntervalMs = 350,
            adaptiveEnabled = true,
            consecutiveIdleOrSafeCount = 0,
            autoThrottleLowBattery = true,
            batteryPercent = 21,
            isCharging = false,
            isPowerSaveMode = false,
            wasLowBatteryThrottled = true
        )
        assertTrue(stillThrottled21)

        val (releasedMs22, released22) = AegisPreferences.computeEffectiveScanIntervalMs(
            baseIntervalMs = 350,
            adaptiveEnabled = true,
            consecutiveIdleOrSafeCount = 0,
            autoThrottleLowBattery = true,
            batteryPercent = 22,
            isCharging = false,
            isPowerSaveMode = false,
            wasLowBatteryThrottled = true
        )
        assertFalse(released22)
        assertEquals(350, releasedMs22)

        val (chargingMs, throttledCharging) = AegisPreferences.computeEffectiveScanIntervalMs(
            baseIntervalMs = 350,
            adaptiveEnabled = true,
            consecutiveIdleOrSafeCount = 0,
            autoThrottleLowBattery = true,
            batteryPercent = 12,
            isCharging = true,
            isPowerSaveMode = true,
            wasLowBatteryThrottled = true
        )
        assertFalse(throttledCharging)
        assertEquals(350, chargingMs)
    }

    @Test
    fun `perceptual luma skip ignores minor shimmer but triggers on real visual changes`() {
        val preprocessor = FramePreprocessor(224, 224)
        val frame1 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        frame1.eraseColor(android.graphics.Color.rgb(40, 80, 120))
        assertFalse(preprocessor.isFrameDuplicate(frame1))

        // Tiny ambient change (+2 RGB) is treated as static to save battery
        val nearStatic = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        nearStatic.eraseColor(android.graphics.Color.rgb(42, 82, 121))
        assertTrue(preprocessor.isFrameDuplicate(nearStatic))

        // Real visual scene change must trigger inspection immediately
        val sceneChange = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        sceneChange.eraseColor(android.graphics.Color.rgb(220, 210, 200))
        assertFalse(preprocessor.isFrameDuplicate(sceneChange))

        preprocessor.recyclePools()
    }

    @Test
    fun `zero cost skip packages and telemetry eco percentage calculate accurately`() {
        assertTrue(AegisProtectionService.isZeroCostSkipPackage("com.ahsan.tcr.sheildlenz", "com.ahsan.tcr.sheildlenz"))
        assertTrue(AegisProtectionService.isZeroCostSkipPackage("com.android.systemui", "com.ahsan.tcr.sheildlenz"))
        assertTrue(AegisProtectionService.isZeroCostSkipPackage("com.google.android.apps.nexuslauncher", "com.ahsan.tcr.sheildlenz"))
        assertFalse(AegisProtectionService.isZeroCostSkipPackage("com.android.chrome", "com.ahsan.tcr.sheildlenz"))

        val telemetry = EngineTelemetry(framesProcessedCount = 30L, framesSkippedCount = 70L)
        assertEquals(70, telemetry.ecoSavedPercent())
    }
}

