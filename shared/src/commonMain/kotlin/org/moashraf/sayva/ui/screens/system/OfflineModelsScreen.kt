package org.moashraf.sayva.ui.screens.system

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.data.MockSayvaData
import org.moashraf.sayva.data.OfflineLanguagePack
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OnTertiaryContainer
import org.moashraf.sayva.designsystem.OnWarningContainer
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.TertiaryContainer
import org.moashraf.sayva.designsystem.WarningContainer
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.components.Pill
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.Tertiary50

@Composable
fun OfflineModelsScreen(nav: SayvaNavController) {
    var downloadedIds by remember {
        mutableStateOf(MockSayvaData.offlinePacks.filter { it.isDownloaded }.map { it.id }.toSet())
    }

    val packs = MockSayvaData.offlinePacks
    val downloaded = packs.filter { downloadedIds.contains(it.id) }
    val available = packs.filter { !downloadedIds.contains(it.id) }

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
                "Offline models",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(96.dp).background(SurfaceContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier.size(70.dp).background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("418", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                                Text("MB USED", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StorageLegendRow(color = Primary40, label = "ASL model", value = "182 MB")
                        StorageLegendRow(color = Secondary50, label = "BSL model", value = "108 MB")
                        StorageLegendRow(color = Tertiary50, label = "Cache · history", value = "72 MB")
                        StorageLegendRow(color = Outline, label = "Free", value = "56 MB", labelColor = OnSurfaceVariant)
                    }
                }
            }

            item {
                Text(
                    "DOWNLOADED · ${downloaded.size} OF 14",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(downloaded, key = { it.id }) { pack ->
                DownloadedPackRow(pack = pack, onRemove = { downloadedIds = downloadedIds - pack.id })
                Spacer(Modifier.height(6.dp))
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "AVAILABLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(available, key = { it.id }) { pack ->
                AvailablePackRow(pack = pack, onDownload = { downloadedIds = downloadedIds + pack.id })
                Spacer(Modifier.height(6.dp))
            }

            item {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OnSurface, RoundedCornerShape(100))
                        .clickable {}
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SymbolIcon(name = "cleaning_services", size = 18.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear cache · 72 MB", style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "14 languages roadmap · ASL, BSL, LSF, DGS, LIS, LSE, JSL, KSL, Auslan, NZSL, Libras, ISL, RSL, CSL — packs downloaded on-demand to keep base install <200 MB.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StorageLegendRow(color: Color, label: String, value: String, labelColor: Color = OnSurface) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = labelColor, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = OnSurface)
    }
}

@Composable
private fun DownloadedPackRow(pack: OfflineLanguagePack, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(PrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(pack.flag, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(pack.name, style = MaterialTheme.typography.titleSmall)
            Text(pack.detail, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        if (pack.isActive) {
            Pill(text = "ACTIVE", backgroundColor = TertiaryContainer, contentColor = OnTertiaryContainer)
        } else {
            Box(modifier = Modifier.size(36.dp).clickable { onRemove() }, contentAlignment = Alignment.Center) {
                SymbolIcon(name = "delete_outline", size = 20.dp, color = OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AvailablePackRow(pack: OfflineLanguagePack, onDownload: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(SurfaceContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(pack.flag, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(pack.name, style = MaterialTheme.typography.titleSmall)
            Text(pack.detail, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        if (pack.isBeta) {
            Pill(text = "BETA", backgroundColor = WarningContainer, contentColor = OnWarningContainer)
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Primary40, CircleShape)
                    .clickable { onDownload() },
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "download", size = 18.dp, color = Color.White)
            }
        }
    }
}
