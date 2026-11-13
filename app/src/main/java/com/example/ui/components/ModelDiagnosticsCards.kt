package com.example.ui.components

import android.graphics.Bitmap
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.detection.classifier.ModelImportResult
import com.example.detection.classifier.ModelManager
import com.example.detection.classifier.SampleImageTester
import com.example.detection.classifier.SampleTestResult
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisDangerRed
import com.example.ui.theme.AegisElectricBlue
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSecondaryBackground
import com.example.ui.theme.AegisSuccessGreen
import com.example.ui.theme.AegisTextMuted
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisVoidBackground
import com.example.ui.theme.AegisWarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ModelManagerCard(
    telemetry: EngineTelemetry,
    modelManager: ModelManager,
    onModelUpdated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

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
    val borderColor = when {
        errorToShow != null -> AegisDangerRed.copy(alpha = 0.45f)
        telemetry.isModelLoaded -> AegisSuccessGreen.copy(alpha = 0.45f)
        else -> AegisWarningAmber.copy(alpha = 0.45f)
    }

    CyberCard(modifier = modifier, borderColor = borderColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            (if (telemetry.isModelLoaded) AegisSuccessGreen else AegisWarningAmber).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = if (telemetry.isModelLoaded) AegisSuccessGreen else AegisWarningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI MODEL ENGINE (.TFLITE)",
                        color = AegisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = telemetry.loadedModelName ?: "No model file attached",
                        color = if (telemetry.isModelLoaded) AegisPrimaryCyan else AegisTextMuted,
                        fontSize = 12.sp
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
                activeColor = AegisSuccessGreen,
                inactiveColor = if (errorToShow != null) AegisDangerRed else AegisWarningAmber
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (telemetry.isModelLoaded) {
            ModelMetaRow("Input Tensor", telemetry.modelInputSpec)
            ModelMetaRow(
                "Class Mapping",
                when (telemetry.modelOutputClasses) {
                    2 -> "2-Class (Safe / NSFW)"
                    3 -> "3-Class (Safe / Suggestive / Explicit)"
                    5 -> "5-Class (Drawings / Hentai / Neutral / Porn / Sexy)"
                    else -> "${telemetry.modelOutputClasses}-Class"
                }
            )
        } else {
            Text(
                text = "Select a compatible .tflite visual classifier from your phone storage. Supports 2-class, 3-class, and 5-class (MobileNet/Inception NSFW) Float32 or UInt8 models.",
                color = AegisTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        AnimatedVisibility(visible = errorToShow != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AegisDangerRed.copy(alpha = 0.12f))
                    .border(1.dp, AegisDangerRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AegisDangerRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorToShow ?: "",
                        color = AegisDangerRed,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { modelPickerLauncher.launch(arrayOf("*/*")) },
                enabled = !isImporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AegisPrimaryCyan,
                    contentColor = AegisVoidBackground
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("pick_tflite_model_button")
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AegisVoidBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VALIDATING...", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                } else {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (telemetry.isModelLoaded) "REPLACE .TFLITE" else "LOAD .TFLITE FROM PHONE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (telemetry.isModelLoaded && modelManager.activeModelFile.exists()) {
                OutlinedButton(
                    onClick = {
                        modelManager.clearImportedModel()
                        localError = null
                        onModelUpdated()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisDangerRed),
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
    modifier: Modifier = Modifier
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

    CyberCard(modifier = modifier, borderColor = AegisElectricBlue.copy(alpha = 0.35f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AegisElectricBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ImageSearch,
                        contentDescription = null,
                        tint = AegisElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SAMPLE IMAGE TESTER",
                        color = AegisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Verify live TFLite inference on a photo",
                        color = AegisTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Pick any gallery photo using Android's zero-permission Photo Picker to test inference latency, class probabilities, and block decisions offline.",
            color = AegisTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            enabled = telemetry.isModelLoaded && !isTesting,
            colors = ButtonDefaults.buttonColors(
                containerColor = AegisSecondaryBackground,
                contentColor = AegisElectricBlue,
                disabledContainerColor = AegisSecondaryBackground.copy(alpha = 0.5f),
                disabledContentColor = AegisTextMuted
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(
                    1.dp,
                    if (telemetry.isModelLoaded) AegisElectricBlue.copy(alpha = 0.5f) else AegisCardBorder,
                    RoundedCornerShape(10.dp)
                )
                .testTag("test_sample_image_button")
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AegisElectricBlue, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RUNNING INFERENCE...", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            } else {
                Icon(imageVector = Icons.Default.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (telemetry.isModelLoaded) "PICK IMAGE & RUN MODEL TEST" else "LOAD A .TFLITE MODEL FIRST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        AnimatedVisibility(visible = testError != null) {
            Text(
                text = testError ?: "",
                color = AegisDangerRed,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        AnimatedVisibility(visible = testResult != null) {
            testResult?.let { result ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AegisVoidBackground)
                        .border(1.dp, AegisCardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
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
                                .size(68.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, AegisCardBorder, RoundedCornerShape(8.dp))
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            StatusPill(
                                label = if (result.decision.shouldBlock) "WOULD BLOCK" else "ALLOWED (SAFE)",
                                isActive = true,
                                activeColor = if (result.decision.shouldBlock) AegisDangerRed else AegisSuccessGreen
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
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ScoreProgressBar("Safe", result.classification.safeScore, AegisSuccessGreen)
                    ScoreProgressBar("Suggestive", result.classification.suggestiveScore, AegisWarningAmber)
                    ScoreProgressBar("Explicit", result.classification.explicitScore, AegisDangerRed)

                    if (result.classification.rawClassScores.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
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
        Text(text = label, color = AegisTextSecondary, fontSize = 11.sp)
        Text(
            text = value,
            color = AegisTextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ScoreProgressBar(label: String, score: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = AegisTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = "${(score * 100).toInt()}%",
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { score.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}
