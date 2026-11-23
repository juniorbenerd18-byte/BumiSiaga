package com.example.siagabumi.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.views.MapView
import java.util.Locale

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

    /**
     * Generates a custom vector marker icon displaying the exact magnitude number
     * with color coding (Green <4.0, Amber 4.0-4.9, Orange 5.0-5.9, Red >=6.0).
     */
    fun createMagnitudeMarkerIcon(context: Context, magnitude: Double): Drawable {
        val density = context.resources.displayMetrics.density
        val width = (36 * density).toInt()
        val height = (44 * density).toInt()
        val radius = 15f * density

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val color = when {
            magnitude >= 6.0 -> android.graphics.Color.parseColor("#DC2626") // Danger Red
            magnitude >= 5.0 -> android.graphics.Color.parseColor("#EA580C") // Warning Orange
            magnitude >= 4.0 -> android.graphics.Color.parseColor("#D97706") // Alert Amber
            else -> android.graphics.Color.parseColor("#16A34A") // Safe Green
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * density
        }

        // Draw pointer pin at bottom
        val path = Path().apply {
            moveTo(width / 2f - (6f * density), radius + (5f * density))
            lineTo(width / 2f, height.toFloat() - (2f * density))
            lineTo(width / 2f + (6f * density), radius + (5f * density))
            close()
        }
        canvas.drawPath(path, paint)
        canvas.drawPath(path, strokePaint)

        // Draw main circle
        val cx = width / 2f
        val cy = radius + (2f * density)
        canvas.drawCircle(cx, cy, radius, paint)
        canvas.drawCircle(cx, cy, radius, strokePaint)

        // Draw Magnitude text inside circle
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            textSize = 11.5f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val text = String.format(Locale.US, "%.1f", magnitude)
        val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(text, cx, textY, textPaint)

        return BitmapDrawable(context.resources, bitmap)
    }
}
