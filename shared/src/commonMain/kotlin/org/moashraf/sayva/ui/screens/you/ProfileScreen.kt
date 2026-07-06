package org.moashraf.sayva.ui.screens.you

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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.ui.components.a11yButtonRow
import org.moashraf.sayva.designsystem.ErrorColor
import org.moashraf.sayva.designsystem.ErrorContainer
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OnTertiaryContainer
import org.moashraf.sayva.designsystem.OnWarningContainer
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SecondaryContainer
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.designsystem.TertiaryContainer
import org.moashraf.sayva.designsystem.WarningContainer
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.designsystem.WarningColor
import org.moashraf.sayva.ui.viewmodel.LearnViewModel
import org.moashraf.sayva.ui.viewmodel.ProfileViewModel

private data class ProfileMenuRow(
    val title: String,
    val subtitle: String?,
    val icon: String,
    val iconColor: Color,
    val iconBg: Color,
    val target: Screen,
    val badgeText: String? = null,
    val badgeColor: Color = OnWarningContainer,
    val badgeBg: Color = WarningContainer,
)

private val menuRows = listOf(
    ProfileMenuRow(
        title = "Translation history",
        subtitle = null,
        icon = "history",
        iconColor = Primary40,
        iconBg = PrimaryContainer,
        target = Screen.History,
    ),
    ProfileMenuRow(
        title = "Offline models",
        subtitle = "ASL · BSL · 2 packs · 240 MB",
        icon = "cloud_download",
        iconColor = Tertiary50,
        iconBg = TertiaryContainer,
        target = Screen.OfflineModels,
    ),
    ProfileMenuRow(
        title = "Contribute to AI",
        subtitle = "Help improve recognition · earn badges",
        icon = "volunteer_activism",
        iconColor = Secondary50,
        iconBg = SecondaryContainer,
        target = Screen.Contribute,
        badgeText = "NEW",
        badgeColor = Color.White,
        badgeBg = Tertiary50,
    ),
    ProfileMenuRow(
        title = "Family sharing",
        subtitle = null,
        icon = "family_restroom",
        iconColor = OnTertiaryContainer,
        iconBg = TertiaryContainer,
        target = Screen.Family,
    ),
    ProfileMenuRow(
        title = "Accessibility",
        subtitle = null,
        icon = "accessibility_new",
        iconColor = Primary40,
        iconBg = PrimaryContainer,
        target = Screen.Accessibility,
    ),
    ProfileMenuRow(
        title = "Notifications",
        subtitle = null,
        icon = "notifications",
        iconColor = OnSurfaceVariant,
        iconBg = SurfaceContainer,
        target = Screen.Notifications,
    ),
    ProfileMenuRow(
        title = "Upgrade to Plus",
        subtitle = "Unlock voice cloning · all languages",
        icon = "workspace_premium",
        iconColor = OnWarningContainer,
        iconBg = WarningContainer,
        target = Screen.Paywall,
    ),
    ProfileMenuRow(
        title = "Settings",
        subtitle = null,
        icon = "settings",
        iconColor = OnSurfaceVariant,
        iconBg = SurfaceContainer,
        target = Screen.Settings,
    ),
    ProfileMenuRow(
        title = "Help & feedback",
        subtitle = null,
        icon = "help",
        iconColor = ErrorColor,
        iconBg = ErrorContainer,
        target = Screen.CrashReport,
    ),
)

@Composable
fun ProfileScreen(nav: SayvaNavController) {
    val viewModel: ProfileViewModel = koinInject()
    val learnViewModel: LearnViewModel = koinInject()
    val user by viewModel.currentUser.collectAsState()
    val stats by learnViewModel.progressStats.collectAsState()

    // Prefer a real display name; fall back to email prefix; then "Guest".
    val displayName = user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        ?: "Guest"
    val emailLine = user?.email ?: if (user?.isAnonymous == true) "Anonymous session" else "Not signed in"
    val avatarInitial = displayName.firstOrNull()?.uppercase()?.toString() ?: "?"

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("You", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
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
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Brush.linearGradient(listOf(Primary40, Secondary50)), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(avatarInitial, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(Tertiary50, CircleShape)
                                .border(3.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            SymbolIcon(name = "edit", size = 14.dp, color = Color.White, filled = true)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayName, style = MaterialTheme.typography.titleMedium)
                        Text(emailLine, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .background(WarningContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SymbolIcon(name = "workspace_premium", size = 12.dp, color = OnWarningContainer, filled = true)
                            Spacer(Modifier.width(4.dp))
                            Text("Sayva Plus", style = MaterialTheme.typography.labelSmall, color = OnWarningContainer)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ProfileStatTile(
                        value = "${stats.streakDays}",
                        label = "🔥 Streak",
                        valueColor = WarningColor,
                        modifier = Modifier.weight(1f),
                    )
                    ProfileStatTile(
                        value = "${stats.totalXp}",
                        label = "Translations",
                        valueColor = Primary40,
                        modifier = Modifier.weight(1f),
                    )
                    ProfileStatTile(
                        value = "${stats.signsLearned}",
                        label = "Signs known",
                        valueColor = Tertiary50,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            items(menuRows, key = { it.title }) { row ->
                ProfileMenuRowItem(row = row, onClick = { nav.navigate(row.target) })
                Spacer(Modifier.height(4.dp))
            }

            item {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Sayva 2.4.1 · AI model 2026.06 · ",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                    )
                    Text(
                        "Sign out",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorColor,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Sign out") {
                            viewModel.signOut()
                            nav.replaceAll(Screen.Welcome)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatTile(value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
    }
}

@Composable
private fun ProfileMenuRowItem(row: ProfileMenuRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .a11yButtonRow(label = "${row.title}${row.subtitle?.let { ", $it" } ?: ""}") { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(row.iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(name = row.icon, size = 18.dp, color = row.iconColor, filled = true)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.titleSmall)
            row.subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
        if (row.badgeText != null) {
            Text(
                row.badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = row.badgeColor,
                modifier = Modifier
                    .background(row.badgeBg, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        } else {
            SymbolIcon(name = "chevron_right", size = 18.dp, color = OnSurfaceVariant)
        }
    }
}
