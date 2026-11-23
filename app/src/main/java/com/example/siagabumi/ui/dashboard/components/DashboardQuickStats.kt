package com.example.siagabumi.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siagabumi.R
import com.example.siagabumi.ui.dashboard.DashboardScreenData
import com.example.siagabumi.ui.theme.MagGreen
import com.example.siagabumi.ui.theme.MagOrange
import com.example.siagabumi.ui.theme.MagRed

@Composable
fun DashboardQuickStats(
    data: DashboardScreenData,
    onQuakeClick: () -> Unit,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_quick_stats),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stat 1: Gempa Terkini
            QuickStatCard(
                icon = Icons.Default.Warning,
                iconTint = MagOrange,
                label = stringResource(R.string.dashboard_stat_latest_quake),
                value = data.gempaTerbaru?.let { "M ${it.magnitude}" } ?: "-",
                subValue = data.gempaTerbaru?.wilayah?.take(18)?.let { "$it…" } ?: "-",
                onClick = onQuakeClick,
                modifier = Modifier.weight(1f)
            )

            // Stat 2: Gempa M5+
            QuickStatCard(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                iconTint = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.dashboard_stat_m5_count),
                value = stringResource(R.string.dashboard_stat_m5_active, data.m5PlusCount),
                subValue = stringResource(R.string.dashboard_stat_bmkg_catalog),
                onClick = onQuakeClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stat 3: Cuaca Lokal
            val isEn = LocalConfiguration.current.locales[0].language == "en"
            val cuaca = data.cuacaDetail
            val weatherDesc = cuaca?.kondisiSaatIni?.let {
                if (isEn && it.weatherDescEn.isNotBlank()) it.weatherDescEn else it.weatherDesc
            } ?: stringResource(R.string.dashboard_stat_view_weather)

            val weatherLabel = if (data.userPreferences.isGpsLocation) {
                "📍 " + (cuaca?.lokasi?.nama ?: data.userPreferences.userLocationName.split(",").firstOrNull()?.trim() ?: stringResource(R.string.dashboard_stat_current_temp))
            } else {
                cuaca?.lokasi?.nama ?: stringResource(R.string.dashboard_stat_current_temp)
            }

            QuickStatCard(
                icon = Icons.Default.WbSunny,
                iconTint = Color(0xFFF59E0B),
                label = weatherLabel,
                value = cuaca?.kondisiSaatIni?.let { "${it.suhu}°C" } ?: "-",
                subValue = weatherDesc,
                onClick = onWeatherClick,
                modifier = Modifier.weight(1f)
            )

            // Stat 4: Status Tsunami
            val isTsunami = data.isTsunamiAlert
            QuickStatCard(
                icon = Icons.Default.CrisisAlert,
                iconTint = if (isTsunami) MagRed else MagGreen,
                label = stringResource(R.string.dashboard_stat_tsunami_status),
                value = if (isTsunami) stringResource(R.string.dashboard_stat_alert_label) else stringResource(R.string.dashboard_stat_safe_label),
                subValue = if (isTsunami) stringResource(R.string.dashboard_stat_tsunami_active) else stringResource(R.string.dashboard_stat_tsunami_safe),
                onClick = onQuakeClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickStatCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    subValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
    }
}
