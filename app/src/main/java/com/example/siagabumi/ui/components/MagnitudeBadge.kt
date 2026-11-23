package com.example.siagabumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siagabumi.ui.theme.getMagnitudeColor
import java.util.Locale

@Composable
fun MagnitudeBadge(
    magnitude: Double,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fontSize: Int = 16
) {
    val bgColor = getMagnitudeColor(magnitude)
    val text = String.format(Locale.US, "%.1f", magnitude)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
