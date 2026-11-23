package com.example.siagabumi.data.repository

import android.content.Context
import com.example.siagabumi.data.local.dao.WilayahDao
import com.example.siagabumi.data.local.entity.WilayahEntity
import com.example.siagabumi.domain.model.Wilayah
import com.example.siagabumi.domain.repository.WilayahRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WilayahRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val wilayahDao: WilayahDao
) : WilayahRepository {

    companion object {
        val DEFAULT_WILAYAH = Wilayah(
            kode = "31.71.03.1001",
            nama = "Kemayoran",
            level = 4,
            parentKode = "31.71.03",
            namaLengkap = "Kemayoran, Kemayoran, Kota Adm. Jakarta Pusat, DKI Jakarta"
        )

        val SAMPLE_WILAYAH_ENTITIES = listOf(
            WilayahEntity("31", "DKI Jakarta", 1, null, "DKI Jakarta"),
            WilayahEntity("31.71", "Kota Adm. Jakarta Pusat", 2, "31", "Kota Adm. Jakarta Pusat, DKI Jakarta"),
            WilayahEntity("31.71.03", "Kemayoran", 3, "31.71", "Kemayoran, Kota Adm. Jakarta Pusat, DKI Jakarta"),
            WilayahEntity("31.71.03.1001", "Kemayoran", 4, "31.71.03", "Kemayoran, Kemayoran, Kota Adm. Jakarta Pusat, DKI Jakarta"),

            WilayahEntity("31.73", "Kota Adm. Jakarta Selatan", 2, "31", "Kota Adm. Jakarta Selatan, DKI Jakarta"),
            WilayahEntity("31.73.04", "Menteng", 3, "31.73", "Menteng, Kota Adm. Jakarta Selatan, DKI Jakarta"),
            WilayahEntity("31.73.04.1001", "Menteng", 4, "31.73.04", "Menteng, Menteng, Kota Adm. Jakarta Selatan, DKI Jakarta"),

            WilayahEntity("32", "Jawa Barat", 1, null, "Jawa Barat"),
            WilayahEntity("32.73", "Kota Bandung", 2, "32", "Kota Bandung, Jawa Barat"),
            WilayahEntity("32.73.01", "Coblong", 3, "32.73", "Coblong, Kota Bandung, Jawa Barat"),
            WilayahEntity("32.73.01.1001", "Dago", 4, "32.73.01", "Dago, Coblong, Kota Bandung, Jawa Barat"),

            WilayahEntity("33", "Jawa Tengah", 1, null, "Jawa Tengah"),
            WilayahEntity("33.74", "Kota Semarang", 2, "33", "Kota Semarang, Jawa Tengah"),
            WilayahEntity("33.74.01", "Candisari", 3, "33.74", "Candisari, Kota Semarang, Jawa Tengah"),
            WilayahEntity("33.74.01.1001", "Candisari", 4, "33.74.01", "Candisari, Candisari, Kota Semarang, Jawa Tengah"),

            WilayahEntity("34", "DI Yogyakarta", 1, null, "DI Yogyakarta"),
            WilayahEntity("34.71", "Kota Yogyakarta", 2, "34", "Kota Yogyakarta, DI Yogyakarta"),
            WilayahEntity("34.71.01", "Gedongtengen", 3, "34.71", "Gedongtengen, Kota Yogyakarta, DI Yogyakarta"),
            WilayahEntity("34.71.01.1001", "Sosromenduran", 4, "34.71.01", "Sosromenduran, Gedongtengen, Kota Yogyakarta, DI Yogyakarta"),

            WilayahEntity("35", "Jawa Timur", 1, null, "Jawa Timur"),
            WilayahEntity("35.78", "Kota Surabaya", 2, "35", "Kota Surabaya, Jawa Timur"),
            WilayahEntity("35.78.01", "Gubeng", 3, "35.78", "Gubeng, Kota Surabaya, Jawa Timur"),
            WilayahEntity("35.78.01.1001", "Gubeng", 4, "35.78.01", "Gubeng, Gubeng, Kota Surabaya, Jawa Timur"),

            WilayahEntity("51", "Bali", 1, null, "Bali"),
            WilayahEntity("51.71", "Kota Denpasar", 2, "51", "Kota Denpasar, Bali"),
            WilayahEntity("51.71.01", "Denpasar Selatan", 3, "51.71", "Denpasar Selatan, Kota Denpasar, Bali"),
            WilayahEntity("51.71.01.1001", "Sanur", 4, "51.71.01", "Sanur, Denpasar Selatan, Kota Denpasar, Bali"),

            WilayahEntity("12", "Sumatera Utara", 1, null, "Sumatera Utara"),
            WilayahEntity("12.71", "Kota Medan", 2, "12", "Kota Medan, Sumatera Utara"),
            WilayahEntity("12.71.01", "Medan Kota", 3, "12.71", "Medan Kota, Kota Medan, Sumatera Utara"),
            WilayahEntity("12.71.01.1001", "Medan Kota", 4, "12.71.01", "Medan Kota, Medan Kota, Kota Medan, Sumatera Utara"),

            WilayahEntity("73", "Sulawesi Selatan", 1, null, "Sulawesi Selatan"),
            WilayahEntity("73.71", "Kota Makassar", 2, "73", "Kota Makassar, Sulawesi Selatan"),
            WilayahEntity("73.71.01", "Ujung Pandang", 3, "73.71", "Ujung Pandang, Kota Makassar, Sulawesi Selatan"),
            WilayahEntity("73.71.01.1001", "Ujung Pandang", 4, "73.71.01", "Ujung Pandang, Ujung Pandang, Kota Makassar, Sulawesi Selatan"),

            WilayahEntity("91", "Papua", 1, null, "Papua"),
            WilayahEntity("91.71", "Kota Jayapura", 2, "91", "Kota Jayapura, Papua"),
            WilayahEntity("91.71.01", "Jayapura Utara", 3, "91.71", "Jayapura Utara, Kota Jayapura, Papua"),
            WilayahEntity("91.71.01.1001", "Jayapura Utara", 4, "91.71.01", "Jayapura Utara, Jayapura Utara, Kota Jayapura, Papua")
        )
    }

    override fun searchWilayah(query: String): Flow<List<Wilayah>> {
        return wilayahDao.searchWilayah(query.trim()).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getWilayahByKode(kode: String): Wilayah? {
        val entity = wilayahDao.getWilayahByKode(kode)
        return entity?.toDomain()
    }

    override suspend fun getDefaultWilayah(): Wilayah {
        val local = wilayahDao.getWilayahByKode(DEFAULT_WILAYAH.kode)
        return local?.toDomain() ?: DEFAULT_WILAYAH
    }

    override suspend fun resolveToAdm4(kode: String): String {
        if (kode.count { it == '.' } >= 3) return kode
        val entity = wilayahDao.findLevel4Wilayah(kode)
        return entity?.kode ?: kode
    }

    override suspend fun ensureWilayahDataSeeded(onProgress: ((Float) -> Unit)?) {
        withContext(Dispatchers.IO) {
            val count = wilayahDao.countWilayah()
            // If already seeded with the rich dataset (100+ entities), skip
            if (count >= 100) {
                onProgress?.invoke(1f)
                return@withContext
            }

            try {
                val inputStream = context.assets.open("wilayah.csv")
                val reader = BufferedReader(InputStreamReader(inputStream))
                val rawLines = reader.readLines()
                reader.close()

                val codeToName = mutableMapOf<String, String>()
                val parsedRaw = mutableListOf<Pair<String, String>>()

                for (line in rawLines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("kode", ignoreCase = true)) continue
                    val parts = trimmed.split(",")
                    if (parts.size >= 2) {
                        val kode = parts[0].trim()
                        val nama = parts.subList(1, parts.size).joinToString(",").trim()
                        if (kode.isNotEmpty() && nama.isNotEmpty()) {
                            parsedRaw.add(kode to nama)
                            codeToName[kode] = nama
                        }
                    }
                }

                val total = parsedRaw.size
                val entities = mutableListOf<WilayahEntity>()

                for ((index, item) in parsedRaw.withIndex()) {
                    val (kode, nama) = item
                    val level = kode.count { it == '.' } + 1
                    val parentKode = if (kode.contains('.')) kode.substringBeforeLast('.') else null

                    val namaLengkap = when (level) {
                        4 -> {
                            val kecKode = kode.substringBeforeLast('.')
                            val kabKode = if (kecKode.contains('.')) kecKode.substringBeforeLast('.') else ""
                            val provKode = if (kabKode.contains('.')) kabKode.substringBeforeLast('.') else kabKode
                            val kec = codeToName[kecKode].orEmpty()
                            val kab = codeToName[kabKode].orEmpty()
                            val prov = codeToName[provKode].orEmpty()
                            listOf(nama, kec, kab, prov).filter { it.isNotEmpty() }.joinToString(", ")
                        }
                        3 -> {
                            val kabKode = kode.substringBeforeLast('.')
                            val provKode = if (kabKode.contains('.')) kabKode.substringBeforeLast('.') else kabKode
                            val kab = codeToName[kabKode].orEmpty()
                            val prov = codeToName[provKode].orEmpty()
                            listOf(nama, kab, prov).filter { it.isNotEmpty() }.joinToString(", ")
                        }
                        2 -> {
                            val provKode = kode.substringBeforeLast('.')
                            val prov = codeToName[provKode].orEmpty()
                            listOf(nama, prov).filter { it.isNotEmpty() }.joinToString(", ")
                        }
                        else -> nama
                    }

                    entities.add(
                        WilayahEntity(
                            kode = kode,
                            nama = nama,
                            level = level,
                            parentKode = parentKode,
                            namaLengkap = namaLengkap
                        )
                    )

                    if (entities.size >= 500) {
                        wilayahDao.insertAll(entities)
                        entities.clear()
                        onProgress?.invoke((index + 1).toFloat() / total.toFloat())
                    }
                }

                if (entities.isNotEmpty()) {
                    wilayahDao.insertAll(entities)
                    entities.clear()
                }

                onProgress?.invoke(1f)
            } catch (_: Exception) {
                // If CSV fails to load or does not exist, insert the 10 built-in sample regions
                wilayahDao.insertAll(SAMPLE_WILAYAH_ENTITIES)
                onProgress?.invoke(1f)
            }
        }
    }

    private fun WilayahEntity.toDomain(): Wilayah {
        return Wilayah(
            kode = kode,
            nama = nama,
            level = level,
            parentKode = parentKode,
            namaLengkap = namaLengkap
        )
    }
}
