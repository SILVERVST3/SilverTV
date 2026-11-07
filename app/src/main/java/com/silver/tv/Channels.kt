package com.silver.tv

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Locale

data class Channel(
    val name: String,
    val url: String,
    val logo: String?,
    val groups: List<String>,
    val userAgent: String?,
    val referrer: String?,
)

/** Leitor de listas M3U/M3U8 (formato usado pelo iptv-org e pela maioria das listas). */
object M3uParser {
    private val attrRegex = Regex("""([\w-]+)="([^"]*)"""")

    private val categoriasPt = mapOf(
        "animation" to "Animação", "auto" to "Automóveis", "business" to "Negócios",
        "classic" to "Clássicos", "comedy" to "Comédia", "cooking" to "Culinária",
        "culture" to "Cultura", "documentary" to "Documentários", "education" to "Educação",
        "entertainment" to "Entretenimento", "family" to "Família", "general" to "Geral",
        "kids" to "Infantil", "legislative" to "Legislativo", "lifestyle" to "Estilo de vida",
        "movies" to "Filmes", "music" to "Música", "news" to "Notícias", "outdoor" to "Ao ar livre",
        "public" to "Públicos", "relax" to "Relax", "religious" to "Religiosos",
        "science" to "Ciência", "series" to "Séries", "shop" to "Compras", "sports" to "Esportes",
        "travel" to "Viagem", "weather" to "Clima", "undefined" to "Outros",
    )

    private fun traduz(cat: String): String =
        categoriasPt[cat.trim().lowercase()] ?: cat.trim()

    fun parse(text: String): List<Channel> {
        val out = mutableListOf<Channel>()
        var name: String? = null
        var logo: String? = null
        var groups: List<String> = emptyList()
        var ua: String? = null
        var ref: String? = null

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val matches = attrRegex.findAll(line).toList()
                    val attrs = matches.associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                    val fimAttrs = matches.lastOrNull()?.range?.last ?: -1
                    val depoisAttrs = line.substring(fimAttrs + 1)
                    name = depoisAttrs.substringAfter(',', depoisAttrs).trim()
                    logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
                    groups = (attrs["group-title"] ?: "")
                        .split(';')
                        .map { traduz(it) }
                        .filter { it.isNotBlank() }
                        .ifEmpty { listOf("Outros") }
                    ua = attrs["http-user-agent"]?.takeIf { it.isNotBlank() }
                    ref = attrs["http-referrer"]?.takeIf { it.isNotBlank() }
                }
                line.startsWith("#EXTVLCOPT:http-user-agent=", ignoreCase = true) ->
                    ua = line.substringAfter('=').trim()
                line.startsWith("#EXTVLCOPT:http-referrer=", ignoreCase = true) ->
                    ref = line.substringAfter('=').trim()
                line.isNotEmpty() && !line.startsWith("#") && name != null -> {
                    out += Channel(
                        name = name!!.ifBlank { "Canal sem nome" },
                        url = line,
                        logo = logo,
                        groups = groups,
                        userAgent = ua,
                        referrer = ref,
                    )
                    name = null; logo = null; groups = emptyList(); ua = null; ref = null
                }
            }
        }
        return out
    }
}

/** Busca sem diferenciar acentos nem maiúsculas ("globo" acha "GLOBO", "noticias" acha "Notícias"). */
fun String.semAcento(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)

data class LoadResult(val channels: List<Channel>, val fromCache: Boolean)

object ChannelRepository {
    /** Lista pública de canais brasileiros do projeto iptv-org (atualizada pela comunidade). */
    const val DEFAULT_URL = "https://iptv-org.github.io/iptv/countries/br.m3u"

    /** Canais que o player percorre ao trocar de canal (a lista filtrada na tela). */
    @Volatile
    var playlist: List<Channel> = emptyList()

    private fun cacheFile(context: Context, url: String) =
        File(context.filesDir, "lista_${url.hashCode()}.m3u")

    suspend fun load(context: Context, url: String): LoadResult = withContext(Dispatchers.IO) {
        try {
            val text = download(url)
            val list = M3uParser.parse(text)
            if (list.isEmpty()) error("Lista vazia")
            cacheFile(context, url).writeText(text)
            LoadResult(list, fromCache = false)
        } catch (e: Exception) {
            val cache = cacheFile(context, url)
            if (cache.exists()) LoadResult(M3uParser.parse(cache.readText()), fromCache = true)
            else throw e
        }
    }

    private fun download(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "SilverTV/1.0 (Android)")
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
