package com.example.siagabumi.ui.settings

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.siagabumi.R
import com.example.siagabumi.domain.model.AppThemeMode
import com.example.siagabumi.ui.theme.MagOrange

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsState()
    val isDetectingGps by viewModel.isDetectingGps.collectAsState()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.detectGpsLocation { result ->
                if (result != null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_gps_success, result.locationName),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_gps_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.settings_gps_permission_required),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val onTriggerGps: () -> Unit = {
        if (viewModel.hasLocationPermission()) {
            viewModel.detectGpsLocation { result ->
                if (result != null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_gps_success, result.locationName),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_gps_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: PERINGATAN & NOTIFIKASI
            Text(
                text = stringResource(R.string.settings_section_alerts),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Switch Notifikasi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_notif_title),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = stringResource(R.string.settings_notif_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = prefs.notificationsEnabled,
                            onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                        )
                    }

                    // Switch Getar HP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = MagOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_vibrate_title),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = stringResource(R.string.settings_vibrate_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = prefs.vibrationEnabled,
                            onCheckedChange = { viewModel.setVibrationEnabled(it) },
                            enabled = prefs.notificationsEnabled
                        )
                    }

                    // Filter Batas Magnitudo
                    Column {
                        Text(
                            text = stringResource(R.string.settings_min_mag_title),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = stringResource(R.string.settings_min_mag_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(3.0, 4.0, 5.0, 6.0).forEach { mag ->
                                FilterChip(
                                    selected = prefs.minMagnitudeAlert == mag,
                                    onClick = { viewModel.setMinMagnitude(mag) },
                                    label = { Text("M ≥ ${mag.toInt()}.0") },
                                    enabled = prefs.notificationsEnabled
                                )
                            }
                        }
                    }

                    // Jarak Gempa di Sekitar
                    Column {
                        Text(
                            text = stringResource(R.string.settings_nearby_radius_title),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = stringResource(R.string.settings_nearby_radius_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val radiusOptions = listOf(
                                0 to stringResource(R.string.settings_radius_all),
                                50 to stringResource(R.string.settings_radius_50km),
                                100 to stringResource(R.string.settings_radius_100km),
                                250 to stringResource(R.string.settings_radius_250km),
                                500 to stringResource(R.string.settings_radius_500km)
                            )
                            radiusOptions.forEach { (radiusKm, label) ->
                                FilterChip(
                                    selected = prefs.nearbyRadiusKm == radiusKm,
                                    onClick = { viewModel.setNearbyRadius(radiusKm) },
                                    label = { Text(label) },
                                    enabled = prefs.notificationsEnabled
                                )
                            }
                        }
                    }

                    // Lokasi Referensi & Deteksi GPS
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_location_ref_title),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        // Current active location badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_location_active),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = prefs.userLocationName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.gempa_coordinates, "${String.format(java.util.Locale.US, "%.4f", prefs.userLat)}, ${String.format(java.util.Locale.US, "%.4f", prefs.userLon)}"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // GPS Auto-Detect Button
                        OutlinedButton(
                            onClick = onTriggerGps,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isDetectingGps
                        ) {
                            if (isDetectingGps) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_gps_detecting))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.settings_gps_btn),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Quick Presets
                        Text(
                            text = stringResource(R.string.settings_location_presets),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val cities = listOf(
                                Triple(-6.2088, 106.8456, "Jakarta"),
                                Triple(-6.9175, 107.6191, "Bandung"),
                                Triple(-7.2575, 112.7521, "Surabaya"),
                                Triple(-7.7956, 110.3695, "Yogyakarta"),
                                Triple(-7.9839, 112.6214, "Malang"),
                                Triple(-6.9667, 110.4167, "Semarang"),
                                Triple(3.5952, 98.6722, "Medan"),
                                Triple(-5.1477, 119.4327, "Makassar"),
                                Triple(-8.4095, 115.1889, "Bali"),
                                Triple(-0.9471, 100.4172, "Padang"),
                                Triple(-1.2654, 116.8312, "Balikpapan"),
                                Triple(-2.5489, 140.7181, "Jayapura")
                            )
                            cities.forEach { (lat, lon, name) ->
                                FilterChip(
                                    selected = prefs.userLocationName == name,
                                    onClick = { viewModel.setUserLocation(lat, lon, name) },
                                    label = { Text(name) },
                                    enabled = !isDetectingGps
                                )
                            }
                        }
                    }

                    // Button Uji Coba Getar & Notifikasi
                    Button(
                        onClick = {
                            val notifsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                            viewModel.triggerTestAlert(context)
                            if (!notifsEnabled) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_notif_permission_missing),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_test_alert_feedback),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_test_alert_btn),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // SECTION 2: TAMPILAN (TEMA GELAP / TERANG)
            Text(
                text = stringResource(R.string.settings_section_appearance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme_title),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = prefs.themeMode == AppThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM) },
                            label = { Text(stringResource(R.string.settings_theme_system)) },
                            leadingIcon = {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = prefs.themeMode == AppThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                            label = { Text(stringResource(R.string.settings_theme_light)) },
                            leadingIcon = {
                                Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = prefs.themeMode == AppThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
                            label = { Text(stringResource(R.string.settings_theme_dark)) },
                            leadingIcon = {
                                Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // SECTION 3: BAHASA (LANGUAGE)
            Text(
                text = stringResource(R.string.settings_section_language),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = prefs.language == "in",
                            onClick = { viewModel.setLanguage("in") },
                            label = { Text(stringResource(R.string.settings_lang_id)) },
                            leadingIcon = {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = prefs.language == "en",
                            onClick = { viewModel.setLanguage("en") },
                            label = { Text(stringResource(R.string.settings_lang_en)) },
                            leadingIcon = {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // SECTION 4: SUMBER RESMI BMKG
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🏛️ " + stringResource(R.string.about_bmkg_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.bmkg_attribution),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
