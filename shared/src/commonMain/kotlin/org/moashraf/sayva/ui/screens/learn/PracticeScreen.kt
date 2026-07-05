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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.ui.components.a11yButtonRow
import org.moashraf.sayva.ui.viewmodel.LearnViewModel
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.designsystem.WarningColor
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController

@Composable
fun PracticeScreen(nav: SayvaNavController, lessonId: String) {
    val viewModel: LearnViewModel = koinInject()
    val questions = viewModel.quiz(lessonId)
    var index by remember { mutableStateOf(0) }
    var answered by remember { mutableStateOf(false) }

    val question = questions.getOrNull(index)

    // XP earned so far — mirrors the "${110 + index * 20} XP" chip formula so
    // the totals stay consistent between what the user sees and what the DB
    // records on completion.
    fun xpAtIndex(i: Int): Int = 110 + i * 20

    fun advance() {
        if (index + 1 >= questions.size) {
            // Final question answered — commit XP and stats before navigating.
            viewModel.completePracticeSession(
                xpEarned = xpAtIndex(questions.size - 1),
                signsLearned = questions.size,
                lessonCompleted = true,
                lessonId = lessonId,
            )
            nav.navigate(Screen.Progress)
        } else {
            index += 1
            answered = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF3A3550), Color(0xFF1A1B25), Color(0xFF0A0B12)),
                ),
            ),
    ) {
        // Top bar: close + progress + XP
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Close practice") { nav.back() }
                    .semantics { contentDescription = "Close" },
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "close", size = 18.dp, color = Color.White, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Q ${(index + 1).coerceAtMost(questions.size)} of ${questions.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(3.dp))) {
                    val fraction = if (questions.isNotEmpty()) (index + if (answered) 1 else 0).toFloat() / questions.size else 0f
                    Box(modifier = Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).background(Tertiary50, RoundedCornerShape(3.dp)))
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .background(Color(0x33FFD700), RoundedCornerShape(100))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(name = "bolt", size = 14.dp, color = Color(0xFFFFD700), filled = true)
                Spacer(Modifier.width(4.dp))
                Text("${xpAtIndex(index)} XP", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD700))
            }
        }

        // Prompt card
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .padding(top = 88.dp)
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                .padding(14.dp),
        ) {
            Column {
                Text(
                    "SIGN THE PHRASE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "\"${question?.targetPhrase ?: ""}\"",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    // Announce the new prompt on each question advance.
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                )
            }
        }

        if (answered) {
            // Live feedback chips
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 300.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FeedbackChip(text = "Handshape ✓", color = Tertiary50, icon = "check_circle")
                FeedbackChip(text = "Position ✓", color = Tertiary50, icon = "check_circle")
                FeedbackChip(text = "Slow it down", color = WarningColor, icon = "timer")
            }
        }

        // Bottom result card + actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 48.dp)
                .fillMaxWidth(),
        ) {
            if (answered) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xD9141524), RoundedCornerShape(24.dp))
                        .padding(16.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).background(Tertiary50, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                SymbolIcon(name = "auto_awesome", size = 18.dp, color = Color.White, filled = true)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Almost there — 84%", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                Text(
                                    "Move hand a touch slower next time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(3.dp))) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.84f)
                                    .height(6.dp)
                                    .background(Brush.linearGradient(listOf(Tertiary50, Color(0xFF6BCFAB))), RoundedCornerShape(3.dp)),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (answered) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(100))
                            .a11yButtonRow(label = "Retry") { answered = false }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("Retry", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    Row(
                        modifier = Modifier
                            .weight(1.5f)
                            .background(Tertiary50, RoundedCornerShape(100))
                            .a11yButtonRow(label = "Accept and next") { advance() }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Accept & next", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(Modifier.width(6.dp))
                        SymbolIcon(name = "arrow_forward", size = 16.dp, color = Color.White, contentDescription = null)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(100))
                            .a11yButtonRow(label = "Skip question") { advance() }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text("Skip", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    Row(
                        modifier = Modifier
                            .weight(1.5f)
                            .background(Tertiary50, RoundedCornerShape(100))
                            .a11yButtonRow(label = "I signed it, check my answer") { answered = true }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SymbolIcon(name = "videocam", size = 18.dp, color = Color.White, filled = true, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("I signed it", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackChip(text: String, color: Color, icon: String) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(name = icon, size = 12.dp, color = Color.White, filled = true)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}
