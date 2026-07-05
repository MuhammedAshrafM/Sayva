package org.moashraf.sayva.ui.screens.you

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.data.MockSayvaData
import org.moashraf.sayva.data.NotificationItem
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Primary80
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.nav.SayvaNavController

@Composable
fun NotificationsScreen(nav: SayvaNavController) {
    val grouped = MockSayvaData.notifications.groupBy { it.group }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(SurfaceContainer, CircleShape)
                    .clickable { nav.back() },
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "arrow_back", size = 18.dp, color = OnSurface)
            }
            Text(
                "Notifications",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Text(
                "Mark all",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurface,
                modifier = Modifier
                    .background(SurfaceContainer, RoundedCornerShape(100))
                    .clickable {}
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            grouped.forEach { (group, items) ->
                item {
                    Text(
                        group,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                    )
                }
                items(items) { notification ->
                    NotificationRow(notification)
                    Spacer(Modifier.height(6.dp))
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Visual-first · Every notification is also paired with a phone-flash pulse + strong haptic — no audio reliance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: NotificationItem) {
    val backgroundColor = if (notification.highlighted) notification.iconBg.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.White
    val borderColor = if (notification.highlighted) Primary80 else Outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (notification.highlighted) {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, end = 4.dp)
                    .size(6.dp)
                    .background(Primary40, CircleShape),
            )
        } else {
            Spacer(Modifier.width(10.dp))
        }
        Box(
            modifier = Modifier.size(36.dp).background(notification.iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(name = notification.icon, size = 18.dp, color = notification.iconColor, filled = true)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(notification.title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(notification.body, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            val timeText = if (notification.actionLabel != null) {
                "${notification.timeAgo} · ${notification.actionLabel}"
            } else {
                notification.timeAgo
            }
            Text(
                timeText,
                style = MaterialTheme.typography.labelSmall,
                color = if (notification.highlighted) Primary40 else OnSurfaceVariant,
            )
        }
    }
}
