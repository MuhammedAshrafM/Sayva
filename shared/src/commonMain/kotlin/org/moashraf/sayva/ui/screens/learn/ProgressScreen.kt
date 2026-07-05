package org.moashraf.sayva.ui.screens.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.data.Badge
import org.moashraf.sayva.data.MockSayvaData
import org.moashraf.sayva.ui.viewmodel.LearnViewModel
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SurfaceDim
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.designsystem.WarningColor
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.components.SayvaTopBar

@Composable
fun ProgressScreen(nav: SayvaNavController) {
    val viewModel: LearnViewModel = koinInject()
    val stats by viewModel.progressStats.collectAsState()
    // Badges still ship from MockSayvaData — a real badges catalog + earned/
    // unlocked flags lands with the gamification backend in Phase 3.
    val badges = MockSayvaData.badges

    Column(modifier = Modifier.fillMaxSize()) {
        SayvaTopBar(
            onBack = { nav.back() },
            title = "Your progress",
            trailing = {
                Box(
                    modifier = Modifier.size(38.dp).background(SurfaceDim, CircleShape).clickable { },
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "share", size = 18.dp, color = Color(0xFF1A1B25))
                }
            },
        )

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(WarningColor, Secondary50)), RoundedCornerShape(24.dp))
                .padding(18.dp),
        ) {
            SymbolIcon(
                name = "whatshot",
                size = 160.dp,
                color = Color.White.copy(alpha = 0.1f),
                filled = true,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${stats.streakDays}",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "DAY STREAK 🔥",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    Text(
                        "Personal best · ${stats.personalBestStreak} days",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard("${stats.totalXp}", "XP", Primary40, Modifier.weight(1f))
            StatCard("${stats.signsLearned}", "Signs", Tertiary50, Modifier.weight(1f))
            StatCard("${stats.lessonsCompleted}", "Lessons", Secondary50, Modifier.weight(1f))
            StatCard("${stats.badgesEarned}", "Badges", WarningColor, Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(18.dp))
                .border(1.dp, Outline, RoundedCornerShape(18.dp))
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("This week", style = MaterialTheme.typography.titleSmall)
                Text("+12% vs last", style = MaterialTheme.typography.labelMedium, color = Primary40)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                val maxIndex = stats.weeklyHeights.indices.maxByOrNull { stats.weeklyHeights[it] } ?: -1
                stats.weeklyHeights.forEachIndexed { i, fraction ->
                    val label = stats.weeklyLabels.getOrNull(i) ?: ""
                    val isToday = i == stats.weeklyHeights.lastIndex
                    val barColor = if (isToday) WarningColor else if (i == maxIndex) Primary40 else Primary40.copy(alpha = 0.18f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .fillMaxHeight(fraction.coerceIn(0f, 1f))
                                .background(barColor, RoundedCornerShape(6.dp)),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) WarningColor else OnSurfaceVariant,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Badges", style = MaterialTheme.typography.titleSmall)
            Text("See all ${badges.size + 14}", style = MaterialTheme.typography.labelMedium, color = Primary40)
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(badges) { badge ->
                BadgeCard(badge)
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
    }
}

@Composable
private fun BadgeCard(badge: Badge) {
    Box(
        modifier = Modifier
            .width(78.dp)
            .background(badge.bg, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = if (badge.locked) Modifier.alpha(0.5f) else Modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SymbolIcon(
                name = badge.icon,
                size = 28.dp,
                color = badge.iconColor,
                filled = !badge.locked,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                badge.name,
                style = MaterialTheme.typography.labelSmall,
                color = badge.iconColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
