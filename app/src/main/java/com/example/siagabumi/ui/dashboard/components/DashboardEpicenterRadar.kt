package com.example.siagabumi.ui.dashboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.siagabumi.R
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.domain.model.UserPreferences
import com.example.siagabumi.ui.components.DisasterMapView
import com.example.siagabumi.ui.components.ZoomableShakemap
import com.example.siagabumi.ui.theme.MagGreen
import com.example.siagabumi.ui.theme.MagOrange
import com.example.siagabumi.ui.theme.MagRed
import com.example.siagabumi.ui.theme.MagYellow
import com.example.siagabumi.util.GeoUtils
import kotlin.math.roundToInt

enum class RadarTargetMode {
    NEAREST, // Gempa Terdekat
    LATEST   // Gempa Terbaru BMKG
}

@Composable
fun DashboardEpicenterRadar(
    gempa: Gempa?,
    userPreferences: UserPreferences,
    onViewAllClick: () -> Unit,
    onOpenDetailClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSyncGpsClick: (() -> Unit)? = null,
    isSyncingGps: Boolean = false
) {
    DashboardEpicenterRadar(
        gempaTerbaru = gempa,
        gempaList = listOfNotNull(gempa),
        userPreferences = userPreferences,
        onViewAllClick = onViewAllClick,
        onOpenDetailClick = onOpenDetailClick,
        modifier = modifier,
        onSyncGpsClick = onSyncGpsClick,
        isSyncingGps = isSyncingGps
    )
}

@Composable
fun DashboardEpicenterRadar(
    gempaTerbaru: Gempa?,
    gempaList: List<Gempa>,
    userPreferences: UserPreferences,
    onViewAllClick: () -> Unit,
    onOpenDetailClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSyncGpsClick: (() -> Unit)? = null,
    isSyncingGps: Boolean = false
) {
    val context = LocalContext.current
    var isShakemapDialogOpen by remember { mutableStateOf(false) }
    var isRadarFullscreenOpen by remember { mutableStateOf(false) }

    // Tab 0: Shakemap BMKG, Tab 1: Radar Episentrum
    var selectedMapTab by remember { mutableIntStateOf(0) }

    // User safe coordinates
    val safeUserLat = remember(userPreferences) {
        if (userPreferences.userLat == 0.0 && userPreferences.userLon == 0.0) {
            GeoUtils.getApproximateCoordinates(userPreferences.userAdm4, userPreferences.userLocationName).first
        } else {
            userPreferences.userLat
        }
    }
    val safeUserLon = remember(userPreferences) {
        if (userPreferences.userLat == 0.0 && userPreferences.userLon == 0.0) {
            GeoUtils.getApproximateCoordinates(userPreferences.userAdm4, userPreferences.userLocationName).second
        } else {
            userPreferences.userLon
        }
    }

    // Deduplicate and combine all recent earthquakes
    val allQuakes = remember(gempaTerbaru, gempaList) {
        (listOfNotNull(gempaTerbaru) + gempaList).distinctBy { it.id }
    }

    if (allQuakes.isEmpty()) return

    // Calculate distance to user for each earthquake
    val quakesWithDistance = remember(allQuakes, safeUserLat, safeUserLon) {
        allQuakes.map { q ->
            val dist = GeoUtils.calculateDistanceKm(
                safeUserLat,
                safeUserLon,
                q.latitude,
                q.longitude
            ).roundToInt()
            q to dist
        }.sortedBy { it.second }
    }

    val nearestQuakePair = quakesWithDistance.firstOrNull()
    val latestQuake = gempaTerbaru ?: allQuakes.firstOrNull()
    val latestQuakePair = remember(quakesWithDistance, latestQuake) {
        quakesWithDistance.find { it.first.id == latestQuake?.id } ?: nearestQuakePair
    }

    var targetMode by remember { mutableStateOf(RadarTargetMode.NEAREST) }
    var selectedQuakeId by remember { mutableStateOf<String?>(null) }

    // Determine the active earthquake to display
    val activeQuakePair = remember(selectedQuakeId, targetMode, quakesWithDistance, nearestQuakePair, latestQuakePair) {
        if (selectedQuakeId != null) {
            quakesWithDistance.find { it.first.id == selectedQuakeId } ?: nearestQuakePair ?: latestQuakePair
        } else if (targetMode == RadarTargetMode.NEAREST) {
            nearestQuakePair ?: latestQuakePair
        } else {
            latestQuakePair ?: nearestQuakePair
        }
    } ?: return

    val activeGempa = activeQuakePair.first
    val distanceKm = activeQuakePair.second

    val direction = remember(activeGempa, safeUserLat, safeUserLon) {
        GeoUtils.calculateDirection(
            safeUserLat,
            safeUserLon,
            activeGempa.latitude,
            activeGempa.longitude
        )
    }

    val mmiEstimate = remember(activeGempa, distanceKm) {
        GeoUtils.estimateMmi(
            activeGempa.magnitude,
            distanceKm.toDouble(),
            activeGempa.kedalamanKm
        )
    }

    val mmiBadgeColor = when (mmiEstimate.level) {
        1, 2 -> MagGreen
        3 -> MagYellow
        4 -> MagOrange
        else -> MagRed
    }

    // Shakemap Resolution: If active quake has Shakemap use it; else fallback to latestQuake's shakemap
    val fallbackShakemapUrl = activeGempa.shakemapUrl
        ?: latestQuake?.shakemapUrl
        ?: allQuakes.firstOrNull { !it.shakemapUrl.isNullOrEmpty() }?.shakemapUrl

    val isUsingFallbackShakemap = activeGempa.shakemapUrl.isNullOrEmpty() && !fallbackShakemapUrl.isNullOrEmpty()

    val infiniteTransition = rememberInfiniteTransition(label = "pulseRadar")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 38f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.dashboard_epicenter_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.dashboard_epicenter_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.clickable { onViewAllClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.gempa_tab_terkini),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Epicenter Impact Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Target Mode Selector (Gempa Terdekat vs Gempa Terbaru)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = targetMode == RadarTargetMode.NEAREST && selectedQuakeId == null,
                        onClick = {
                            targetMode = RadarTargetMode.NEAREST
                            selectedQuakeId = null
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.dashboard_radar_target_nearest) +
                                        (nearestQuakePair?.let { " (${it.second} km)" } ?: ""),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = targetMode == RadarTargetMode.LATEST && selectedQuakeId == null,
                        onClick = {
                            targetMode = RadarTargetMode.LATEST
                            selectedQuakeId = null
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.dashboard_radar_target_latest) +
                                        (latestQuake?.let { " (M ${it.magnitude})" } ?: ""),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // If multiple earthquakes detected, show horizontal scroll selector
                if (quakesWithDistance.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quakesWithDistance.take(8).forEach { (q, dist) ->
                            val isSelected = activeGempa.id == q.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 0.5.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.clickable {
                                    selectedQuakeId = q.id
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val badgeColor = when {
                                        q.magnitude >= 6.0 -> MagRed
                                        q.magnitude >= 5.0 -> MagOrange
                                        else -> MagYellow
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "${q.wilayah.take(15)}… M${q.magnitude} (${dist} km)",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Clear Explainer Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (targetMode == RadarTargetMode.NEAREST && selectedQuakeId == null) {
                                stringResource(R.string.dashboard_radar_showing_nearest, activeGempa.wilayah)
                            } else if (targetMode == RadarTargetMode.LATEST && selectedQuakeId == null) {
                                stringResource(R.string.dashboard_radar_showing_latest, activeGempa.wilayah)
                            } else {
                                "📍 Fokus: ${activeGempa.wilayah}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Row 1: Streamlined Distance & Direction Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing Compass Radar Graphic
                    Box(
                        modifier = Modifier.size(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = primaryColor.copy(alpha = pulseAlpha),
                                radius = pulseRadius * density,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "%,d".format(distanceKm).replace(',', '.'),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "km",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Compact badges for direction & depth
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Explore,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_radar_direction, direction),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${activeGempa.kedalamanKm} km",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // GPS update button
                    if (onSyncGpsClick != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.clickable(enabled = !isSyncingGps) { onSyncGpsClick() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSyncingGps) "..." else stringResource(R.string.dashboard_gps_sync),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Row 2: Streamlined Estimated Shaking (MMI)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = mmiBadgeColor.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = mmiBadgeColor,
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Text(
                                text = mmiEstimate.scale,
                                color = if (mmiEstimate.level == 3) Color(0xFF1E293B) else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    R.string.dashboard_radar_mmi_summary,
                                    mmiEstimate.scale
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (userPreferences.language == "en") mmiEstimate.descriptionEn else mmiEstimate.descriptionId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Row 3: Visual Disaster Map / Shakemap Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    SecondaryTabRow(
                        selectedTabIndex = selectedMapTab,
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedMapTab == 0,
                            onClick = { selectedMapTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_tab_shakemap),
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedMapTab == 0) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedMapTab == 1,
                            onClick = { selectedMapTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Radar,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_tab_interactive_map),
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedMapTab == 1) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedMapTab == 0) {
                        // Official BMKG Shakemap (Verified Map Image with Fallback)
                        if (!fallbackShakemapUrl.isNullOrEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (isUsingFallbackShakemap && latestQuake != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.dashboard_shakemap_fallback_notice, latestQuake.magnitude),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { isShakemapDialogOpen = true }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(fallbackShakemapUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = stringResource(R.string.dashboard_shakemap_official_badge),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(R.string.dashboard_shakemap_official_badge),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(R.string.dashboard_btn_open_shakemap),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_shakemap_policy_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // 100% Native Vector Radar Map (Never goes black, zero network dependency)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            DisasterMapView(
                                epicenterLat = activeGempa.latitude,
                                epicenterLon = activeGempa.longitude,
                                magnitude = activeGempa.magnitude,
                                depthKm = activeGempa.kedalamanKm,
                                userLat = safeUserLat,
                                userLon = safeUserLon,
                                userLocationName = userPreferences.userLocationName,
                                allQuakes = allQuakes,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Tap to Enlarge Radar Button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clickable { isRadarFullscreenOpen = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_btn_open_radar),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Native Radar Dialog
    if (isRadarFullscreenOpen) {
        Dialog(
            onDismissRequest = { isRadarFullscreenOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
            ) {
                DisasterMapView(
                    epicenterLat = activeGempa.latitude,
                    epicenterLon = activeGempa.longitude,
                    magnitude = activeGempa.magnitude,
                    depthKm = activeGempa.kedalamanKm,
                    userLat = safeUserLat,
                    userLon = safeUserLon,
                    userLocationName = userPreferences.userLocationName,
                    allQuakes = allQuakes,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                )

                IconButton(
                    onClick = { isRadarFullscreenOpen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Zoomable Shakemap Dialog
    if (isShakemapDialogOpen && !fallbackShakemapUrl.isNullOrEmpty()) {
        Dialog(
            onDismissRequest = { isShakemapDialogOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
            ) {
                IconButton(
                    onClick = { isShakemapDialogOpen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ZoomableShakemap(
                        imageUrl = fallbackShakemapUrl,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
