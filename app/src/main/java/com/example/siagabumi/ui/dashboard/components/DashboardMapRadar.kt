package com.example.siagabumi.ui.dashboard.components

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.siagabumi.R
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.util.OsmHelper
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun DashboardMapRadar(
    quakeList: List<Gempa>,
    onViewAllClick: () -> Unit,
    onQuakeClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }

    val mapView = remember {
        MapView(context).apply {
            OsmHelper.setupMapView(this)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.dashboard_map_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.dashboard_map_subtitle),
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                AndroidView(
                    factory = { mapView },
                    update = { map ->
                        val indonesiaCenter = GeoPoint(-2.5, 118.0)
                        map.controller.setZoom(4.8)
                        map.controller.setCenter(indonesiaCenter)

                        map.overlays.clear()
                        quakeList.take(15).forEach { gempa ->
                            if (gempa.latitude != 0.0 && gempa.longitude != 0.0) {
                                val marker = Marker(map).apply {
                                    position = GeoPoint(gempa.latitude, gempa.longitude)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    icon = OsmHelper.createMagnitudeMarkerIcon(context, gempa.magnitude)
                                    title = "M ${gempa.magnitude}"
                                    snippet = "${gempa.wilayah} (${gempa.jam})"
                                    setOnMarkerClickListener { _, _ ->
                                        onQuakeClick?.invoke(gempa.id)
                                        true
                                    }
                                }
                                map.overlays.add(marker)
                            }
                        }
                        map.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(18.dp))
                )

                // Top OpenStreetMap Free Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OpenStreetMap (Gratis)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Interactive Floating Control Buttons on Map Right Side
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Fullscreen Mode Button
                    SmallFloatingActionButton(
                        onClick = { isFullscreen = true },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = stringResource(R.string.dashboard_map_fullscreen), modifier = Modifier.size(18.dp))
                    }

                    // Zoom In Button
                    SmallFloatingActionButton(
                        onClick = { mapView.controller.zoomIn() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.map_btn_zoom_in), modifier = Modifier.size(18.dp))
                    }

                    // Zoom Out Button
                    SmallFloatingActionButton(
                        onClick = { mapView.controller.zoomOut() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.map_btn_zoom_out), modifier = Modifier.size(18.dp))
                    }

                    // Focus to Latest Quake Button
                    if (quakeList.isNotEmpty()) {
                        val latest = quakeList.first()
                        SmallFloatingActionButton(
                            onClick = {
                                if (latest.latitude != 0.0 && latest.longitude != 0.0) {
                                    mapView.controller.animateTo(GeoPoint(latest.latitude, latest.longitude))
                                    mapView.controller.setZoom(7.5)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = stringResource(R.string.map_btn_center_quake), modifier = Modifier.size(18.dp))
                        }
                    }

                    // Recenter Indonesia Button
                    SmallFloatingActionButton(
                        onClick = {
                            mapView.controller.animateTo(GeoPoint(-2.5, 118.0))
                            mapView.controller.setZoom(4.8)
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.secondary,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = stringResource(R.string.map_btn_center_indonesia), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // Fullscreen Map Dialog
    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val fullMapView = remember {
                    MapView(context).apply {
                        OsmHelper.setupMapView(this)
                    }
                }

                DisposableEffect(fullMapView) {
                    fullMapView.onResume()
                    onDispose { fullMapView.onPause() }
                }

                AndroidView(
                    factory = { fullMapView },
                    update = { map ->
                        map.controller.setZoom(5.2)
                        map.controller.setCenter(GeoPoint(-2.5, 118.0))
                        map.overlays.clear()
                        quakeList.forEach { gempa ->
                            if (gempa.latitude != 0.0 && gempa.longitude != 0.0) {
                                val marker = Marker(map).apply {
                                    position = GeoPoint(gempa.latitude, gempa.longitude)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    icon = OsmHelper.createMagnitudeMarkerIcon(context, gempa.magnitude)
                                    title = "M ${gempa.magnitude} - ${gempa.wilayah}"
                                    snippet = "Kedalaman: ${gempa.kedalamanKm} km | ${gempa.jam}"
                                    setOnMarkerClickListener { _, _ ->
                                        isFullscreen = false
                                        onQuakeClick?.invoke(gempa.id)
                                        true
                                    }
                                }
                                map.overlays.add(marker)
                            }
                        }
                        map.invalidate()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Close Button Top End
                IconButton(
                    onClick = { isFullscreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.dashboard_map_close_fullscreen),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Fullscreen floating controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { fullMapView.controller.zoomIn() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                    SmallFloatingActionButton(
                        onClick = { fullMapView.controller.zoomOut() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            fullMapView.controller.animateTo(GeoPoint(-2.5, 118.0))
                            fullMapView.controller.setZoom(5.0)
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.secondary,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                    }
                }
            }
        }
    }
}
