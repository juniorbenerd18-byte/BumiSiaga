package com.example.siagabumi.ui.components

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
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
    isInteractive: Boolean = true
) {
    val isDark = isSystemInDarkTheme()

    val htmlContent = remember(epicenterLat, epicenterLon, magnitude, depthKm, userLat, userLon, isDark) {
        generateLeafletHtml(
            epicenterLat = epicenterLat,
            epicenterLon = epicenterLon,
            magnitude = magnitude,
            depthKm = depthKm,
            userLat = userLat,
            userLon = userLon,
            userLocationName = userLocationName.replace("'", "\\'"),
            isDark = isDark,
            isInteractive = isInteractive
        )
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()

                    if (isInteractive) {
                        setOnTouchListener { v, event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                            false
                        }
                    }

                    loadDataWithBaseURL(
                        "https://openstreetmap.org",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(
                    "https://openstreetmap.org",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        )
    }
}

private fun generateLeafletHtml(
    epicenterLat: Double,
    epicenterLon: Double,
    magnitude: Double,
    depthKm: Int,
    userLat: Double,
    userLon: Double,
    userLocationName: String,
    isDark: Boolean,
    isInteractive: Boolean
): String {
    val hasUserLocation = (userLat != 0.0 || userLon != 0.0) && (userLat != epicenterLat || userLon != epicenterLon)
    val mapTileFilter = if (isDark) {
        "filter: brightness(0.72) invert(1) contrast(3) hue-rotate(200deg) saturate(0.35);"
    } else {
        ""
    }

    val shakingRadiusMeters = (magnitude * 30000).coerceIn(40000.0, 350000.0)

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body { margin: 0; padding: 0; background-color: ${if (isDark) "#0F172A" else "#F8FAFC"}; }
                #map { width: 100vw; height: 100vh; }
                .leaflet-tile-pane { $mapTileFilter }
                .pulse-epicenter {
                    width: 22px;
                    height: 22px;
                    background: #EF4444;
                    border-radius: 50%;
                    box-shadow: 0 0 0 6px rgba(239, 68, 68, 0.45);
                    animation: pulse 1.5s infinite;
                    border: 2px solid white;
                }
                .pulse-user {
                    width: 16px;
                    height: 16px;
                    background: #3B82F6;
                    border-radius: 50%;
                    box-shadow: 0 0 0 5px rgba(59, 130, 246, 0.4);
                    border: 2px solid white;
                }
                @keyframes pulse {
                    0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7); }
                    70% { box-shadow: 0 0 0 16px rgba(239, 68, 68, 0); }
                    100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
                }
                .leaflet-popup-content-wrapper {
                    background: ${if (isDark) "#1E293B" else "#FFFFFF"};
                    color: ${if (isDark) "#F8FAFC" else "#0F172A"};
                    border-radius: 10px;
                    font-family: sans-serif;
                    font-size: 12px;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: $isInteractive,
                    dragging: $isInteractive,
                    touchZoom: $isInteractive,
                    scrollWheelZoom: $isInteractive,
                    doubleClickZoom: $isInteractive,
                    attributionControl: false
                });

                L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 18
                }).addTo(map);

                // Epicenter Pulse Marker
                var epicenterIcon = L.divIcon({
                    className: 'pulse-epicenter',
                    iconSize: [22, 22],
                    iconAnchor: [11, 11]
                });
                var epicenterMarker = L.marker([$epicenterLat, $epicenterLon], { icon: epicenterIcon }).addTo(map);
                epicenterMarker.bindPopup("<b>🌋 Pusat Gempa M $magnitude</b><br>Kedalaman: $depthKm km");

                // Shaking Zone Circle
                L.circle([$epicenterLat, $epicenterLon], {
                    color: '#EF4444',
                    fillColor: '#EF4444',
                    fillOpacity: 0.15,
                    weight: 1.5,
                    dashArray: '4, 4',
                    radius: $shakingRadiusMeters
                }).addTo(map);

                ${if (hasUserLocation) """
                    // User Location Marker
                    var userIcon = L.divIcon({
                        className: 'pulse-user',
                        iconSize: [16, 16],
                        iconAnchor: [8, 8]
                    });
                    var userMarker = L.marker([$userLat, $userLon], { icon: userIcon }).addTo(map);
                    userMarker.bindPopup("<b>📍 Posisi Anda</b><br>$userLocationName");

                    // Distance Polyline
                    var latlngs = [
                        [$epicenterLat, $epicenterLon],
                        [$userLat, $userLon]
                    ];
                    var polyline = L.polyline(latlngs, {
                        color: '#38BDF8',
                        weight: 2.5,
                        dashArray: '6, 6',
                        opacity: 0.85
                    }).addTo(map);

                    // Fit bounds to show both user and epicenter
                    var bounds = L.latLngBounds([
                        [$epicenterLat, $epicenterLon],
                        [$userLat, $userLon]
                    ]);
                    map.fitBounds(bounds, { padding: [40, 40] });
                """ else """
                    map.setView([$epicenterLat, $epicenterLon], 6);
                """}
            </script>
        </body>
        </html>
    """.trimIndent()
}
