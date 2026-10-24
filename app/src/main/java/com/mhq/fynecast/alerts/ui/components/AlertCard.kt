package com.mhq.fynecast.alerts.ui.components

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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
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
import com.mhq.fynecast.alerts.presentation.model.AlertPresentationModel
import com.mhq.fynecast.alerts.presentation.model.AlertSeverity
import com.mhq.fynecast.alerts.ui.mapper.toColor
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

/**
 * Premium, modern, sleek, and space-efficient weather card implementation.
 * Maintains full architectural compatibility, existing callbacks, and domain models.
 */
@Composable
fun AlertCard(
    alert: AlertPresentationModel,
    isTracked: Boolean,
    isExpanded: Boolean,
    onTrackToggle: () -> Unit,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val severityColor = remember(alert.severity) { alert.severity.toColor() }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Premium multi-layered glass-morphic background effect
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            severityColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .clip(RoundedCornerShape(22.dp))
                .clickable { onExpandToggle() }
                .padding(16.dp) // Optimized space efficiency (reduced from 20dp)
        ) {
            // Header Layer: Status Indicator, Event Title, and Severity Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color = severityColor, shape = CircleShape)
                    )
                    Text(
                        text = alert.event.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = severityColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(
                            color = severityColor.copy(alpha = 0.14f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = alert.severityLabel.uppercase(),
                        color = severityColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Headline & Expand Toggle Icon Layer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = alert.headline,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Smooth Expandable Metadata Area (Description & Instructions)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // Divider line for sleek visual separation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = alert.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    if (alert.instruction.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
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
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tracking Control Button Action Layer
            AlertTrackingButton(
                isTracked = isTracked,
                severityColor = severityColor,
                onTrackToggle = onTrackToggle
            )
        }
    }
}

@Preview
@Composable
private fun AlertCardPreview() {
    FyneCastTheme() {
        AlertCard(
            alert = AlertPresentationModel(
                id = "1",
                event = "Flood",
                headline = "Flood warning for the coast",
                description = "Water levels are rising.",
                instruction = "Move to higher ground.",
                severity = AlertSeverity.Severe,
                severityLabel = "Severe"
            ),
            isTracked = true,
            isExpanded = true,
            onTrackToggle = {},
            onExpandToggle = {}
        )
    }
}