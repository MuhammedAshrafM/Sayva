package sayva.shared.generated.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font

import sayva.shared.generated.resources.Res
import sayva.shared.generated.resources.material_symbols_filled
import sayva.shared.generated.resources.material_symbols_outline
import sayva.shared.generated.resources.plus_jakarta_sans_bold
import sayva.shared.generated.resources.plus_jakarta_sans_extrabold
import sayva.shared.generated.resources.plus_jakarta_sans_light
import sayva.shared.generated.resources.plus_jakarta_sans_medium
import sayva.shared.generated.resources.plus_jakarta_sans_regular
import sayva.shared.generated.resources.plus_jakarta_sans_semibold

@Composable
fun plusJakartaSans(): FontFamily {
    val light = Font(Res.font.plus_jakarta_sans_light, FontWeight.Light)
    val regular = Font(Res.font.plus_jakarta_sans_regular, FontWeight.Normal)
    val medium = Font(Res.font.plus_jakarta_sans_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.plus_jakarta_sans_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.plus_jakarta_sans_bold, FontWeight.Bold)
    val extraBold = Font(Res.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
    return remember { FontFamily(light, regular, medium, semiBold, bold, extraBold) }
}

@Composable
fun materialSymbolsOutlineFamily(): FontFamily {
    val font = Font(Res.font.material_symbols_outline)
    return remember { FontFamily(font) }
}

@Composable
fun materialSymbolsFilledFamily(): FontFamily {
    val font = Font(Res.font.material_symbols_filled)
    return remember { FontFamily(font) }
}

@Composable
fun sayvaTypography(): Typography {
    val font = plusJakartaSans()
    return Typography(
        displayLarge = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.04).sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.02).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.01).sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.01).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.04.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.02.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = font,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.02.sp,
        ),
    )
}
