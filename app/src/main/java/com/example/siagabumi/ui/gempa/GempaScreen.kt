package com.example.siagabumi.ui.gempa

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.siagabumi.R
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.ui.components.BmkgAttributionFooter
import com.example.siagabumi.ui.components.ErrorView
import com.example.siagabumi.ui.components.GempaItemCard
import com.example.siagabumi.ui.components.GempaTerbaruCard
import com.example.siagabumi.ui.components.LoadingView
import com.example.siagabumi.ui.components.OfflineBanner

@Composable
fun GempaScreen(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GempaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is UiState.Loading -> {
            LoadingView(modifier = modifier)
        }
        is UiState.Error -> {
            ErrorView(
                message = state.message,
                onRetry = { viewModel.loadInitialData() },
                modifier = modifier
            )
        }
        is UiState.Success -> {
            GempaContent(
                data = state.data,
                onSelectTab = { viewModel.selectTab(it) },
                onSelectMinMag = { viewModel.setMinMagnitudeFilter(it) },
                onRefresh = { viewModel.refresh() },
                onNavigateToDetail = onNavigateToDetail,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GempaContent(
    data: GempaScreenData,
    onSelectTab: (Int) -> Unit,
    onSelectMinMag: (Double) -> Unit,
    onRefresh: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Offline Cache Banner
            if (data.isOfflineCached) {
                item {
                    OfflineBanner(lastUpdated = data.lastUpdatedText)
                }
            }

            // Gempa Terbaru Hero Card
            data.gempaTerbaru?.let { terbaru ->
                item {
                    GempaTerbaruCard(
                        gempa = terbaru,
                        onCardClick = { onNavigateToDetail(terbaru.id) }
                    )
                }
            }

            // Tab Selection: M 5.0+ vs Dirasakan
            item {
                SecondaryTabRow(
                    selectedTabIndex = data.selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = data.selectedTab == 0,
                        onClick = { onSelectTab(0) },
                        text = {
                            Text(
                                text = stringResource(R.string.gempa_tab_terkini),
                                fontWeight = if (data.selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = data.selectedTab == 1,
                        onClick = { onSelectTab(1) },
                        text = {
                            Text(
                                text = stringResource(R.string.gempa_tab_dirasakan),
                                fontWeight = if (data.selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // Magnitude Filter Chips
            item {
                val filterOptions = listOf(
                    0.0 to stringResource(R.string.gempa_all_mag),
                    5.5 to "M ≥ 5.5",
                    6.0 to "M ≥ 6.0",
                    6.5 to "M ≥ 6.5",
                    7.0 to "M ≥ 7.0"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { (mag, label) ->
                        FilterChip(
                            selected = data.minMagnitudeFilter == mag,
                            onClick = { onSelectMinMag(mag) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Earthquakes list
            val items = data.displayedList
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.gempa_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(
                    items = items,
                    key = { it.id }
                ) { gempa ->
                    GempaItemCard(
                        gempa = gempa,
                        onClick = { onNavigateToDetail(gempa.id) }
                    )
                }
            }

            // Mandatory BMKG Attribution Footer
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BmkgAttributionFooter()
            }
        }
    }
}
