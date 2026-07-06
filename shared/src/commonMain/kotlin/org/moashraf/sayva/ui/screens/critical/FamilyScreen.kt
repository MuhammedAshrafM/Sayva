package org.moashraf.sayva.ui.screens.critical

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.data.FamilyMember
import org.moashraf.sayva.data.MockSayvaData
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.components.SayvaTopBar

@Composable
fun FamilyScreen(nav: SayvaNavController) {
    val members = MockSayvaData.familyMembers

    Column(modifier = Modifier.fillMaxSize()) {
        SayvaTopBar(
            onBack = { nav.back() },
            title = "Sayva Family",
            trailing = {
                Box(
                    modifier = Modifier.size(38.dp).background(SurfaceContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "settings", size = 18.dp, color = OnSurface)
                }
            },
        )

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 14.dp)
                        .background(
                            Brush.linearGradient(listOf(Secondary50, Color(0xFFE69500))),
                            RoundedCornerShape(24.dp),
                        )
                        .padding(18.dp),
                ) {
                    SymbolIcon(
                        name = "family_restroom",
                        size = 100.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        filled = true,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                    Column {
                        Text(
                            "THE HALE FAMILY",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${members.size} of 6 members",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                        Text(
                            "Shared streak · 28 days 🔥",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            items(members, key = { it.id }) { member ->
                FamilyMemberRow(member = member)
                Spacer(Modifier.height(6.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 6.dp, bottom = 14.dp)
                        .background(SurfaceContainer, RoundedCornerShape(14.dp))
                        .border(1.5.dp, OutlineStrong, RoundedCornerShape(14.dp))
                        .clickable { /* mock invite flow - no real invite sent */ }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(Primary40, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        SymbolIcon(name = "person_add", size = 22.dp, color = Color.White, filled = true)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Invite ${6 - members.size} more", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Share link · QR · or email",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                        )
                    }
                    SymbolIcon(name = "chevron_right", size = 20.dp, color = Primary40)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .background(OnSurface, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(name = "shield", size = 22.dp, color = Color(0xFFFF9A8F), filled = true)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Parental controls", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(
                    "Content filter active for Ella",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(24.dp)
                    .background(Secondary50, RoundedCornerShape(100)),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp)
                        .size(20.dp)
                        .background(Color.White, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun FamilyMemberRow(member: FamilyMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Brush.linearGradient(member.gradient), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(member.initial, color = Color.White, style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(member.name, style = MaterialTheme.typography.titleSmall)
            Text(member.detail, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        }
        if (member.badge != null) {
            Text(
                member.badge,
                style = MaterialTheme.typography.labelSmall,
                color = member.badgeColor,
                modifier = Modifier
                    .background(member.badgeBg, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        } else {
            SymbolIcon(name = "more_vert", size = 18.dp, color = OnSurfaceVariant)
        }
    }
}
