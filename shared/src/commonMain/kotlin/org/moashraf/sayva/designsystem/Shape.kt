package org.moashraf.sayva.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object SayvaSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

object SayvaShape {
    val xs = RoundedCornerShape(4.dp)
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(16.dp)
    val lg = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(100)
}

val SayvaShapes = Shapes(
    extraSmall = SayvaShape.xs,
    small = SayvaShape.sm,
    medium = SayvaShape.md,
    large = SayvaShape.lg,
    extraLarge = SayvaShape.pill,
)
