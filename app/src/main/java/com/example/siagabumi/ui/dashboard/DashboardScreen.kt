package com.example.siagabumi.ui.dashboard

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.siagabumi.R
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.domain.model.Wilayah
import com.example.siagabumi.ui.components.BmkgAttributionFooter
import com.example.siagabumi.ui.components.ErrorView
import com.example.siagabumi.ui.components.LoadingView
import com.example.siagabumi.ui.components.OfflineBanner
import com.example.siagabumi.ui.cuaca.components.CurrentWeatherCard
import com.example.siagabumi.ui.dashboard.components.DashboardEmergencyGuide
import com.example.siagabumi.ui.dashboard.components.DashboardEpicenterRadar
import com.example.siagabumi.ui.dashboard.components.DashboardQuickStats
import com.example.siagabumi.ui.dashboard.components.DashboardStatusHero
import com.example.siagabumi.ui.dashboard.components.EmergencyFlashlightCard
import com.example.siagabumi.ui.dashboard.components.WeatherCardPager

@Composable
fun DashboardScreen(
    onNavigateToGempa: () -> Unit,
    onNavigateToCuaca: (String?) -> Unit,
    onNavigateToSearchWilayah: () -> Unit,
    onNavigateToGempaDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDetectingGps by viewModel.isDetectingGps.collectAsState()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
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

    LaunchedEffect(Unit) {
        viewModel.onScreenEntered { needPermission ->
            if (needPermission) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    when (val state = uiState) {
        is UiState.Loading -> {
            LoadingView(modifier = modifier)
        }
        is UiState.Error -> {
            ErrorView(
                message = state.message,
                onRetry = { viewModel.loadData() },
                modifier = modifier
            )
        }
        is UiState.Success -> {
            DashboardContent(
                data = state.data,
                onRefresh = { viewModel.refresh() },
                onNavigateToGempa = onNavigateToGempa,
                onNavigateToCuaca = onNavigateToCuaca,
                onNavigateToSearchWilayah = onNavigateToSearchWilayah,
                onNavigateToGempaDetail = onNavigateToGempaDetail,
                onNavigateToSettings = onNavigateToSettings,
                onSelectLocationAsActive = { viewModel.setActiveLocation(it) },
                onToggleFavoriteLocation = { viewModel.toggleFavorit(it) },
                onSyncGps = onTriggerGps,
                isSyncingGps = isDetectingGps,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    data: DashboardScreenData,
    onRefresh: () -> Unit,
    onNavigateToGempa: () -> Unit,
    onNavigateToCuaca: (String?) -> Unit,
    onNavigateToSearchWilayah: () -> Unit,
    onNavigateToGempaDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onSelectLocationAsActive: (Wilayah) -> Unit = {},
    onToggleFavoriteLocation: (Wilayah) -> Unit = {},
    onSyncGps: () -> Unit = {},
    isSyncingGps: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = data.isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (data.isOfflineCached) {
                item {
                    OfflineBanner(lastUpdated = data.lastUpdatedText)
                }
            }

            // Hero Status Banner
            item {
                DashboardStatusHero(
                    isTsunamiAlert = data.isTsunamiAlert,
                    hasSignificantQuake = data.hasSignificantQuake,
                    latestGempa = data.gempaTerbaru,
                    onCardClick = {
                        data.gempaTerbaru?.let { onNavigateToGempaDetail(it.id) } ?: onNavigateToGempa()
                    }
                )
            }

            // Quick Interactive Action Buttons Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Panggilan Darurat
                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:117"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.dashboard_btn_quick_emergency),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    // Ubah Lokasi
                    FilledTonalButton(
                        onClick = onNavigateToSearchWilayah,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.dashboard_btn_change_location),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    // Pengaturan Siaga
                    FilledTonalButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.dashboard_btn_settings),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // Quick Stats Grid (4 metrics)
            item {
                DashboardQuickStats(
                    data = data,
                    onQuakeClick = onNavigateToGempa,
                    onWeatherClick = { onNavigateToCuaca(data.cuacaDetail?.lokasi?.kode) }
                )
            }

            // Epicenter Impact Radar & Official BMKG Shakemap
            item {
                DashboardEpicenterRadar(
                    gempa = data.gempaTerbaru ?: data.gempaTerkiniList.firstOrNull(),
                    userPreferences = data.userPreferences,
                    onViewAllClick = onNavigateToGempa,
                    onOpenDetailClick = onNavigateToGempaDetail,
                    onSyncGpsClick = onSyncGps,
                    isSyncingGps = isSyncingGps
                )
            }

            // Multi-Location Horizontal Weather Cards (Swipeable)
            val displayWeatherList = data.cuacaDetailList.ifEmpty { listOfNotNull(data.cuacaDetail) }
            item {
                WeatherCardPager(
                    weatherList = displayWeatherList,
                    activeAdm4 = data.userPreferences.userAdm4.ifBlank { "31.71.03.1001" },
                    onSelectLocationAsActive = onSelectLocationAsActive,
                    onToggleFavorite = onToggleFavoriteLocation,
                    onChangeLocationClick = { onNavigateToCuaca(it.kode) },
                    onAddNewLocationClick = onNavigateToSearchWilayah
                )
            }

            // Emergency Flashlight & SOS Strobe Tool
            item {
                EmergencyFlashlightCard()
            }

            // Emergency Call Hotlines & Mitigation Tips
            item {
                DashboardEmergencyGuide()
            }

            // BMKG Mandatory Attribution Footer
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BmkgAttributionFooter()
            }
        }
    }
}
