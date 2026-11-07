package com.silver.tv

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

const val FILTER_ALL = "__todos__"
const val FILTER_FAV = "__favoritos__"

class ChannelViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("silvertv", Context.MODE_PRIVATE)

    var all by mutableStateOf<List<Channel>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var query by mutableStateOf("")
    var filter by mutableStateOf(prefs.getString("filtro", FILTER_ALL) ?: FILTER_ALL)
        private set
    var favorites by mutableStateOf(prefs.getStringSet("favs", emptySet())?.toSet() ?: emptySet())
        private set
    var listUrl by mutableStateOf(prefs.getString("url", ChannelRepository.DEFAULT_URL) ?: ChannelRepository.DEFAULT_URL)
        private set

    init {
        reload()
    }

    fun reload() {
        if (loading) return
        viewModelScope.launch {
            loading = true
            message = null
            try {
                val r = ChannelRepository.load(getApplication(), listUrl)
                all = r.channels
                if (r.fromCache) message = "Sem conexão com a lista — mostrando a última versão salva."
            } catch (e: Exception) {
                message = "Não foi possível carregar a lista de canais. Verifique a internet e toque em ⟳."
            }
            loading = false
        }
    }

    fun selectFilter(f: String) {
        filter = f
        prefs.edit().putString("filtro", f).apply()
    }

    fun toggleFavorite(c: Channel) {
        favorites = if (c.url in favorites) favorites - c.url else favorites + c.url
        prefs.edit().putStringSet("favs", favorites).apply()
    }

    fun changeListUrl(url: String) {
        listUrl = url.trim().ifBlank { ChannelRepository.DEFAULT_URL }
        prefs.edit().putString("url", listUrl).apply()
        reload()
    }

    /** Categorias ordenadas pela quantidade de canais. */
    fun categories(): List<String> =
        all.flatMap { it.groups }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

    fun visible(): List<Channel> {
        val q = query.trim().semAcento()
        return all.filter { c ->
            val passaFiltro = when (filter) {
                FILTER_ALL -> true
                FILTER_FAV -> c.url in favorites
                else -> filter in c.groups
            }
            passaFiltro && (q.isEmpty() || c.name.semAcento().contains(q))
        }
    }
}
