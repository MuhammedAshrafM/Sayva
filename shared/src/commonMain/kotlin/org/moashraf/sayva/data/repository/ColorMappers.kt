package org.moashraf.sayva.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Small helpers for persisting Compose [Color] values in SQLite TEXT columns.
 *
 * Format: uppercase ARGB hex, no `#` prefix, always 8 chars.
 * Example: `Color.Red` → `"FFFF0000"`, `Color(0x805B5FEF)` → `"805B5FEF"`.
 *
 * Gradient / color-list format: pipe-separated hex values.
 * Example: `[Color.Red, Color.Blue]` → `"FFFF0000|FF0000FF"`.
 *
 * Rationale for pipe over JSON: no serialization library needed, one-line
 * encode / decode, zero external deps, and pipes never appear inside a hex
 * color string. If we later need structured metadata (stops, angles), we
 * upgrade to JSON at that time.
 */
internal fun Color.toHexArgb(): String {
    val argbInt = this.toArgb()
    // Signed Int → unsigned Long so negative alpha bytes don't drop chars.
    val unsigned = argbInt.toLong() and 0xFFFFFFFFL
    return unsigned.toString(16).padStart(8, '0').uppercase()
}

/**
 * Parse an ARGB hex string produced by [toHexArgb]. Malformed input returns
 * a safe default (transparent) rather than throwing — the DB round-trip must
 * survive corrupted rows without crashing the UI.
 */
internal fun String.toColorFromHex(): Color = try {
    Color(this.toLong(16))
} catch (_: NumberFormatException) {
    Color(0x00000000)
}

/** Encode a list of colors as `"FFRRGGBB|FFRRGGBB|..."`. Empty list → empty string. */
internal fun List<Color>.encodeGradient(): String =
    joinToString(separator = "|") { it.toHexArgb() }

/** Decode a gradient string produced by [encodeGradient]. Empty string → empty list. */
internal fun String.decodeGradient(): List<Color> =
    if (isBlank()) emptyList()
    else split("|").filter { it.isNotBlank() }.map { it.toColorFromHex() }
