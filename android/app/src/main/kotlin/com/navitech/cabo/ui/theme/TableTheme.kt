package com.navitech.cabo.ui.theme

import androidx.compose.ui.graphics.Color

enum class BoardTheme(val raw: String) {
    FOREST("forest"),
    OCEAN("ocean"),
    SUNSET("sunset"),
    AMETHYST("amethyst"),
    RUBY("ruby"),
    OBSIDIAN("obsidian");

    companion object {
        fun fromRaw(raw: String?): BoardTheme {
            val key = when (raw) {
                "midnight" -> "amethyst"
                else -> raw
            }
            return entries.firstOrNull { it.raw == key } ?: FOREST
        }
    }
}

data class BoardPalette(
    val gradientTop: Color,
    val gradientBottom: Color,
    val felt: Color,
    val feltBorder: Color,
    val accentPrimary: Color,
    val accentGold: Color,
    val actionLabelOnPrimary: Color,
)

fun BoardTheme.palette(): BoardPalette = when (this) {
    BoardTheme.FOREST -> BoardPalette(
        gradientTop = Color(0xFF0D1913),
        gradientBottom = Color(0xFF08100D),
        felt = Color(0xFF15281E),
        feltBorder = Color(0xFF2E8E67),
        accentPrimary = Color(0xFF4CAF50),
        accentGold = Color(0xFFF2BF4D),
        actionLabelOnPrimary = Color(0xFF101A14),
    )

    BoardTheme.OCEAN -> BoardPalette(
        gradientTop = Color(0xFF0F1720),
        gradientBottom = Color(0xFF080E16),
        felt = Color(0xFF142A38),
        feltBorder = Color(0xFF4090D8),
        accentPrimary = Color(0xFF59B8F0),
        accentGold = Color(0xFFF2CC73),
        actionLabelOnPrimary = Color(0xFF0E141C),
    )

    BoardTheme.SUNSET -> BoardPalette(
        gradientTop = Color(0xFF1C160A),
        gradientBottom = Color(0xFF0F0C06),
        felt = Color(0xFF382E14),
        feltBorder = Color(0xFFD4A82A),
        accentPrimary = Color(0xFFF0B01C),
        accentGold = Color(0xFFFFDD66),
        actionLabelOnPrimary = Color(0xFF1A1408),
    )

    BoardTheme.AMETHYST -> BoardPalette(
        gradientTop = Color(0xFF0F0F1E),
        gradientBottom = Color(0xFF080814),
        felt = Color(0xFF1A1A33),
        feltBorder = Color(0xFF7366D8),
        accentPrimary = Color(0xFF8C83F4),
        accentGold = Color(0xFFC0B8F8),
        actionLabelOnPrimary = Color(0xFF0E0E18),
    )

    BoardTheme.RUBY -> BoardPalette(
        gradientTop = Color(0xFF1A0E14),
        gradientBottom = Color(0xFF0D080C),
        felt = Color(0xFF2E1420),
        feltBorder = Color(0xFFD94866),
        accentPrimary = Color(0xFFF26688),
        accentGold = Color(0xFFF2BF66),
        actionLabelOnPrimary = Color(0xFF160A10),
    )

    BoardTheme.OBSIDIAN -> BoardPalette(
        gradientTop = Color(0xFF0A0A0C),
        gradientBottom = Color(0xFF050507),
        felt = Color(0xFF121419),
        feltBorder = Color(0xFF404D61),
        accentPrimary = Color(0xFF6EB3F7),
        accentGold = Color(0xFF9EAEC7),
        actionLabelOnPrimary = Color(0xFF080809),
    )
}

enum class CardBackPattern {
    SOLID,
    STRIPES,
    DOTS,
    DIAMONDS,
    CROSSHATCH,
    PLAID,
    SQUIGGLES,
}

enum class CardDeckStyle(val raw: String) {
    CLASSIC("classic"),
    TARTAN("tartan"),
    MARINA("marina"),
    SAFFRON("saffron"),
    VELVET("velvet"),
    CASINO("casino"),
    NOIR("noir");

    companion object {
        fun fromRaw(raw: String?): CardDeckStyle {
            val key = when (raw) {
                "amethyst" -> "velvet"
                else -> raw
            }
            return entries.firstOrNull { it.raw == key } ?: CLASSIC
        }
    }
}

fun CardDeckStyle.faceBackground(): Color = when (this) {
    CardDeckStyle.CLASSIC -> Color.White
    CardDeckStyle.TARTAN -> Color(0xFFF5F0E0)
    CardDeckStyle.MARINA -> Color(0xFFF5F7F0)
    CardDeckStyle.SAFFRON -> Color(0xFFFFF8E0)
    CardDeckStyle.VELVET -> Color(0xFFFCF5FA)
    CardDeckStyle.CASINO -> Color(0xFFFCF7EA)
    CardDeckStyle.NOIR -> Color(0xFF2E2E32)
}

fun CardDeckStyle.redSuit(): Color = when (this) {
    CardDeckStyle.CLASSIC -> Color(0xFFD9262E)
    CardDeckStyle.TARTAN -> Color(0xFFAE3034)
    CardDeckStyle.MARINA -> Color(0xFFD13852)
    CardDeckStyle.SAFFRON -> Color(0xFFC74724)
    CardDeckStyle.VELVET -> Color(0xFFB8295C)
    CardDeckStyle.CASINO -> Color(0xFFB81A24)
    CardDeckStyle.NOIR -> Color(0xFFF27272)
}

fun CardDeckStyle.blackSuit(): Color = when (this) {
    CardDeckStyle.CLASSIC -> Color(0xFF14161A)
    CardDeckStyle.TARTAN -> Color(0xFF2E3034)
    CardDeckStyle.MARINA -> Color(0xFF243656)
    CardDeckStyle.SAFFRON -> Color(0xFF382914)
    CardDeckStyle.VELVET -> Color(0xFF33244D)
    CardDeckStyle.CASINO -> Color(0xFF1F1A1C)
    CardDeckStyle.NOIR -> Color(0xFFDCE1EB)
}

fun CardDeckStyle.backBase(): Color = when (this) {
    CardDeckStyle.CLASSIC -> Color(0xFF172138)
    CardDeckStyle.TARTAN -> Color(0xFF24331E)
    CardDeckStyle.MARINA -> Color(0xFF125260)
    CardDeckStyle.SAFFRON -> Color(0xFFB87A14)
    CardDeckStyle.VELVET -> Color(0xFF4D1A42)
    CardDeckStyle.CASINO -> Color(0xFF8C1E2E)
    CardDeckStyle.NOIR -> Color(0xFF1A1A1C)
}

fun CardDeckStyle.backAccent(): Color = when (this) {
    CardDeckStyle.CLASSIC -> Color(0xFF476178)
    CardDeckStyle.TARTAN -> Color(0xFF7AB062)
    CardDeckStyle.MARINA -> Color(0xFF8CE0DC)
    CardDeckStyle.SAFFRON -> Color(0xFFFFE884)
    CardDeckStyle.VELVET -> Color(0xFFF09E95)
    CardDeckStyle.CASINO -> Color(0xFFF2DB8C)
    CardDeckStyle.NOIR -> Color(0xFF6B6D74)
}

fun CardDeckStyle.backPattern(): CardBackPattern = when (this) {
    CardDeckStyle.CLASSIC -> CardBackPattern.SOLID
    CardDeckStyle.TARTAN -> CardBackPattern.PLAID
    CardDeckStyle.MARINA -> CardBackPattern.STRIPES
    CardDeckStyle.SAFFRON -> CardBackPattern.DOTS
    CardDeckStyle.VELVET -> CardBackPattern.SQUIGGLES
    CardDeckStyle.CASINO -> CardBackPattern.DIAMONDS
    CardDeckStyle.NOIR -> CardBackPattern.CROSSHATCH
}

fun CardDeckStyle.suitColor(text: String): Color =
    if (text.contains("♥") || text.contains("♦")) redSuit() else blackSuit()

fun CardDeckStyle.previewLabel(): String = when (this) {
    CardDeckStyle.CLASSIC -> "Classic"
    CardDeckStyle.TARTAN -> "Tartan"
    CardDeckStyle.MARINA -> "Marina"
    CardDeckStyle.SAFFRON -> "Saffron"
    CardDeckStyle.VELVET -> "Velvet"
    CardDeckStyle.CASINO -> "Casino"
    CardDeckStyle.NOIR -> "Noir"
}

fun BoardTheme.previewLabel(): String = when (this) {
    BoardTheme.FOREST -> "Forest"
    BoardTheme.OCEAN -> "Ocean"
    BoardTheme.SUNSET -> "Sunset"
    BoardTheme.AMETHYST -> "Amethyst"
    BoardTheme.RUBY -> "Ruby"
    BoardTheme.OBSIDIAN -> "Obsidian"
}
