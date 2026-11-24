package com.example.siagabumi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.ui.theme.MagOrange
import com.example.siagabumi.ui.theme.MagRed
import com.example.siagabumi.ui.theme.MagYellow
import kotlin.math.roundToInt

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
    val infiniteTransition = rememberInfiniteTransition(label = "mapAnim")

    // Epicenter shockwave pulse
    val pulseWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseWave"
    )

    // User safe coordinates
    val safeUserLat = if (userLat == 0.0 && userLon == 0.0) -6.2088 else userLat
    val safeUserLon = if (userLat == 0.0 && userLon == 0.0) 106.8456 else userLon

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
            .background(Color(0xFF091224)) // Deep oceanic navy
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padX = 14.dp.toPx()
            val padY = 16.dp.toPx()

            fun projectGeo(lat: Double, lon: Double): Offset {
                val minLon = 95.0
                val maxLon = 141.2
                val minLat = -11.0
                val maxLat = 6.0

                val cLon = lon.coerceIn(minLon, maxLon)
                val cLat = lat.coerceIn(minLat, maxLat)

                val x = padX + ((cLon - minLon) / (maxLon - minLon)).toFloat() * (size.width - padX * 2)
                val y = padY + ((maxLat - cLat) / (maxLat - minLat)).toFloat() * (size.height - padY * 2)
                return Offset(x, y)
            }

            // 1. Draw Geographic Graticule Grid (Equator & Longitudes)
            val gridColor = Color(0xFF16233B)
            // Equator (0 deg)
            val eqY = projectGeo(0.0, 95.0).y
            drawLine(
                color = Color(0xFF1E293B).copy(alpha = 0.8f),
                start = Offset(padX, eqY),
                end = Offset(size.width - padX, eqY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
            // Longitude Meridians: 100E, 110E, 120E, 130E, 140E
            listOf(100.0, 110.0, 120.0, 130.0, 140.0).forEach { lon ->
                val x = projectGeo(0.0, lon).x
                drawLine(
                    color = gridColor.copy(alpha = 0.5f),
                    start = Offset(x, padY),
                    end = Offset(x, size.height - padY),
                    strokeWidth = 0.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
                )
            }

            // 2. Draw Vector Map of Indonesian Archipelago Islands
            val islandColor = Color(0xFF1E2E4A)
            val islandBorder = Color(0xFF334E68)
            drawArchipelago(::projectGeo, islandColor, islandBorder)

            // 3. Draw Secondary Detected Earthquakes Across Indonesia
            allQuakes.filter { q ->
                (q.latitude != epicenterLat || q.longitude != epicenterLon) &&
                        (q.latitude != 0.0 || q.longitude != 0.0)
            }.take(16).forEach { q ->
                val qPos = projectGeo(q.latitude, q.longitude)
                val qColor = when {
                    q.magnitude >= 6.0 -> MagRed
                    q.magnitude >= 5.0 -> MagOrange
                    else -> MagYellow
                }
                drawCircle(
                    color = qColor.copy(alpha = 0.35f),
                    radius = 5.dp.toPx(),
                    center = qPos
                )
                drawCircle(
                    color = qColor,
                    radius = 3.dp.toPx(),
                    center = qPos
                )
            }

            // 4. Calculate Key Coordinates
            val userPos = projectGeo(safeUserLat, safeUserLon)
            val epiPos = projectGeo(epicenterLat, epicenterLon)

            // 5. Distance Trajectory Dotted Line from User to Epicenter
            drawLine(
                color = Color(0xFF38BDF8).copy(alpha = 0.75f),
                start = userPos,
                end = epiPos,
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )

            // 6. Active Epicenter Impact & Shockwave Animation
            val impactRadiusPx = 28.dp.toPx()
            drawCircle(
                color = magColor.copy(alpha = 0.12f),
                radius = impactRadiusPx,
                center = epiPos
            )
            drawCircle(
                color = magColor.copy(alpha = 0.35f),
                radius = impactRadiusPx,
                center = epiPos,
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
            )

            // Pulsing wave
            val pulseRadius = 8.dp.toPx() + (32.dp.toPx() * pulseWave)
            val pulseAlpha = (1f - pulseWave).coerceIn(0f, 1f)
            drawCircle(
                color = magColor.copy(alpha = pulseAlpha * 0.75f),
                radius = pulseRadius,
                center = epiPos,
                style = Stroke(width = 2.dp.toPx())
            )

            // Epicenter Bullseye
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = epiPos
            )
            drawCircle(
                color = magColor,
                radius = 5.dp.toPx(),
                center = epiPos
            )

            // 7. User Location Pin (Center Beacon)
            drawCircle(
                color = Color(0xFF0284C7).copy(alpha = 0.3f),
                radius = 11.dp.toPx(),
                center = userPos
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = userPos
            )
            drawCircle(
                color = Color(0xFF0284C7),
                radius = 4.dp.toPx(),
                center = userPos
            )
        }

        // Overlay: Top-Left Title Badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF334155)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(magColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "M %.1f • %d km".format(magnitude, depthKm),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Overlay: Top-Right Detected Count Badge
        val totalDetected = (allQuakes.size).coerceAtLeast(1)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF334155)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEn) "$totalDetected Quakes Active" else "$totalDetected Gempa Aktif",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE2E8F0)
                )
            }
        }

        // Overlay Legend at Bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF0B132B).copy(alpha = 0.90f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEn) "You" else "Posisi Anda",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(magColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEn) "Target Epicenter" else "Episentrum Target",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEn) "Other Quakes" else "Gempa Lain",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

/**
 * Draws simplified polygon silhouettes of major Indonesian islands
 * based on geographic latitude and longitude coordinates.
 */
private fun DrawScope.drawArchipelago(
    project: (Double, Double) -> Offset,
    fillColor: Color,
    borderColor: Color
) {
    fun drawPolygon(points: List<Pair<Double, Double>>) {
        if (points.size < 3) return
        val path = Path()
        val first = project(points[0].first, points[0].second)
        path.moveTo(first.x, first.y)
        for (i in 1 until points.size) {
            val pt = project(points[i].first, points[i].second)
            path.lineTo(pt.x, pt.y)
        }
        path.close()
        drawPath(path, fillColor, style = Fill)
        drawPath(path, borderColor, style = Stroke(width = 0.8.dp.toPx()))
    }

    // 1. Sumatra Island
    drawPolygon(
        listOf(
            5.5 to 95.3, 5.2 to 96.0, 4.2 to 98.2, 2.8 to 100.0,
            1.2 to 101.8, -0.6 to 103.5, -2.0 to 104.8, -3.4 to 106.0,
            -5.8 to 105.8, -5.5 to 104.5, -4.6 to 103.2, -3.2 to 101.8,
            -1.6 to 100.2, 0.2 to 98.8, 2.2 to 97.2, 4.4 to 95.6, 5.5 to 95.3
        )
    )

    // 2. Java Island & Madura
    drawPolygon(
        listOf(
            -6.0 to 105.9, -6.0 to 107.0, -6.4 to 108.6, -6.9 to 110.5,
            -6.6 to 111.6, -6.9 to 112.8, -7.8 to 114.4, -8.7 to 114.4,
            -8.3 to 112.5, -8.1 to 110.6, -7.7 to 108.6, -7.0 to 106.5,
            -6.8 to 105.2, -6.0 to 105.9
        )
    )
    // Madura
    drawPolygon(
        listOf(
            -7.0 to 112.7, -6.9 to 114.0, -7.2 to 114.1, -7.1 to 112.8, -7.0 to 112.7
        )
    )

    // 3. Bali, Lombok, Sumbawa, Flores, Timor
    // Bali
    drawPolygon(
        listOf(-8.1 to 114.5, -8.1 to 115.3, -8.8 to 115.5, -8.8 to 115.1, -8.1 to 114.5)
    )
    // Lombok
    drawPolygon(
        listOf(-8.3 to 116.0, -8.3 to 116.6, -8.9 to 116.4, -8.7 to 115.9, -8.3 to 116.0)
    )
    // Sumbawa
    drawPolygon(
        listOf(-8.4 to 116.8, -8.3 to 118.8, -8.9 to 118.6, -8.8 to 116.9, -8.4 to 116.8)
    )
    // Flores
    drawPolygon(
        listOf(-8.5 to 119.8, -8.3 to 122.9, -8.8 to 122.8, -8.8 to 119.9, -8.5 to 119.8)
    )
    // Timor (Indonesia)
    drawPolygon(
        listOf(-9.2 to 124.0, -9.0 to 125.0, -9.9 to 124.8, -10.3 to 123.5, -9.8 to 123.6, -9.2 to 124.0)
    )

    // 4. Kalimantan (Borneo - Indonesian territory)
    drawPolygon(
        listOf(
            4.0 to 117.5, 2.5 to 117.8, 1.0 to 118.9, -1.2 to 117.0,
            -3.8 to 116.2, -4.2 to 114.5, -3.2 to 112.5, -2.8 to 110.0,
            -0.8 to 109.0, 1.0 to 109.0, 1.5 to 110.5, 2.0 to 113.0,
            3.0 to 115.5, 4.0 to 116.0, 4.0 to 117.5
        )
    )

    // 5. Sulawesi (The 4-armed island)
    // North arm (Minahasa/Manado)
    drawPolygon(
        listOf(1.5 to 124.9, 0.8 to 123.0, 0.5 to 120.0, 1.2 to 120.8, 1.0 to 122.5, 1.8 to 125.0, 1.5 to 124.9)
    )
    // Central & South arm (Makassar)
    drawPolygon(
        listOf(-0.5 to 119.8, -3.0 to 119.0, -5.6 to 119.5, -5.5 to 120.5, -3.5 to 120.4, -2.0 to 120.2, -1.0 to 120.5, -0.5 to 119.8)
    )
    // Southeast arm (Kendari)
    drawPolygon(
        listOf(-2.5 to 121.2, -4.0 to 122.8, -5.2 to 122.5, -4.8 to 121.8, -3.5 to 121.5, -2.5 to 121.2)
    )
    // East arm
    drawPolygon(
        listOf(-0.8 to 121.5, -1.0 to 123.5, -1.6 to 123.5, -1.5 to 121.5, -0.8 to 121.5)
    )

    // 6. Maluku & Halmahera
    // Halmahera
    drawPolygon(
        listOf(1.8 to 127.8, 1.5 to 128.5, 0.5 to 128.0, 0.0 to 127.3, 0.8 to 127.4, 1.8 to 127.8)
    )
    // Seram / Ambon
    drawPolygon(
        listOf(-3.0 to 128.0, -3.0 to 130.5, -3.6 to 130.0, -3.8 to 128.2, -3.0 to 128.0)
    )

    // 7. Papua Island
    drawPolygon(
        listOf(
            0.0 to 131.2, -0.8 to 131.0, -1.5 to 132.5, -2.5 to 134.0,
            -1.8 to 135.0, -2.5 to 138.0, -2.5 to 141.0, -9.0 to 141.0,
            -8.5 to 138.0, -5.0 to 137.0, -4.5 to 135.0, -3.5 to 133.5,
            -2.0 to 132.0, -0.5 to 132.0, 0.0 to 131.2
        )
    )
}
