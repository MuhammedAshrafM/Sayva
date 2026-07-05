package org.moashraf.sayva.ui.screens.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.ui.components.a11yButtonRow
import org.moashraf.sayva.ui.components.a11yTabRow
import org.moashraf.sayva.ui.components.a11yToggleRow
import org.moashraf.sayva.data.FavoritePhrase
import org.moashraf.sayva.designsystem.ErrorColor
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SurfaceDim
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.speech.speakText
import org.moashraf.sayva.ui.viewmodel.FavoriteFilter
import org.moashraf.sayva.ui.viewmodel.FavoritesViewModel
import org.moashraf.sayva.ui.viewmodel.SettingsViewModel

@Composable
fun FavoritesScreen(nav: SayvaNavController) {
    val favoritesViewModel: FavoritesViewModel = koinInject()
    val settingsViewModel: SettingsViewModel = koinInject()

    val selectedFilter by favoritesViewModel.filter.collectAsState()
    val allFavorites by favoritesViewModel.allFavorites.collectAsState()
    val visibleFavorites by favoritesViewModel.visibleFavorites.collectAsState()
    val settings by settingsViewModel.state.collectAsState()
    val emergencyMode = settings.emergencyMode

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(icon = "arrow_back", label = "Back", onClick = { nav.back() })
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("Favorites", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tap any phrase to speak instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Primary40, CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Add favorite phrase") { }
                        .semantics { contentDescription = "Add phrase" },
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "add", size = 18.dp, color = Color.White, contentDescription = null)
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                lazyRowItems(FavoriteFilter.entries.toList()) { filter ->
                    val selected = filter == selectedFilter
                    val label = when (filter) {
                        FavoriteFilter.All -> "All · ${allFavorites.size}"
                        else -> filter.label
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White else OnSurface,
                        modifier = Modifier
                            .background(if (selected) Primary40 else Color.White, RoundedCornerShape(100))
                            .border(if (selected) 0.dp else 1.dp, OutlineStrong, RoundedCornerShape(100))
                            .a11yTabRow(label = label, isSelected = selected) {
                                favoritesViewModel.setFilter(filter)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visibleFavorites) { phrase ->
                    FavoriteCard(phrase = phrase, onClick = { speakText(phrase.text) })
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(SurfaceDim, RoundedCornerShape(16.dp))
                            .border(1.5.dp, OutlineStrong, RoundedCornerShape(16.dp))
                            .clickable { },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White, RoundedCornerShape(14.dp))
                                .border(1.dp, OutlineStrong, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            SymbolIcon(name = "add", size = 22.dp, color = OnSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Add phrase",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDim, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).background(Color.White, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            SymbolIcon(name = "auto_awesome", size = 18.dp, color = Primary40)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AI suggests 3 more based on your week",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
                .background(OnSurface, RoundedCornerShape(18.dp))
                .a11yToggleRow(
                    label = "Emergency mode. Triple-tap power, auto-call 911",
                    checked = emergencyMode,
                    onToggle = { settingsViewModel.onEmergencyModeToggle(it) },
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Secondary50, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "accessibility_new", size = 22.dp, color = Color.White, filled = true)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Emergency mode", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(
                    "Triple-tap power · auto-call 911",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 24.dp)
                    .background(Secondary50, RoundedCornerShape(100)),
            ) {
                Box(
                    modifier = Modifier
                        .align(if (emergencyMode) Alignment.CenterEnd else Alignment.CenterStart)
                        .padding(horizontal = 2.dp)
                        .size(20.dp)
                        .background(Color.White, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun FavoriteCard(phrase: FavoritePhrase, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Brush.linearGradient(phrase.gradient), RoundedCornerShape(16.dp))
            .a11yButtonRow(label = "Speak: ${phrase.text}") { onClick() }
            .padding(14.dp),
    ) {
        SymbolIcon(
            name = phrase.icon,
            size = 60.dp,
            color = Color.White.copy(alpha = 0.18f),
            filled = true,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    phrase.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    phrase.text,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
            }
            Row(
                modifier = Modifier
                    .background(
                        if (phrase.isEmergency) ErrorColor else Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(100),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(name = "volume_up", size = 12.dp, color = Color.White, filled = true)
                Spacer(Modifier.width(4.dp))
                Text(
                    if (phrase.isEmergency) "Speak NOW" else "Speak",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CircleIconButton(icon: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(SurfaceDim, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(name = icon, size = 18.dp, color = OnSurface, contentDescription = null)
    }
}
