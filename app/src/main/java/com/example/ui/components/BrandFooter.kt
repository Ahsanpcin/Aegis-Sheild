package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisVoidBackground
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

const val AHSAN_TCR_CONTACT_URL = "https://ahsantcr.lovable.app"
const val AHSAN_TCR_CONTACT_DOMAIN = "ahsantcr.lovable.app"

fun openAhsanTcrContact(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AHSAN_TCR_CONTACT_URL)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Visit $AHSAN_TCR_CONTACT_DOMAIN to contact AhsanTCr", Toast.LENGTH_LONG).show()
    }
}

/**
 * Sticky Scaffold bottom bar branding shown across every Sheildlenz screen.
 */
@Composable
fun SheildlenzScaffoldBrandBar(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AegisVoidBackground.copy(alpha = 0.90f),
                        AegisCardBackground.copy(alpha = 0.98f)
                    )
                )
            )
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = AegisCardBorder.copy(alpha = 0.75f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            AhsanTcrBrandingPill()
        }
    }
}

/**
 * Compact luxury attribution pill displaying "Built By AhsanTCr" and clickable contact link.
 */
@Composable
fun AhsanTcrBrandingPill(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(AegisCardBackground, AegisSurfaceElevated)
                )
            )
            .border(1.dp, AegisChampagneGold.copy(alpha = 0.55f), RoundedCornerShape(50))
            .clickable { openAhsanTcrContact(context) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("built_by_ahsantcr_link"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Built By ",
            color = AegisTextSecondary,
            fontSize = 11.sp,
            fontFamily = ClassicalBodyFont,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "AhsanTCr",
            color = AegisTextPrimary,
            fontSize = 12.sp,
            fontFamily = ClassicalDisplayFont,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "  •  ",
            color = AegisChampagneGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = AegisPrimaryCyan,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(AHSAN_TCR_CONTACT_DOMAIN)
                }
            },
            fontSize = 11.sp,
            fontFamily = ClassicalBodyFont
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Contact AhsanTCr at ahsantcr.lovable.app",
            tint = AegisPrimaryCyan,
            modifier = Modifier.size(13.dp)
        )
    }
}
