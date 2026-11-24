package com.example.siagabumi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siagabumi.R
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.ui.theme.MagOrange
import com.example.siagabumi.ui.theme.MagRed
import com.example.siagabumi.ui.theme.MagYellow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun DisasterMapView(
    epicenterLat: Double,
    epicenterLon: Double,
    magnitude: Double,
    depthKm: Int,
    userLat: Double,
    userLon: Double,
    userLocationName: String,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    allQuakes: List<Gempa> = emptyList()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarMapAnim")
    
    // Rotating radar sweep angle
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    // Epicenter pulse wave
    val pulseWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseWave"
    )

    // Safe user coordinates
    val safeUserLat = if (userLat == 0.0 && userLon == 0.0) -6.2088 else userLat
    val safeUserLon = if (userLat == 0.0 && userLon == 0.0) 106.8456 else userLon

    // Calculate bearing angle from user to primary epicenter
    val bearingRad = remember(epicenterLat, epicenterLon, safeUserLat, safeUserLon) {
        val dLon = Math.toRadians(epicenterLon - safeUserLon)
        val lat1 = Math.toRadians(safeUserLat)
        val lat2 = Math.toRadians(epicenterLat)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        atan2(y, x) - Math.PI / 2.0 // Math angle: 0 = East, -PI/2 = North
    }

    // Calculate positions for other detected earthquakes on the radar
    val otherQuakeBlips = remember(allQuakes, epicenterLat, epicenterLon, safeUserLat, safeUserLon) {
        allQuakes.filter { q ->
            (q.latitude != epicenterLat || q.longitude != epicenterLon) &&
                    (q.latitude != 0.0 || q.longitude != 0.0)
        }.take(12).map { q ->
            val dLon = Math.toRadians(q.longitude - safeUserLon)
            val lat1 = Math.toRadians(safeUserLat)
            val lat2 = Math.toRadians(q.latitude)
            val y = sin(dLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
            val bearing = atan2(y, x) - Math.PI / 2.0
            val distKm = com.example.siagabumi.util.GeoUtils.calculateDistanceKm(safeUserLat, safeUserLon, q.latitude, q.longitude)
            // Distance factor scaled across radar rings (up to 3000 km)
            val factor = (0.22f + (distKm.toFloat() / 3200f) * 0.65f).coerceIn(0.20f, 0.88f)
            Triple(bearing, factor, q)
        }
    }

    val magColor = when {
        magnitude >= 6.0 -> MagRed
        magnitude >= 5.0 -> MagOrange
        else -> MagYellow
    }

    val isEn = LocalConfiguration.current.locales[0].language == "en"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A1128))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = min(centerX, centerY) * 0.85f

            // 1. Concentric Range Rings
            val gridColor = Color(0xFF1E293B)
            val ringCount = 3
            for (i in 1..ringCount) {
                val r = maxRadius * (i.toFloat() / ringCount)
                drawCircle(
                    color = gridColor,
                    radius = r,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                )
            }

            // 2. Cardinal Crosshairs (North, South, East, West)
            drawLine(
                color = Color(0xFF1E293B).copy(alpha = 0.8f),
                start = Offset(centerX - maxRadius, centerY),
                end = Offset(centerX + maxRadius, centerY),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0xFF1E293B).copy(alpha = 0.8f),
                start = Offset(centerX, centerY - maxRadius),
                end = Offset(centerX, centerY + maxRadius),
                strokeWidth = 1.dp.toPx()
            )

            // 3. Secondary Quake Blips detected across Indonesia
            otherQuakeBlips.forEach { (bearing, factor, q) ->
                val qX = centerX + (maxRadius * factor * cos(bearing)).toFloat()
                val qY = centerY + (maxRadius * factor * sin(bearing)).toFloat()
                val qColor = when {
                    q.magnitude >= 6.0 -> MagRed
                    q.magnitude >= 5.0 -> MagOrange
                    else -> MagYellow
                }
                drawCircle(
                    color = qColor.copy(alpha = 0.45f),
                    radius = 4.5.dp.toPx(),
                    center = Offset(qX, qY)
                )
                drawCircle(
                    color = qColor,
                    radius = 2.5.dp.toPx(),
                    center = Offset(qX, qY)
                )
            }

            // 4. Animated Radar Sweep Beam
            val sweepRad = Math.toRadians(sweepAngle.toDouble())
            val sweepEndX = centerX + (maxRadius * cos(sweepRad)).toFloat()
            val sweepEndY = centerY + (maxRadius * sin(sweepRad)).toFloat()
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF10B981).copy(alpha = 0.05f), Color(0xFF10B981).copy(alpha = 0.45f)),
                    start = Offset(centerX, centerY),
                    end = Offset(sweepEndX, sweepEndY)
                ),
                start = Offset(centerX, centerY),
                end = Offset(sweepEndX, sweepEndY),
                strokeWidth = 2.dp.toPx()
            )

            // 5. Active Target Epicenter Position (relative to user center)
            val targetDistanceFactor = 0.68f // placed gracefully in the outer ring
            val epiX = centerX + (maxRadius * targetDistanceFactor * cos(bearingRad)).toFloat()
            val epiY = centerY + (maxRadius * targetDistanceFactor * sin(bearingRad)).toFloat()

            // 6. Trajectory Dotted Line from User to Active Epicenter
            drawLine(
                color = Color(0xFF38BDF8).copy(alpha = 0.7f),
                start = Offset(centerX, centerY),
                end = Offset(epiX, epiY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )

            // 7. Shaking Zone / Impact Radius Circle
            val impactRadius = 36.dp.toPx()
            drawCircle(
                color = magColor.copy(alpha = 0.15f),
                radius = impactRadius,
                center = Offset(epiX, epiY)
            )
            drawCircle(
                color = magColor.copy(alpha = 0.4f),
                radius = impactRadius,
                center = Offset(epiX, epiY),
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
            )

            // 8. Epicenter Pulsing Wave Animation
            val pulseRadiusPx = 10.dp.toPx() + (30.dp.toPx() * pulseWave)
            val pulseAlpha = (1f - pulseWave).coerceIn(0f, 1f)
            drawCircle(
                color = magColor.copy(alpha = pulseAlpha * 0.7f),
                radius = pulseRadiusPx,
                center = Offset(epiX, epiY),
                style = Stroke(width = 2.dp.toPx())
            )

            // 9. Epicenter Bullseye Center
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(epiX, epiY)
            )
            drawCircle(
                color = magColor,
                radius = 6.dp.toPx(),
                center = Offset(epiX, epiY)
            )

            // 10. User Location Marker (Center)
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.3f),
                radius = 12.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color(0xFF0284C7),
                radius = 4.5.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }

        // Overlay: Cardinal Directions
        Text(
            text = "U",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
        )
        Text(
            text = "S",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp)
        )
        Text(
            text = "T",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )
        Text(
            text = "B",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        )

        // Overlay: Top-Right Detected Count Badge
        val totalDetected = (allQuakes.size).coerceAtLeast(1)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF334155)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEn) "$totalDetected Quakes Detected" else "$totalDetected Gempa Terdeteksi",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE2E8F0)
                )
            }
        }

        // Overlay: Floating Badge Info
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF334155)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(magColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "M %.1f • %d km".format(magnitude, depthKm),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Overlay Legend at Bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8))
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isEn) "You" else "Anda",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(magColor)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isEn) "Target Focus" else "Fokus Target",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            if (otherQuakeBlips.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isEn) "Other Quakes (${otherQuakeBlips.count()})" else "Gempa Lain (${otherQuakeBlips.count()})",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(magColor.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isEn) "Impact Zone" else "Zona Guncangan",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
