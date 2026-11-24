package com.example.siagabumi.ui.cuaca

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.siagabumi.domain.model.CuacaDetail
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.ui.components.BmkgAttributionFooter
import com.example.siagabumi.ui.components.ErrorView
import com.example.siagabumi.ui.components.LoadingView
import com.example.siagabumi.ui.components.OfflineBanner
import com.example.siagabumi.ui.cuaca.components.CurrentWeatherCard
import com.example.siagabumi.ui.cuaca.components.DailyForecastList
import com.example.siagabumi.ui.cuaca.components.HourlyForecastStrip

@Composable
fun CuacaScreen(
    onNavigateToSearchWilayah: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CuacaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is UiState.Loading -> {
            LoadingView(modifier = modifier)
        }
        is UiState.Error -> {
            ErrorView(
                message = state.message,
                onRetry = { viewModel.refresh() },
                modifier = modifier
            )
        }
        is UiState.Success -> {
            CuacaContent(
                cuacaDetail = state.data,
                isFromCache = state.isFromCache,
                lastUpdated = state.lastUpdated,
                onRefresh = { viewModel.refresh() },
                onChangeLocation = onNavigateToSearchWilayah,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuacaContent(
    cuacaDetail: CuacaDetail,
    isFromCache: Boolean,
    lastUpdated: String?,
    onRefresh: () -> Unit,
    onChangeLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isFromCache) {
                item {
                    OfflineBanner(lastUpdated = lastUpdated)
                }
            }

            item {
                CurrentWeatherCard(
                    cuacaDetail = cuacaDetail,
                    onChangeLocationClick = onChangeLocation
                )
            }

            item {
                HourlyForecastStrip(hourlyList = cuacaDetail.prakiraanHariIni3Jam)
            }

            item {
                DailyForecastList(dailyList = cuacaDetail.ringkasanHarian)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                BmkgAttributionFooter()
            }
        }
    }
}
