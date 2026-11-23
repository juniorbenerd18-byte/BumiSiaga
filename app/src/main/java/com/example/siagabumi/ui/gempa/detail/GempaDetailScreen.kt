package com.example.siagabumi.ui.gempa.detail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.siagabumi.R
import com.example.siagabumi.domain.model.Gempa
import com.example.siagabumi.domain.model.UiState
import com.example.siagabumi.ui.components.BmkgAttributionFooter
import com.example.siagabumi.ui.components.ErrorView
import com.example.siagabumi.ui.components.LoadingView
import com.example.siagabumi.ui.components.MagnitudeBadge
import com.example.siagabumi.ui.components.TsunamiWarningBadge
import com.example.siagabumi.ui.components.ZoomableShakemap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GempaDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GempaDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gempa_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState is UiState.Success) {
                        val gempa = (uiState as UiState.Success<Gempa>).data
                        IconButton(onClick = { shareGempa(context, gempa) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.gempa_share)
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                LoadingView(modifier = Modifier.padding(innerPadding))
            }
            is UiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is UiState.Success -> {
                GempaDetailContent(
                    gempa = state.data,
                    onShareClick = { shareGempa(context, state.data) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun GempaDetailContent(
    gempa: Gempa,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tsunami warning badge
        if (gempa.isTsunamiWarning) {
            item {
                TsunamiWarningBadge()
            }
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MagnitudeBadge(
                            magnitude = gempa.magnitude,
                            size = 64.dp,
                            fontSize = 24
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gempa.wilayah,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.gempa_depth, gempa.kedalamanKm),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    DetailItemRow(
                        icon = Icons.Default.AccessTime,
                        label = stringResource(R.string.gempa_detail_title),
                        value = "${gempa.formattedLocalTime}\n(${gempa.jam})"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailItemRow(
                        icon = Icons.Default.Place,
                        label = stringResource(R.string.gempa_coordinates),
                        value = "${gempa.coordinates} (${gempa.lintang}, ${gempa.bujur})"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailItemRow(
                        icon = Icons.Default.Info,
                        label = stringResource(R.string.gempa_potential_label),
                        value = gempa.potensi
                    )

                    if (!gempa.dirasakan.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailItemRow(
                            icon = Icons.Default.Info,
                            label = stringResource(R.string.gempa_felt_title),
                            value = gempa.dirasakan
                        )
                    }
                }
            }
        }

        // Official BMKG Shakemap (Peta Guncangan Resmi)
        if (!gempa.shakemapUrl.isNullOrEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.gempa_shakemap),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.dashboard_shakemap_tap_to_zoom),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ZoomableShakemap(imageUrl = gempa.shakemapUrl)
                }
            }
        }

        // Share Button
        item {
            Button(
                onClick = onShareClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.gempa_share),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        // Attribution
        item {
            BmkgAttributionFooter()
        }
    }
}

@Composable
private fun DetailItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun shareGempa(context: Context, gempa: Gempa) {
    val feltText = if (!gempa.dirasakan.isNullOrBlank()) {
        "Dirasakan: ${gempa.dirasakan}"
    } else ""
    val sendText = context.getString(
        R.string.gempa_share_text,
        gempa.magnitude,
        gempa.kedalamanKm,
        gempa.wilayah,
        gempa.formattedLocalTime.ifEmpty { "${gempa.tanggal} ${gempa.jam}" },
        gempa.potensi,
        feltText
    )
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, sendText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.gempa_share)))
}
