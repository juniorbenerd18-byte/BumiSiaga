package com.example.siagabumi.util

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.views.MapView

object OsmHelper {

    /**
     * Modern, fast, and 100% free CartoDB Voyager tile source based on OpenStreetMap data.
     * High performance global CDN with no volunteer-server IP / User-Agent blocks.
     */
    val CARTO_VOYAGER: OnlineTileSourceBase = XYTileSource(
        "CartoVoyager",
        0, 19, 256, ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
        ),
        "© OpenStreetMap contributors, © CARTO"
    )

    /**
     * Initializes Osmdroid with a compliant, unique User-Agent and purges any legacy blocked tiles.
     */
    fun initOsmdroid(context: Context) {
        val prefs = context.getSharedPreferences("siagabumi_osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, prefs)

        // Strict OSM rule: NEVER use 'com.example' or empty User-Agent!
        Configuration.getInstance().userAgentValue = "SiagaBumi-DisasterApp/1.0 (Android; id.siagabumi.app; contact@siagabumi.id)"

        // Purge old cached tiles that may contain the "Blocked" image
        try {
            val tileCache = Configuration.getInstance().osmdroidTileCache
            if (tileCache != null && tileCache.exists()) {
                val oldMapnik = java.io.File(tileCache, "Mapnik")
                if (oldMapnik.exists()) {
                    oldMapnik.deleteRecursively()
                }
            }
        } catch (_: Exception) {
            // Ignore cache cleanup failure
        }
    }

    /**
     * Applies standard settings to a MapView instance.
     */
    fun setupMapView(mapView: MapView) {
        mapView.setTileSource(CARTO_VOYAGER)
        mapView.setMultiTouchControls(true)
        mapView.isTilesScaledToDpi = true
    }
}
