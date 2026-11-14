package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.models.EngineTelemetry
import com.example.core.models.SensitivityLevel
import com.example.detection.classifier.ModelDownloadState
import com.example.detection.classifier.ModelDownloader
import com.example.detection.classifier.ModelImportResult
import com.example.detection.classifier.ModelManager
import com.example.detection.classifier.SampleImageTester
import com.example.detection.classifier.SampleTestResult
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderLight
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSecondaryBackground
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ModelManagerCard(
    telemetry: EngineTelemetry,
    modelManager: ModelManager,
    onModelUpdated: () -> Unit,
    modifier: Modifier = Modifier,
    cardTone: CyberCardTone? = null,
    showCloudDownloader: Boolean = true,
    onBeforeOpenPicker: () -> Unit = {},
    onRequireAuth: (actionLabel: String, onAuthorized: () -> Unit) -> Unit = { _, action -> action() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val downloadState by ModelDownloader.downloadState.collectAsState()
    val isDownloadingOrUnzipping = downloadState is ModelDownloadState.Downloading ||
        downloadState is ModelDownloadState.Extracting ||
        downloadState is ModelDownloadState.Validating

    val startCloudDownload: (Int) -> Unit = { linkIndex ->
        localError = null
        ModelDownloader.startDownload(
            context = context,
            modelManager = modelManager,
            linkIndex = linkIndex,
            onCompleted = { onModelUpdated() }
        )
    }

    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            localError = null
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    modelManager.importModelFromUri(uri)
                }
                isImporting = false
                when (result) {
                    is ModelImportResult.Success -> {
                        localError = null
                        onModelUpdated()
                    }
                    is ModelImportResult.Failure -> {
                        localError = result.reason
                        onModelUpdated()
                    }
                }
            }
        }
    }

    val errorToShow = localError ?: telemetry.modelErrorMessage
    val accent = when {
        errorToShow != null -> AegisWarningAmber
        telemetry.isModelLoaded -> AegisPrimaryCyan
        else -> AegisChampagneGold
    }
    val resolvedTone = cardTone ?: if (telemetry.isModelLoaded && errorToShow == null) {
        CyberCardTone.SKY
    } else {
        CyberCardTone.GOLD
    }

    CyberCard(
        modifier = modifier,
        borderColor = accent.copy(alpha = 0.45f),
        tone = resolvedTone
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(accent, AegisPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AI Model Engine (.tflite)",
                        color = AegisTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = telemetry.loadedModelName ?: "No model file attached",
                        color = if (telemetry.isModelLoaded) AegisPrimaryCyan else AegisWarningAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }

            StatusPill(
                label = when {
                    errorToShow != null -> "ERROR"
                    telemetry.isModelLoaded -> "READY"
                    else -> "MISSING"
                },
                isActive = telemetry.isModelLoaded && errorToShow == null,
                activeColor = AegisPrimaryCyan,
                inactiveColor = AegisWarningAmber
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (telemetry.isModelLoaded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AegisLavenderSoft.copy(alpha = 0.72f))
                    .border(1.dp, AegisLavenderBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                ModelMetaRow("Input Tensor", telemetry.modelInputSpec)
                HorizontalDivider(color = AegisLavenderBorder.copy(alpha = 0.65f), modifier = Modifier.padding(vertical = 4.dp))
                ModelMetaRow(
                    "Class Mapping",
                    when (telemetry.modelOutputClasses) {
                        2 -> "2-Class (Safe / NSFW)"
                        3 -> "3-Class (Safe / Suggestive / Explicit)"
                        5 -> "5-Class (NSFW MobileNet)"
                        else -> "${telemetry.modelOutputClasses}-Class"
                    }
                )
            }
        } else {
            Text(
                text = "Pick a 2/3/5-class .tflite file from phone storage.",
                color = AegisTextSecondary,
                fontSize = 12.sp,
                fontFamily = ClassicalBodyFont
            )
        }

        AnimatedVisibility(visible = errorToShow != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AegisChampagneSoft)
                    .border(1.dp, AegisGoldBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AegisWarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorToShow ?: "",
                        color = AegisTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        if (showCloudDownloader) {
            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(visible = isDownloadingOrUnzipping) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AegisSurfaceElevated)
                        .border(1.dp, AegisCardBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    when (val state = downloadState) {
                        is ModelDownloadState.Downloading -> {
                            val pctLabel = state.progress?.let { "${(it * 100).toInt()}%" } ?: "..."
                            val sizeLabel = if (state.totalBytes > 0L) {
                                "${ModelDownloader.formatBytesMb(state.bytesDownloaded)} / ${ModelDownloader.formatBytesMb(state.totalBytes)}"
                            } else {
                                ModelDownloader.formatBytesMb(state.bytesDownloaded)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading Link ${state.linkIndex} • $pctLabel",
                                    color = AegisPrimaryCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ClassicalBodyFont
                                )
                                Text(
                                    text = sizeLabel,
                                    color = AegisTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.progress != null) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = AegisPrimaryCyan,
                                    trackColor = AegisPrimaryCyan.copy(alpha = 0.18f),
                                    strokeCap = StrokeCap.Round
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = AegisPrimaryCyan,
                                    trackColor = AegisPrimaryCyan.copy(alpha = 0.18f)
                                )
                            }
                        }
                        ModelDownloadState.Extracting -> {
                            Text(
                                text = "Unzipping .tflite model automatically...",
                                color = AegisPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = AegisPurple,
                                trackColor = AegisPurple.copy(alpha = 0.18f)
                            )
                        }
                        ModelDownloadState.Validating -> {
                            Text(
                                text = "Validating & loading AI Classifier...",
                                color = AegisChampagneGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = AegisChampagneGold,
                                trackColor = AegisChampagneGold.copy(alpha = 0.18f)
                            )
                        }
                        else -> {}
                    }
                }
            }

            Button(
                onClick = {
                    if (isDownloadingOrUnzipping) {
                        ModelDownloader.cancelDownload()
                    } else if (telemetry.isModelLoaded) {
                        onRequireAuth("download & replace AI model") {
                            startCloudDownload(1)
                        }
                    } else {
                        startCloudDownload(1)
                    }
                },
                enabled = !isImporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDownloadingOrUnzipping) AegisWarningAmber else AegisPrimaryCyan,
                    contentColor = AegisOnPrimaryWhite
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("download_tflite_model_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isDownloadingOrUnzipping -> "CANCEL DOWNLOAD"
                        telemetry.isModelLoaded -> "REFRESH MODEL (LINK 1 • GITHUB)"
                        else -> "DOWNLOAD AI MODEL (LINK 1)"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont,
                    letterSpacing = 0.6.sp
                )
            }

            if (!isDownloadingOrUnzipping) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        if (telemetry.isModelLoaded) {
                            onRequireAuth("download AI model from Link 2") {
                                startCloudDownload(2)
                            }
                        } else {
                            startCloudDownload(2)
                        }
                    },
                    enabled = !isImporting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AegisChampagneSoft.copy(alpha = 0.55f),
                        contentColor = AegisChampagneGold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("download_tflite_link2_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DOWNLOAD VIA LINK 2 (DROPBOX MIRROR)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }

            val failedState = downloadState as? ModelDownloadState.Failed
            AnimatedVisibility(visible = failedState != null) {
                failedState?.let { err ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AegisChampagneSoft)
                            .border(1.dp, AegisGoldBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = err.message,
                            color = AegisTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ClassicalBodyFont
                        )
                        OutlinedButton(
                            onClick = {
                                val fallbackLink = if (err.linkIndex == 1) 2 else 1
                                if (telemetry.isModelLoaded) {
                                    onRequireAuth("download AI model from Link $fallbackLink") {
                                        startCloudDownload(fallbackLink)
                                    }
                                } else {
                                    startCloudDownload(fallbackLink)
                                }
                            },
                            enabled = !isDownloadingOrUnzipping && !isImporting,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AegisOnPrimaryWhite.copy(alpha = 0.6f),
                                contentColor = AegisChampagneGold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_tflite_retry_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (err.linkIndex == 1) "TRY LINK 2 (DROPBOX MIRROR)" else "RETRY LINK 1 (GITHUB)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (telemetry.isModelLoaded) {
                        onRequireAuth("replace AI model") {
                            onBeforeOpenPicker()
                            modelPickerLauncher.launch(arrayOf("*/*"))
                        }
                    } else {
                        onBeforeOpenPicker()
                        modelPickerLauncher.launch(arrayOf("*/*"))
                    }
                },
                enabled = !isImporting && !isDownloadingOrUnzipping,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (telemetry.isModelLoaded) AegisPurple else AegisPrimaryCyan,
                    contentColor = AegisOnPrimaryWhite
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("pick_tflite_model_button")
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AegisOnPrimaryWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VALIDATING...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        letterSpacing = 0.8.sp
                    )
                } else {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (telemetry.isModelLoaded) "REPLACE .TFLITE" else "LOAD .TFLITE FROM PHONE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalBodyFont,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            if (telemetry.isModelLoaded && modelManager.activeModelFile.exists()) {
                OutlinedButton(
                    onClick = {
                        onRequireAuth("remove AI model") {
                            modelManager.clearImportedModel()
                            localError = null
                            ModelDownloader.clearState()
                            onModelUpdated()
                        }
                    },
                    enabled = !isDownloadingOrUnzipping,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AegisChampagneSoft,
                        contentColor = AegisWarningAmber
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("remove_tflite_model_button")
                ) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Remove model", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SampleImageTesterCard(
    telemetry: EngineTelemetry,
    modelManager: ModelManager,
    sensitivity: SensitivityLevel,
    isStrictEverything: Boolean,
    modifier: Modifier = Modifier,
    cardTone: CyberCardTone = CyberCardTone.LAVENDER,
    onBeforeOpenPicker: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<SampleTestResult?>(null) }
    var testError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isTesting = true
            testError = null
            scope.launch {
                val res = withContext(Dispatchers.Default) {
                    SampleImageTester.testImageUri(
                        context = context,
                        uri = uri,
                        modelManager = modelManager,
                        sensitivity = sensitivity,
                        isStrictEverything = isStrictEverything
                    )
                }
                isTesting = false
                res.onSuccess { result ->
                    testResult = result
                    testError = null
                }.onFailure { err ->
                    testError = err.message ?: "Inference failed on selected image"
                }
            }
        }
    }

    CyberCard(
        modifier = modifier,
        borderColor = AegisLavenderBorder,
        tone = cardTone
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(AegisPurple, AegisLavenderLight)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ImageSearch,
                        contentDescription = null,
                        tint = AegisOnPrimaryWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Sample Image Tester",
                        color = AegisTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ClassicalDisplayFont
                    )
                    Text(
                        text = "Offline photo check",
                        color = AegisPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ClassicalBodyFont
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                onBeforeOpenPicker()
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            enabled = telemetry.isModelLoaded && !isTesting,
            colors = ButtonDefaults.buttonColors(
                containerColor = AegisPurple,
                contentColor = AegisOnPrimaryWhite,
                disabledContainerColor = AegisLavenderLight.copy(alpha = 0.45f),
                disabledContentColor = AegisTextPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("test_sample_image_button")
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AegisOnPrimaryWhite, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RUNNING INFERENCE...", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = ClassicalBodyFont)
            } else {
                Icon(imageVector = Icons.Default.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (telemetry.isModelLoaded) "PICK IMAGE & RUN MODEL TEST" else "LOAD A .TFLITE MODEL FIRST",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ClassicalBodyFont,
                    letterSpacing = 0.6.sp
                )
            }
        }

        AnimatedVisibility(visible = testError != null) {
            Text(
                text = testError ?: "",
                color = AegisWarningAmber,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        AnimatedVisibility(visible = testResult != null) {
            testResult?.let { result ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AegisSecondaryBackground.copy(alpha = 0.85f))
                        .border(1.dp, AegisCardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = result.previewBitmap.asImageBitmap(),
                            contentDescription = "Sample test thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, AegisLavenderBorder, RoundedCornerShape(12.dp))
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            StatusPill(
                                label = if (result.decision.shouldBlock) "WOULD BLOCK" else "ALLOWED (SAFE)",
                                isActive = true,
                                activeColor = if (result.decision.shouldBlock) AegisChampagneGold else AegisPrimaryCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Latency: ${result.classification.latencyMs} ms • Src: ${result.originalDimensions}",
                                color = AegisTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = result.decision.reason,
                                color = AegisTextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ScoreProgressBar("Safe", result.classification.safeScore, AegisPrimaryCyan)
                    ScoreProgressBar("Suggestive", result.classification.suggestiveScore, AegisPurple)
                    ScoreProgressBar("Explicit", result.classification.explicitScore, AegisChampagneGold)

                    if (result.classification.rawClassScores.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val rawSummary = result.classification.rawClassScores.entries.joinToString(" • ") { (k, v) ->
                            "$k: ${(v * 100).toInt()}%"
                        }
                        Text(
                            text = rawSummary,
                            color = AegisTextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AegisTextSecondary, fontSize = 12.sp, fontFamily = ClassicalBodyFont)
        Text(
            text = value,
            color = AegisTextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ScoreProgressBar(label: String, score: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = AegisTextSecondary, fontSize = 12.sp, fontFamily = ClassicalBodyFont, fontWeight = FontWeight.Medium)
            Text(
                text = "${(score * 100).toInt()}%",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { score.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.14f),
            strokeCap = StrokeCap.Round
        )
    }
}
