package com.example.siagabumi.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object Gempa : Screen("gempa")
    data object Cuaca : Screen("cuaca?adm4={adm4}") {
        fun createRoute(adm4: String? = null): String {
            return if (!adm4.isNullOrEmpty()) {
                "cuaca?adm4=${URLEncoder.encode(adm4, StandardCharsets.UTF_8.toString())}"
            } else {
                "cuaca"
            }
        }
    }
    data object Favorit : Screen("favorit")
    data object About : Screen("about")
    data object GempaDetail : Screen("gempa_detail/{gempaId}") {
        fun createRoute(gempaId: String): String {
            return "gempa_detail/${URLEncoder.encode(gempaId, StandardCharsets.UTF_8.toString())}"
        }
    }
    data object WilayahSearch : Screen("wilayah_search")
}
