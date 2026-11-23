package com.example.siagabumi.ui.cuaca.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.siagabumi.R
import com.example.siagabumi.domain.model.CuacaDetail

@Composable
fun CurrentWeatherCard(
    cuacaDetail: CuacaDetail,
    onChangeLocationClick: () -> Unit,
    onToggleFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val current = cuacaDetail.kondisiSaatIni
    val lokasi = cuacaDetail.lokasi

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Location Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lokasi.nama,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = lokasi.namaLengkap,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Row {
                    IconButton(onClick = onToggleFavoriteClick) {
                        Icon(
                            imageVector = if (cuacaDetail.isFavorit) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.nav_favorit),
                            tint = if (cuacaDetail.isFavorit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onChangeLocationClick) {
                        Icon(
                            imageVector = Icons.Default.EditLocation,
                            contentDescription = stringResource(R.string.wilayah_search_title),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Weather Hero
            if (current != null) {
                val isEn = LocalConfiguration.current.locales[0].language == "en"
                val displayDesc = if (isEn && current.weatherDescEn.isNotBlank()) current.weatherDescEn else current.weatherDesc

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.cuaca_temp, current.suhu),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = displayDesc,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (current.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(current.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = displayDesc,
                            modifier = Modifier.size(90.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4 Metric Items Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WeatherMetricItem(
                        icon = Icons.Default.InvertColors,
                        label = stringResource(R.string.cuaca_humidity),
                        value = stringResource(R.string.cuaca_humidity_val, current.kelembapan)
                    )
                    WeatherMetricItem(
                        icon = Icons.Default.Air,
                        label = stringResource(R.string.cuaca_wind),
                        value = stringResource(R.string.cuaca_wind_val, current.kecepatanAngin, current.arahAngin)
                    )
                    WeatherMetricItem(
                        icon = Icons.Default.Cloud,
                        label = stringResource(R.string.cuaca_clouds),
                        value = stringResource(R.string.cuaca_clouds_val, current.tutupanAwan)
                    )
                    WeatherMetricItem(
                        icon = Icons.Default.WaterDrop,
                        label = stringResource(R.string.cuaca_rain),
                        value = stringResource(R.string.cuaca_rain_val, current.curahHujan)
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.cuaca_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun WeatherMetricItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
