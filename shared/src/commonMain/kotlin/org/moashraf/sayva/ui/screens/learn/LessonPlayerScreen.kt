package org.moashraf.sayva.ui.screens.learn

import androidx.compose.foundation.background
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
import org.koin.compose.koinInject
import org.moashraf.sayva.ui.components.a11yButtonRow
import org.moashraf.sayva.ui.viewmodel.LearnViewModel
import org.moashraf.sayva.designsystem.ErrorColor
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.SurfaceDim
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.speech.speakText

@Composable
fun LessonPlayerScreen(nav: SayvaNavController, lessonId: String) {
    val viewModel: LearnViewModel = koinInject()
    // Fall back to the sole seeded lesson when an unknown id is passed —
    // safer than crashing the screen. Real content will replace this fallback.
    val lesson = viewModel.lesson(lessonId) ?: viewModel.lesson("l-hello") ?: return

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(icon = "arrow_back", label = "Back", onClick = { nav.back() })
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(lesson.categoryLabel, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Text(lesson.indexLabel, style = MaterialTheme.typography.titleSmall)
                }
                CircleIconButton(icon = "bookmark_border", label = "Bookmark lesson", onClick = {})
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(6.dp)
                    .background(Outline, RoundedCornerShape(3.dp)),
            ) {
                Box(modifier = Modifier.fillMaxWidth(0.5f).height(6.dp).background(Tertiary50, RoundedCornerShape(3.dp)))
            }

            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF005544), Tertiary50)), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = lesson.icon, size = 140.dp, color = Color.White, filled = true)

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SymbolIcon(name = "circle", size = 12.dp, color = ErrorColor, filled = true)
                    Spacer(Modifier.width(6.dp))
                    Text("SLOW-MO", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                            .clickable { speakText(lesson.title) },
                        contentAlignment = Alignment.Center,
                    ) {
                        SymbolIcon(name = "play_arrow", size = 18.dp, color = OnSurface, filled = true)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(4.dp).background(Color.White, RoundedCornerShape(2.dp)))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("0.5×", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(lesson.title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(lesson.description, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    lesson.tags.forEach { tag ->
                        val icon = when (tag) {
                            "One hand" -> "back_hand"
                            "Forward motion" -> "trending_flat"
                            else -> "sign_language"
                        }
                        Row(
                            modifier = Modifier
                                .background(SurfaceDim, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SymbolIcon(name = icon, size = 14.dp, color = OnSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(tag, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(SurfaceDim, RoundedCornerShape(100))
                    .a11yButtonRow(label = "Replay ${lesson.title}") { speakText(lesson.title) }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(name = "slow_motion_video", size = 18.dp, color = OnSurface, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Replay", style = MaterialTheme.typography.titleSmall, color = OnSurface)
            }
            Row(
                modifier = Modifier
                    .weight(1.5f)
                    .background(Primary40, RoundedCornerShape(100))
                    .a11yButtonRow(label = "Try practicing ${lesson.title}") {
                        nav.navigate(Screen.Practice(lesson.id))
                    }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(name = "videocam", size = 18.dp, color = Color.White, filled = true, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Try it", style = MaterialTheme.typography.titleSmall, color = Color.White)
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
