package org.moashraf.sayva.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.ui.components.a11yButtonRow
import org.moashraf.sayva.designsystem.OnPrimaryContainer
import org.moashraf.sayva.designsystem.OnSecondaryContainer
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OnTertiaryContainer
import org.moashraf.sayva.designsystem.OnWarningContainer
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SecondaryContainer
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.designsystem.TertiaryContainer
import org.moashraf.sayva.designsystem.WarningColor
import org.moashraf.sayva.designsystem.WarningContainer
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController

private data class QuickAction(val label: String, val icon: String, val bg: Color, val iconBg: Color, val fg: Color, val target: Screen)

private val quickActions = listOf(
    QuickAction("Conversation", "forum", SecondaryContainer, Secondary50, OnSecondaryContainer, Screen.Conversation),
    QuickAction("History", "history", PrimaryContainer, Primary40, OnPrimaryContainer, Screen.History),
    QuickAction("Favorites", "star", WarningContainer, WarningColor, OnWarningContainer, Screen.Favorites),
    QuickAction("Learn", "school", TertiaryContainer, Tertiary50, OnTertiaryContainer, Screen.LearnCategories),
    QuickAction("Saved chats", "bookmark", SurfaceContainer, OnSurface, OnSurface, Screen.SavedConversations),
    QuickAction("Settings", "settings", SurfaceContainer, OnSurfaceVariant, OnSurface, Screen.Settings),
)

@Composable
fun HomeScreen(nav: SayvaNavController) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Brush.linearGradient(listOf(Primary40, Secondary50)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("J", color = Color.White, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Hi, Jordan 👋", style = MaterialTheme.typography.titleMedium)
                Text("Ready to talk?", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceContainer, CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Open notifications") {
                        nav.navigate(Screen.Notifications)
                    }
                    .semantics { contentDescription = "Notifications" },
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "notifications", size = 20.dp, color = OnSurface, contentDescription = null)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(8.dp)
                        .background(Secondary50, CircleShape),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .background(SurfaceContainer, RoundedCornerShape(100))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(name = "search", size = 18.dp, color = OnSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Text(
                "Search a sign or phrase…",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            SymbolIcon(name = "mic", size = 18.dp, color = Primary40)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .background(
                    Brush.linearGradient(listOf(Primary40, Color(0xFF8A8EF5), Secondary50)),
                    RoundedCornerShape(24.dp),
                )
                .padding(18.dp),
        ) {
            Text(
                "ONLINE · ASL",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(100))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Column {
                Text("QUICK START", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(4.dp))
                Text("Translate now", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Point the camera and we'll speak for you in real time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.width(200.dp),
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(100))
                        .a11yButtonRow(label = "Open camera") { nav.navigate(Screen.LiveCamera) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SymbolIcon(name = "videocam", size = 18.dp, color = Primary40, filled = true, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Open camera", style = MaterialTheme.typography.labelLarge, color = Primary40)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().height(180.dp).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(quickActions) { action ->
                Column(
                    modifier = Modifier
                        .background(action.bg, RoundedCornerShape(16.dp))
                        .a11yButtonRow(label = action.label) { nav.navigate(action.target) }
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(action.iconBg, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        SymbolIcon(name = action.icon, size = 18.dp, color = Color.White, filled = true, contentDescription = null)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        action.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = action.fg,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(18.dp))
                .border(1.dp, Outline, RoundedCornerShape(18.dp))
                .a11yButtonRow(label = "Daily challenge, 2 of 3 signs, 60 seconds left, +50 XP") {
                    nav.navigate(Screen.Practice("l-hello"))
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(52.dp).background(Tertiary50.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(42.dp).background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SymbolIcon(name = "whatshot", size = 16.dp, color = Tertiary50, filled = true)
                        Text("12d", style = MaterialTheme.typography.labelSmall, color = Tertiary50)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily challenge", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "+50 XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnTertiaryContainer,
                        modifier = Modifier.background(TertiaryContainer, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text("2 of 3 signs · 60 s left", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(OutlineStrong, RoundedCornerShape(2.dp))) {
                    Box(modifier = Modifier.fillMaxWidth(0.66f).height(4.dp).background(Tertiary50, RoundedCornerShape(2.dp)))
                }
            }
            SymbolIcon(name = "chevron_right", size = 20.dp, color = OnSurfaceVariant)
        }

        Spacer(Modifier.height(12.dp))
    }
}
