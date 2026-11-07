package com.silver.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

object Silver {
    val Bg = Color(0xFF121316)
    val Card = Color(0xFF22252A)
    val Dim = Color(0xFF7D828A)
    val Mid = Color(0xFFC9CDD2)
    val Light = Color(0xFFEDEFF2)
    val Gold = Color(0xFFE6C766)
}

@Composable
fun SilverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Silver.Mid,
            onPrimary = Silver.Bg,
            background = Silver.Bg,
            surface = Silver.Card,
            onSurface = Silver.Light,
            onBackground = Silver.Light,
        ),
        content = content,
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SilverTheme {
                ChannelListScreen(onOpen = { list, index ->
                    ChannelRepository.playlist = list
                    startActivity(
                        Intent(this, PlayerActivity::class.java).putExtra("index", index)
                    )
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(
    vm: ChannelViewModel = viewModel(),
    onOpen: (List<Channel>, Int) -> Unit,
) {
    var showSettings by remember { mutableStateOf(false) }
    val categories = remember(vm.all) { vm.categories() }
    val visible = remember(vm.all, vm.query, vm.filter, vm.favorites) { vm.visible() }

    Scaffold(
        containerColor = Silver.Bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SILVERTV",
                        color = Silver.Light,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                    )
                },
                actions = {
                    IconButton(onClick = { vm.reload() }) {
                        Icon(Icons.Filled.Refresh, "Atualizar lista", tint = Silver.Mid)
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, "Lista de canais", tint = Silver.Mid)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Silver.Bg),
            )
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                placeholder = { Text("Buscar canal") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Silver.Mid,
                    unfocusedBorderColor = Silver.Card,
                    focusedContainerColor = Silver.Card,
                    unfocusedContainerColor = Silver.Card,
                    focusedTextColor = Silver.Light,
                    unfocusedTextColor = Silver.Light,
                    cursorColor = Silver.Light,
                    focusedLeadingIconColor = Silver.Mid,
                    unfocusedLeadingIconColor = Silver.Dim,
                    focusedPlaceholderColor = Silver.Dim,
                    unfocusedPlaceholderColor = Silver.Dim,
                ),
            )

            val chips = listOf(FILTER_ALL, FILTER_FAV) + categories
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(chips) { f ->
                    val label = when (f) {
                        FILTER_ALL -> "Todos (${vm.all.size})"
                        FILTER_FAV -> "★ Favoritos"
                        else -> f
                    }
                    FilterChip(
                        selected = vm.filter == f,
                        onClick = { vm.selectFilter(f) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Silver.Card,
                            labelColor = Silver.Mid,
                            selectedContainerColor = Silver.Mid,
                            selectedLabelColor = Silver.Bg,
                        ),
                    )
                }
            }

            if (vm.loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Silver.Mid,
                    trackColor = Silver.Card,
                )
            }
            vm.message?.let {
                Text(
                    it,
                    color = Silver.Gold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            if (!vm.loading && visible.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        when {
                            vm.all.isEmpty() -> "Nenhum canal carregado."
                            vm.filter == FILTER_FAV && vm.query.isBlank() ->
                                "Nenhum favorito ainda.\nToque na ☆ ao lado de um canal."
                            else -> "Nenhum canal encontrado."
                        },
                        color = Silver.Dim,
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(visible) { i, c ->
                        ChannelRow(
                            channel = c,
                            isFavorite = c.url in vm.favorites,
                            onClick = { onOpen(visible, i) },
                            onFavorite = { vm.toggleFavorite(c) },
                        )
                        HorizontalDivider(color = Silver.Card, thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    if (showSettings) {
        ListUrlDialog(
            current = vm.listUrl,
            onDismiss = { showSettings = false },
            onSave = { vm.changeListUrl(it); showSettings = false },
        )
    }
}

@Composable
fun ChannelRow(
    channel: Channel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 64.dp, height = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Silver.Card),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                channel.name.take(1).uppercase(),
                color = Silver.Dim,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            if (channel.logo != null) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                channel.name,
                color = Silver.Light,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                channel.groups.joinToString(" · "),
                color = Silver.Dim,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onFavorite) {
            Icon(
                if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                tint = if (isFavorite) Silver.Gold else Silver.Dim,
            )
        }
    }
}

@Composable
fun ListUrlDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Silver.Card,
        title = { Text("Lista de canais", color = Silver.Light) },
        text = {
            Column {
                Text(
                    "Endereço da lista M3U. O padrão é a lista pública de canais brasileiros do iptv-org.",
                    color = Silver.Mid,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { text = ChannelRepository.DEFAULT_URL }) {
                    Text("Restaurar padrão", color = Silver.Mid)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text("Salvar", color = Silver.Light) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Silver.Dim) }
        },
    )
}
