package com.example.siagabumi.ui.dashboard

import android.content.Intent
import android.net.Uri
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
import com.example.siagabumi.ui.components.BmkgAttributionFooter
import com.example.siagabumi.ui.components.ErrorView
import com.example.siagabumi.ui.components.LoadingView
import com.example.siagabumi.ui.components.OfflineBanner
import com.example.siagabumi.ui.cuaca.components.CurrentWeatherCard
import com.example.siagabumi.ui.dashboard.components.DashboardEmergencyGuide
import com.example.siagabumi.ui.dashboard.components.DashboardMapRadar
import com.example.siagabumi.ui.dashboard.components.DashboardQuickStats
import com.example.siagabumi.ui.dashboard.components.DashboardStatusHero

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

            // Interactive Live Earthquake Radar (With map controls & marker click)
            item {
                val allQuakes = listOfNotNull(data.gempaTerbaru) + data.gempaTerkiniList
                DashboardMapRadar(
                    quakeList = allQuakes,
                    onViewAllClick = onNavigateToGempa,
                    onQuakeClick = { gempaId -> onNavigateToGempaDetail(gempaId) }
                )
            }

            // Quick Weather Glance
            data.cuacaDetail?.let { cuaca ->
                item {
                    CurrentWeatherCard(
                        cuacaDetail = cuaca,
                        onChangeLocationClick = onNavigateToSearchWilayah,
                        onToggleFavoriteClick = { onNavigateToCuaca(cuaca.lokasi.kode) }
                    )
                }
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
