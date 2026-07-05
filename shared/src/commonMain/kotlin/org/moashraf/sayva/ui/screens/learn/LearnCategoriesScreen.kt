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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.data.LearnCategory
import org.moashraf.sayva.ui.components.a11yButtonRow
import org.moashraf.sayva.ui.viewmodel.LearnViewModel
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OnWarningContainer
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.designsystem.WarningColor
import org.moashraf.sayva.designsystem.WarningContainer
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController

@Composable
fun LearnCategoriesScreen(nav: SayvaNavController) {
    val viewModel: LearnViewModel = koinInject()
    val categories by viewModel.categories.collectAsState()
    val progress by viewModel.progressStats.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Learn",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .background(WarningContainer, RoundedCornerShape(100))
                    .border(1.dp, Color(0xFFFBE5B8), RoundedCornerShape(100))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                org.moashraf.sayva.designsystem.SymbolIcon(name = "whatshot", size = 16.dp, color = WarningColor, filled = true)
                Spacer(Modifier.width(4.dp))
                Text(
                    "${progress.streakDays}d",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnWarningContainer,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(Brush.linearGradient(listOf(Tertiary50, Color(0xFF005544))), RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    org.moashraf.sayva.designsystem.SymbolIcon(name = "workspace_premium", size = 28.dp, color = Color.White, filled = true)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "DAILY CHALLENGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    Text("3 new signs · 60s", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(
                        "+50 XP · keep your streak alive",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Categories", style = MaterialTheme.typography.titleSmall)
            Text("See all", style = MaterialTheme.typography.labelMedium, color = Primary40)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryCard(
                    category = category,
                    onClick = { nav.navigate(Screen.LessonPlayer("l-hello")) },
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(category: LearnCategory, onClick: () -> Unit) {
    val fraction = if (category.total > 0) category.learned.toFloat() / category.total.toFloat() else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .a11yButtonRow(
                label = "${category.name}, ${category.learned} of ${category.total} signs",
            ) { onClick() }
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Box(
                modifier = Modifier.size(36.dp).background(category.iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                org.moashraf.sayva.designsystem.SymbolIcon(name = category.icon, size = 18.dp, color = category.iconColor, filled = true)
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleSmall)
                val label = if (category.learned >= category.total) {
                    "${category.learned} / ${category.total} ✓"
                } else {
                    "${category.learned} / ${category.total} signs"
                }
                Text(label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Outline, RoundedCornerShape(2.dp))) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(category.barColor, RoundedCornerShape(2.dp)),
            )
        }
    }
}
