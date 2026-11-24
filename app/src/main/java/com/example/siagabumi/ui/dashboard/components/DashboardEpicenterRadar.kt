package com.example.siagabumi.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    var isMapFullscreenOpen by remember { mutableStateOf(false) }

    // Tab 0: Peta Wilayah Indonesia, Tab 1: Shakemap BMKG
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

    // Active earthquake to display
    val activeQuakePair = if (targetMode == RadarTargetMode.NEAREST) {
        nearestQuakePair ?: latestQuakePair
    } else {
        latestQuakePair ?: nearestQuakePair
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

    // Shakemap Resolution with Fallback
    val fallbackShakemapUrl = activeGempa.shakemapUrl
        ?: latestQuake?.shakemapUrl
        ?: allQuakes.firstOrNull { !it.shakemapUrl.isNullOrEmpty() }?.shakemapUrl

    val isUsingFallbackShakemap = activeGempa.shakemapUrl.isNullOrEmpty() && !fallbackShakemapUrl.isNullOrEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header (Clean & Minimal)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.dashboard_map_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.dashboard_map_section_subtitle),
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ROW 1: Compact Target Toggle & GPS Sync
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modern Compact Pill Switch: [Terdekat] | [Terbaru]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (targetMode == RadarTargetMode.NEAREST) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier.clickable { targetMode = RadarTargetMode.NEAREST }
                            ) {
                                Text(
                                    text = "🎯 " + stringResource(R.string.dashboard_target_nearest_short),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetMode == RadarTargetMode.NEAREST) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (targetMode == RadarTargetMode.LATEST) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier.clickable { targetMode = RadarTargetMode.LATEST }
                            ) {
                                Text(
                                    text = "⚡ " + stringResource(R.string.dashboard_target_latest_short),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetMode == RadarTargetMode.LATEST) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Compact GPS button
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
                                    modifier = Modifier.size(13.dp)
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

                // ROW 2: Streamlined Distance & Direction Metrics (NO Wall of Text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "%,d".format(distanceKm).replace(',', '.'),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "km",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        // Compact Badges: Direction & Depth
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                                        text = direction,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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

                    // Compact MMI Intensity Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = mmiBadgeColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, mmiBadgeColor.copy(alpha = 0.4f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = mmiEstimate.scale,
                                color = if (mmiEstimate.level == 3) Color(0xFFD97706) else mmiBadgeColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (userPreferences.language == "en") {
                                    when (mmiEstimate.level) {
                                        1 -> "Not Felt"
                                        2 -> "Weak"
                                        3 -> "Felt"
                                        4 -> "Moderate"
                                        5 -> "Strong"
                                        else -> "Severe"
                                    }
                                } else {
                                    when (mmiEstimate.level) {
                                        1 -> "Tak Terasa"
                                        2 -> "Lemah"
                                        3 -> "Terasa"
                                        4 -> "Sedang"
                                        5 -> "Kuat"
                                        else -> "Merusak"
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Single Clean Location Summary Line
                Text(
                    text = stringResource(R.string.dashboard_epicenter_target_prefix, activeGempa.wilayah, activeGempa.magnitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // ROW 3: Tab Row (Peta Indonesia | Shakemap BMKG)
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
                                        imageVector = Icons.Default.Map,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_tab_interactive_map),
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
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_tab_shakemap),
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedMapTab == 1) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedMapTab == 0) {
                        // 100% Native Indonesian Archipelago Disaster Map
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
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

                            // Tap to Enlarge Map Button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clickable { isMapFullscreenOpen = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_btn_open_map),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    } else {
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
                    }
                }
            }
        }
    }

    // Fullscreen Indonesia Map Dialog
    if (isMapFullscreenOpen) {
        Dialog(
            onDismissRequest = { isMapFullscreenOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070D1E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dashboard_map_section_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${allQuakes.size} gempa terdeteksi di katalog BMKG",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        IconButton(
                            onClick = { isMapFullscreenOpen = false },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }

                    // Map Container with proper aspect-fit geometry
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
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
                    }

                    // Earthquake Details Bottom Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F172A)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeGempa.wilayah,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = mmiBadgeColor
                                ) {
                                    Text(
                                        text = "M %.1f".format(activeGempa.magnitude),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (mmiEstimate.level == 3) Color(0xFF1E293B) else Color.White,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📏 %,d km (%s)".format(distanceKm, direction).replace(',', '.'),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "🌊 Kedalaman: ${activeGempa.kedalamanKm} km",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "⚡ ${mmiEstimate.scale}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = mmiBadgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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
