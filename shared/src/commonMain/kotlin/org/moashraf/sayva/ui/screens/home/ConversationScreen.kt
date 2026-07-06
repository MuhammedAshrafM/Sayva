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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.designsystem.ErrorColor
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SecondaryContainer
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.speech.speakText
import org.moashraf.sayva.ui.components.a11yButtonRow
import org.moashraf.sayva.ui.viewmodel.ConversationsViewModel

private data class TranscriptLine(
    val text: String,
    val isSign: Boolean,
    val time: String,
)

private val transcript = listOf(
    TranscriptLine("Hello, my name is Jordan.", isSign = true, time = "9:41"),
    TranscriptLine("Hi Jordan, I'm Dr. Lee. What brings you in today?", isSign = false, time = "9:42"),
    TranscriptLine("My ear hurts since Monday.", isSign = true, time = "9:43"),
    TranscriptLine("Which side, and is there fluid?", isSign = false, time = "9:43"),
)

@Composable
fun ConversationScreen(nav: SayvaNavController) {
    val viewModel: ConversationsViewModel = koinInject()

    // The transcript is still a hardcoded fixture — the AI pipeline (Phase 2)
    // will populate it in real time. The metadata below is derived from that
    // fixture so "Stop & save" persists something coherent to the SavedConversations
    // list; a later ticket adds per-message persistence and wires the partner
    // name from the pipeline's speaker detection.
    val partnerName = "Dr. Lee"
    val partnerInitial = "L"
    val partnerColor = Tertiary50
    val category = "Medical"
    val duration = "02:14"
    val title = "Conversation · $partnerName"
    val preview = "\"${transcript.last().text}\""

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App bar.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SurfaceContainer, CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Go back") { nav.back() }
                        .semantics { contentDescription = "Back" },
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "arrow_back", size = 18.dp, color = OnSurface, contentDescription = null)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Conversation", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(6.dp))
                        Row(
                            modifier = Modifier
                                .background(ErrorColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.size(5.dp).background(Color.White, CircleShape))
                            Spacer(Modifier.width(3.dp))
                            Text("REC", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                    Text("02:14 · Jordan ↔ Dr. Lee", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SurfaceContainer, CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Adjust conversation settings") { }
                        .semantics { contentDescription = "Settings" },
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "tune", size = 18.dp, color = OnSurface, contentDescription = null)
                }
            }

            // Date divider.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f), color = Outline)
                Spacer(Modifier.width(8.dp))
                Text("TODAY · 9:41 AM", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f), color = Outline)
            }

            // Transcript. Marked as a polite live region so screen readers
            // announce new lines as they arrive without stealing focus.
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    transcript,
                    // Static demo transcript has no domain id, so key on the
                    // composite of time + text. If this ever becomes a live
                    // dynamic list, replace with a real message id.
                    key = { line -> "${line.time}␟${line.text}" },
                ) { line -> TranscriptBubble(line, onSpeak = { speakText(line.text) }) }
                item { TypingBubble() }
            }
        }

        // Floating camera mini-preview.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 116.dp)
                .size(width = 82.dp, height = 108.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF3A3550), Color(0xFF1A1B25))), RoundedCornerShape(14.dp))
                .border(2.dp, Color.White, RoundedCornerShape(14.dp)),
        ) {
            SymbolIcon(
                name = "sign_language",
                size = 36.dp,
                color = Color.White.copy(alpha = 0.4f),
                filled = true,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                "LIVE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(ErrorColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        // Bottom toolbar.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 24.dp)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(1.dp, Outline, RoundedCornerShape(24.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarCircleButton(icon = "file_download", bg = SurfaceContainer, iconColor = OnSurface, onClick = {})
            ToolbarCircleButton(icon = "share", bg = SurfaceContainer, iconColor = OnSurface, onClick = {})
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(OnSurface, RoundedCornerShape(100))
                    .a11yButtonRow(label = "Stop and save conversation") {
                        viewModel.saveConversation(
                            title = title,
                            preview = preview,
                            partnerInitial = partnerInitial,
                            durationLabel = "$duration · ${transcript.size} messages",
                            messageCount = transcript.size,
                            category = category,
                            partnerColor = partnerColor,
                        )
                        nav.navigate(Screen.SavedConversations)
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(name = "stop_circle", size = 16.dp, color = Color.White, filled = true)
                Spacer(Modifier.width(6.dp))
                Text("Stop & save", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
            ToolbarCircleButton(icon = "delete_sweep", bg = SecondaryContainer, iconColor = ErrorColor, onClick = { nav.back() })
        }
    }
}

@Composable
private fun TranscriptBubble(line: TranscriptLine, onSpeak: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (line.isSign) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (line.isSign) {
            AvatarDot(icon = "sign_language", bg = Primary40)
            Spacer(Modifier.width(6.dp))
        }
        Column(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .background(
                    if (line.isSign) Primary40 else SurfaceContainer,
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (line.isSign) 4.dp else 18.dp,
                        bottomEnd = if (line.isSign) 18.dp else 4.dp,
                    ),
                )
                .clickable { onSpeak() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                line.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (line.isSign) Color.White else OnSurface,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SymbolIcon(
                    name = if (line.isSign) "graphic_eq" else "mic",
                    size = 11.dp,
                    color = if (line.isSign) Color.White.copy(alpha = 0.7f) else OnSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    (if (line.isSign) "SIGNED · " else "VOICE · ") + line.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (line.isSign) Color.White.copy(alpha = 0.7f) else OnSurfaceVariant,
                )
            }
        }
        if (!line.isSign) {
            Spacer(Modifier.width(6.dp))
            AvatarDot(icon = "person", bg = Secondary50)
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(verticalAlignment = Alignment.Bottom) {
        AvatarDot(icon = "sign_language", bg = Primary40)
        Spacer(Modifier.width(6.dp))
        Row(
            modifier = Modifier
                .background(Primary40.copy(alpha = 0.12f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp))
                .border(1.dp, Color(0xFF8A8EF5), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { i ->
                Box(modifier = Modifier.size(5.dp).background(Primary40, CircleShape))
                if (i != 2) Spacer(Modifier.width(3.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text("Signing…", style = MaterialTheme.typography.bodyMedium, color = Primary40)
        }
    }
}

@Composable
private fun AvatarDot(icon: String, bg: Color) {
    Box(
        modifier = Modifier.size(24.dp).background(bg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(name = icon, size = 12.dp, color = Color.White, filled = true)
    }
}

@Composable
private fun ToolbarCircleButton(
    icon: String,
    bg: Color,
    iconColor: Color,
    onClick: () -> Unit,
    label: String = when (icon) {
        "file_download" -> "Download transcript"
        "share" -> "Share conversation"
        "delete_sweep" -> "Delete conversation"
        else -> icon.replace('_', ' ')
    },
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(bg, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(name = icon, size = 20.dp, color = iconColor, contentDescription = null)
    }
}
