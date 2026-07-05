package org.moashraf.sayva.ui.screens.critical

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary20
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.SecondaryContainer
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.components.Pill

private data class PlusPlan(val title: String, val sub: String, val price: String, val priceSub: String?, val badge: String?)

private val plans = listOf(
    PlusPlan("Monthly", "Pause anytime", "$4.99", "/ month", null),
    PlusPlan("Yearly", "7-day free trial", "$39.99", "$3.33/mo · billed yearly", "SAVE 33%"),
    PlusPlan("Lifetime", "Pay once · all future updates", "$99", null, "FOREVER"),
)

private val features = listOf(
    "All 14 sign languages",
    "Voice cloning · your own voice",
    "Partner display · pair to another screen",
    "Sign-language video support · within 1 h",
)

@Composable
fun PaywallScreen(nav: SayvaNavController) {
    var selectedPlan by remember { mutableStateOf("Yearly") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(SurfaceContainer, CircleShape)
                            .clickable { nav.back() },
                        contentAlignment = Alignment.Center,
                    ) {
                        SymbolIcon(name = "close", size = 18.dp, color = OnSurface)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Pill(
                        text = "SAYVA PLUS",
                        backgroundColor = Color(0xFFE69500),
                        contentColor = Color.White,
                        icon = "workspace_premium",
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Translation without limits.",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Unlimited conversations · all 14 languages · voice cloning · partner display · priority support.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    plans.forEach { plan ->
                        PlanCard(
                            plan = plan,
                            selected = plan.title == selectedPlan,
                            onClick = { selectedPlan = plan.title },
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    features.forEach { feature ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SymbolIcon(name = "check_circle", size = 16.dp, color = Tertiary50, filled = true)
                            Spacer(Modifier.width(8.dp))
                            Text(feature, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(140.dp)) }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.White, Color.White)),
                )
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Primary40, RoundedCornerShape(100))
                    .clickable { nav.back() }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Start 7-day free trial", style = MaterialTheme.typography.titleSmall, color = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Free for the deaf community · apply here",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Restore purchase",
                style = MaterialTheme.typography.labelSmall,
                color = Primary40,
                modifier = Modifier.clickable { nav.back() },
            )
        }
    }
}

@Composable
private fun PlanCard(plan: PlusPlan, selected: Boolean, onClick: () -> Unit) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (selected) Brush.linearGradient(listOf(PrimaryContainer, Color(0xFFFCFCFF))) else Brush.linearGradient(listOf(Color.White, Color.White)),
                    RoundedCornerShape(16.dp),
                )
                .border(if (selected) 2.dp else 1.dp, if (selected) Primary40 else Outline, RoundedCornerShape(16.dp))
                .clickable { onClick() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(if (selected) Primary40 else Color.Transparent, CircleShape)
                    .border(2.dp, if (selected) Primary40 else OutlineStrong, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) SymbolIcon(name = "check", size = 13.dp, color = Color.White, filled = true)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    plan.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) Primary20 else OnSurface,
                )
                Text(plan.sub, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    plan.price,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) Primary20 else OnSurface,
                )
                plan.priceSub?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                }
            }
        }
        plan.badge?.let { badge ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp)
                    .offset(y = (-10).dp)
                    .background(
                        if (badge == "SAVE 33%") Tertiary50 else SecondaryContainer,
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (badge == "SAVE 33%") Color.White else Color(0xFF8C2F25),
                )
            }
        }
    }
}
