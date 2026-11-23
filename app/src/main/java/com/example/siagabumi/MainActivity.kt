package com.example.siagabumi

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.siagabumi.domain.model.AppThemeMode
import com.example.siagabumi.domain.model.UserPreferences
import com.example.siagabumi.domain.repository.UserPreferencesRepository
import com.example.siagabumi.ui.navigation.Screen
import com.example.siagabumi.ui.theme.SiagaBumiTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userPrefs by userPreferencesRepository.getUserPreferences()
                .collectAsState(initial = UserPreferences())

            val isDark = when (userPrefs.themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val context = LocalContext.current
            val targetLocale = remember(userPrefs.language) {
                if (userPrefs.language == "en") Locale.ENGLISH else Locale.forLanguageTag("in")
            }
            val localizedContext = remember(userPrefs.language, context) {
                val config = Configuration(context.resources.configuration).apply {
                    setLocale(targetLocale)
                    setLayoutDirection(targetLocale)
                }
                context.createConfigurationContext(config)
            }
            val localizedConfig = remember(userPrefs.language, localizedContext) {
                localizedContext.resources.configuration
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfig,
                LocalContext provides localizedContext
            ) {
                SiagaBumiTheme(darkTheme = isDark) {
                    MainApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Triple(Screen.Dashboard.route, R.string.nav_dashboard, Icons.Default.Dashboard),
        Triple(Screen.Gempa.route, R.string.nav_gempa, Icons.Default.Warning),
        Triple(Screen.Cuaca.createRoute(), R.string.nav_cuaca, Icons.Default.Cloud),
        Triple(Screen.Favorit.route, R.string.nav_favorit, Icons.Default.Favorite)
    )

    val isTopLevelDestination = currentDestination in listOf(
        Screen.Dashboard.route,
        Screen.Gempa.route,
        Screen.Cuaca.route,
        Screen.Cuaca.createRoute(),
        Screen.Favorit.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            currentDestination == Screen.Dashboard.route -> stringResource(R.string.dashboard_title)
                            currentDestination == Screen.Gempa.route -> stringResource(R.string.nav_gempa)
                            currentDestination?.startsWith("cuaca") == true -> stringResource(R.string.nav_cuaca)
                            currentDestination == Screen.Favorit.route -> stringResource(R.string.nav_favorit)
                            currentDestination == Screen.About.route -> stringResource(R.string.nav_about)
                            currentDestination == Screen.Settings.route -> stringResource(R.string.settings_title)
                            currentDestination?.startsWith("gempa_detail") == true -> stringResource(R.string.gempa_detail_title)
                            currentDestination == Screen.WilayahSearch.route -> stringResource(R.string.wilayah_search_title)
                            else -> stringResource(R.string.app_name)
                        }
                    )
                },
                navigationIcon = {
                    if (!isTopLevelDestination) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (isTopLevelDestination) {
                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_title)
                            )
                        }
                        IconButton(onClick = { navController.navigate(Screen.About.route) }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(R.string.nav_about)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar {
                    bottomNavItems.forEach { (route, labelRes, icon) ->
                        val selected = currentDestination == route || (route.startsWith("cuaca") && currentDestination?.startsWith("cuaca") == true)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = stringResource(labelRes)) },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                com.example.siagabumi.ui.dashboard.DashboardScreen(
                    onNavigateToGempa = {
                        navController.navigate(Screen.Gempa.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCuaca = { adm4 ->
                        navController.navigate(Screen.Cuaca.createRoute(adm4))
                    },
                    onNavigateToSearchWilayah = {
                        navController.navigate(Screen.WilayahSearch.route)
                    },
                    onNavigateToGempaDetail = { gempaId ->
                        navController.navigate(Screen.GempaDetail.createRoute(gempaId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(Screen.Gempa.route) {
                com.example.siagabumi.ui.gempa.GempaScreen(
                    onNavigateToDetail = { gempaId ->
                        navController.navigate(Screen.GempaDetail.createRoute(gempaId))
                    }
                )
            }
            composable(
                route = Screen.Cuaca.route,
                arguments = listOf(
                    androidx.navigation.navArgument("adm4") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                com.example.siagabumi.ui.cuaca.CuacaScreen(
                    onNavigateToSearchWilayah = {
                        navController.navigate(Screen.WilayahSearch.route)
                    }
                )
            }
            composable(Screen.Favorit.route) {
                com.example.siagabumi.ui.favorit.FavoritScreen(
                    onNavigateToCuaca = { adm4 ->
                        navController.navigate(Screen.Cuaca.createRoute(adm4))
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.WilayahSearch.route)
                    }
                )
            }
            composable(Screen.About.route) {
                com.example.siagabumi.ui.about.AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                com.example.siagabumi.ui.settings.SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.GempaDetail.route) {
                com.example.siagabumi.ui.gempa.detail.GempaDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WilayahSearch.route) {
                com.example.siagabumi.ui.wilayah.WilayahSearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onWilayahSelected = { adm4 ->
                        navController.navigate(Screen.Cuaca.createRoute(adm4)) {
                            popUpTo(Screen.Cuaca.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }
    }
}
