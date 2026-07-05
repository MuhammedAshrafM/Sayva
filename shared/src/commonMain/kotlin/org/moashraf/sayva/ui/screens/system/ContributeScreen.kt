package org.moashraf.sayva.ui.screens.system

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SuccessColor
import org.moashraf.sayva.designsystem.SuccessContainer
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.WarningColor
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.designsystem.OnTertiaryContainer

@Composable
fun ContributeScreen(nav: SayvaNavController) {
    var contributingEnabled by remember { mutableStateOf(true) }
    var uploadProgress by remember { mutableStateOf(78) }

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
                "Help improve Sayva",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Box(
                modifier = Modifier.size(38.dp).background(SurfaceContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "info", size = 18.dp, color = OnSurface)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .background(Brush.linearGradient(listOf(Secondary50, WarningColor)), RoundedCornerShape(24.dp))
                        .padding(18.dp),
                ) {
                    SymbolIcon(
                        name = "diversity_3",
                        size = 120.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        filled = true,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                    Column {
                        Text(
                            "YOUR IMPACT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("38 clips · 2 badges", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Anonymous recordings teach the AI new dialects and hand variations — helping millions of deaf people communicate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.width(220.dp),
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Share my sign samples", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Anonymous · face blurred · withdraw anytime",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = contributingEnabled,
                        onCheckedChange = { contributingEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Secondary50),
                    )
                }
            }

            item {
                Text(
                    "TODAY'S MISSION",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .border(1.dp, Outline, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                        Box(
                            modifier = Modifier.size(48.dp).background(
                                androidx.compose.ui.graphics.Color(0xFFFFE2DE),
                                RoundedCornerShape(14.dp),
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            SymbolIcon(name = "record_voice_over", size = 24.dp, color = Secondary50, filled = true)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Record 5 greetings", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "3 of 5 done · ~30s left",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                            )
                        }
                        Text("+30 XP", style = MaterialTheme.typography.titleSmall, color = Secondary50)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(bottom = 12.dp)
                            .background(Outline, RoundedCornerShape(3.dp)),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(6.dp).background(Secondary50, RoundedCornerShape(3.dp)))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Secondary50, RoundedCornerShape(100))
                            .clickable {}
                            .padding(vertical = 12.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SymbolIcon(name = "videocam", size = 16.dp, color = Color.White, filled = true)
                        Spacer(Modifier.width(6.dp))
                        Text("Record next", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .background(SuccessContainer, RoundedCornerShape(14.dp))
                        .border(1.dp, SuccessColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    SymbolIcon(name = "verified_user", size = 22.dp, color = OnTertiaryContainer, filled = true)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Anonymous & consensual",
                            style = MaterialTheme.typography.titleSmall,
                            color = OnTertiaryContainer,
                        )
                        Text(
                            "Face blurred · no audio · withdraw anytime · independently audited.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnTertiaryContainer,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, Outline, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        SymbolIcon(name = "cloud_upload", size = 18.dp, color = SuccessColor)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Uploading 3 clips · $uploadProgress%", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Encrypted · 0.4 MB · Wi-Fi only",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Reward loop · Per-contribution XP · monthly leaderboards of top contributors · \"Founding contributor\" lifetime badge for first 10k · early access to new languages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
