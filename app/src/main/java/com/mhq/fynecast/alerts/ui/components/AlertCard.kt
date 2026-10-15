package com.mhq.fynecast.alerts.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhq.fynecast.R
import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.Tangerine
import com.mhq.fynecast.core.ui.theme.VividAmber

@Composable
fun AlertCard(
    alert: AlertDomainModel,
    isTracked: Boolean,
    isExpanded: Boolean,
    onTrackToggle: () -> Unit,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Establish premium, adaptive colors based on severity
    val severityColor = remember(alert.severity) {
        when (alert.severity.lowercase()) {
            "extreme" -> Color(0xFFFF3B30) // Premium Deep Apple Red, was MaterialTheme.colorScheme.error
            "severe" -> Tangerine
            "moderate" -> VividAmber
            else -> Color(0xFF007AFF) // Editorial Royal Blue, was MaterialTheme.colorScheme.tertiary
        }
    }

    // Gentle container background tinted by severity to reduce stark contrast
    val cardBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val borderGradient = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f),
            severityColor.copy(alpha = 0.05f)
        )
    )

    Card(
        shape = RoundedCornerShape(24.dp), // More modern, aggressive corner rounding
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Column(
            modifier = Modifier
                .background(cardBackground)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.03f), Color.Transparent)))
                // Seamlessly embedded premium micro-border
                .then(Modifier.background(borderGradient, RoundedCornerShape(24.dp)))
                .clip(RoundedCornerShape(24.dp))
                .clickable { onExpandToggle() }
                .padding(20.dp)
        ) {
            // Upper Header Layer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Soft pulsing anchor dot instead of a stark structural ribbon
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(severityColor, RoundedCornerShape(50))
                    )
                    Text(
                        text = alert.event.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = severityColor
                    )
                }

                // Minimalistic pill badge replacing the old corner ribbon layout
                Box(
                    modifier = Modifier
                        .background(severityColor.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = alert.severity.uppercase(),
                        color = severityColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Headline Layer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = alert.headline,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Smooth Expanded Metadata Area
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = alert.description,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )

                    if (alert.instruction.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.instructions).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = alert.instruction,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Premium Tracking Interactive layer
            AlertTrackingButton(
                isTracked = isTracked,
                severityColor = severityColor,
                onTrackToggle = onTrackToggle
            )
        }
    }
}

//import android.content.res.Configuration
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.animateContentSize
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.expandVertically
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.animation.shrinkVertically
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ExpandLess
//import androidx.compose.material.icons.filled.ExpandMore
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clipToBounds
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import com.mhq.fynecast.R
//import com.mhq.fynecast.alerts.domain.models.AlertDomainModel
//import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
//import com.mhq.fynecast.core.ui.theme.FyneCastTheme
//import com.mhq.fynecast.core.ui.theme.Tangerine
//import com.mhq.fynecast.core.ui.theme.VividAmber
//
//@Composable
//fun AlertCard(
//    alert: AlertDomainModel,
//    isTracked: Boolean,
//    isExpanded: Boolean,
//    onTrackToggle: () -> Unit,
//    onExpandToggle: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    val bottomShape = remember {
//        RoundedCornerShape(
//            bottomStart = 16.dp,
//            bottomEnd = 16.dp
//        )
//    }
//
//    val ribbonColor = remember(alert.severity) {
//        alert.severity.lowercase()
//    }.let { severity ->
//        when (severity) {
//            "extreme" -> MaterialTheme.colorScheme.error
//            "severe" -> Tangerine
//            "moderate" -> VividAmber
//            else -> MaterialTheme.colorScheme.tertiary
//        }
//    }
//
//    GlassyCard(
//        cornerRadius = 16.dp,
//        padding = 0.dp,
//        modifier = modifier
//            .fillMaxWidth()
//            .clipToBounds()
//            .animateContentSize(animationSpec = tween(durationMillis = 300))
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .clickable { onExpandToggle() }
//        ) {
//            Column(
//                modifier = Modifier.padding(
//                    start = 16.dp,
//                    top = 16.dp,
//                    bottom = 12.dp,
//                    end = 80.dp
//                ),
//                verticalArrangement = Arrangement.Center
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(4.dp)
//                ) {
//                    Text(
//                        text = remember(alert.event) { alert.event.uppercase() },
//                        style = MaterialTheme.typography.labelMedium,
//                        color = ribbonColor,
//                        fontWeight = FontWeight.Bold
//                    )
//                    Icon(
//                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
//                        contentDescription = null,
//                        tint = ribbonColor,
//                        modifier = Modifier.size(16.dp)
//                    )
//                }
//
//                Spacer(
//                    modifier = Modifier.height(6.dp)
//                )
//
//                Text(
//                    text = alert.headline,
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.onSurface,
//                    fontWeight = FontWeight.Bold,
//                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
//                    overflow = TextOverflow.Ellipsis
//                )
//
//                AnimatedVisibility(
//                    visible = isExpanded,
//                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(
//                        animationSpec = tween(
//                            300
//                        )
//                    ),
//                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(
//                        animationSpec = tween(
//                            300
//                        )
//                    )
//                ) {
//                    Column {
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Text(
//                            text = alert.description,
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
//                        )
//                        if (alert.instruction.isNotEmpty()) {
//                            Spacer(modifier = Modifier.height(8.dp))
//                            Text(
//                                text = stringResource(R.string.instructions),
//                                style = MaterialTheme.typography.labelSmall,
//                                color = MaterialTheme.colorScheme.primary,
//                                fontWeight = FontWeight.Bold
//                            )
//                            Text(
//                                text = alert.instruction,
//                                style = MaterialTheme.typography.bodySmall,
//                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
//                            )
//                        }
//                    }
//                }
//            }
//
//            AlertRibbon(
//                severity = alert.severity.uppercase(),
//                ribbonColor = ribbonColor,
//                modifier = Modifier.align(Alignment.TopEnd)
//            )
//        }
//
//        AlertTrackingButton(
//            isTracked = isTracked,
//            onTrackToggle = onTrackToggle,
//            shape = bottomShape
//        )
//    }
//}
//
//@Preview(
//    name = "Light Mode",
//    showBackground = true,
//    backgroundColor = 0xFFE0F7FA
//)
//
//@Preview(
//    name = "Dark Mode",
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true,
//    backgroundColor = 0xFF000000
//)
//
//@Preview(showBackground = true)
//@Composable
//fun AlertCardPreview() {
//    FyneCastTheme() {
//        AlertCard(
//            alert = AlertDomainModel(
//                id = "",
//                headline = "",
//                severity = "",
//                event = "",
//                effectiveMillis = 1L,
//                expiresMillis = 1L,
//                description = "",
//                instruction = ""
//            ),
//            isTracked = true,
//            onTrackToggle = {},
//            isExpanded = true,
//            onExpandToggle = {}
//        )
//    }
//}