package com.example.xtreamplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.xtreamplayer.settings.AppThemeOption

data class AppColors(
    val background: Color,
    val backgroundAlt: Color,
    val panelBackground: Color,
    val panelBorder: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceMuted: Color,
    val surfaceFocused: Color,
    val border: Color,
    val borderStrong: Color,
    val focus: Color,
    val accent: Color,
    val accentAlt: Color,
    val accentMuted: Color,
    val accentMutedAlt: Color,
    val accentSelected: Color,
    val accentSelectedAlt: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val navText: Color,
    val textOnAccent: Color,
    val warning: Color,
    val success: Color,
    val error: Color,
    val overlay: Color,
    val overlaySoft: Color,
    val overlayStrong: Color
)

private val DefaultAppColors =
    AppColors(
        background = Color(0xFF0F1626),
        backgroundAlt = Color(0xFF141C2E),
        panelBackground = Color(0xFF111826),
        panelBorder = Color(0xFF1F2B3E),
        surface = Color(0xFF161E2E),
        surfaceAlt = Color(0xFF222E44),
        surfaceMuted = Color(0xFF121A2B),
        surfaceFocused = Color(0xFF1B2740),
        border = Color(0xFF1E2738),
        borderStrong = Color(0xFF2A3348),
        focus = Color(0xFFB6D9FF),
        accent = Color(0xFF4F8CFF),
        accentAlt = Color(0xFF7FCBFF),
        accentMuted = Color(0xFF3A4A5E),
        accentMutedAlt = Color(0xFF2C3550),
        accentSelected = Color(0xFF273479),
        accentSelectedAlt = Color(0xFF1E275C),
        textPrimary = Color(0xFFE6ECF7),
        textSecondary = Color(0xFF94A3B8),
        textTertiary = Color(0xFF7F8CA6),
        navText = Color(0xFFE6ECF7),
        textOnAccent = Color(0xFF0C1730),
        warning = Color(0xFFFFD86A),
        success = Color(0xFF3CCB7F),
        error = Color(0xFFE4A9A9),
        overlay = Color(0x990A0F1A),
        overlaySoft = Color(0x66000000),
        overlayStrong = Color(0xB20A0F1A)
    )

private val DefaultLightAppColors =
    AppColors(
        background = Color(0xFFF5F7FB),
        backgroundAlt = Color(0xFFEEF2F8),
        panelBackground = Color(0xFFF8FAFD),
        panelBorder = Color(0xFFD7DEE8),
        surface = Color(0xFFFFFFFF),
        surfaceAlt = Color(0xFFF1F5FA),
        surfaceMuted = Color(0xFFE7ECF3),
        surfaceFocused = Color(0xFFE1E9F3),
        border = Color(0xFFD4DCE7),
        borderStrong = Color(0xFFC3CCD9),
        focus = Color(0xFF2C6BFF),
        accent = Color(0xFF3E7BFF),
        accentAlt = Color(0xFF7FB0FF),
        accentMuted = Color(0xFFE1E9F5),
        accentMutedAlt = Color(0xFFD7E1F0),
        accentSelected = Color(0xFF2D5ED6),
        accentSelectedAlt = Color(0xFF244FB6),
        textPrimary = Color(0xFF1C2331),
        textSecondary = Color(0xFF4B5C74),
        textTertiary = Color(0xFF6B7A90),
        navText = Color(0xFF1C2331),
        textOnAccent = Color(0xFFFFFFFF),
        warning = Color(0xFFB86B1B),
        success = Color(0xFF0F8A56),
        error = Color(0xFFB64A4A),
        overlay = Color(0x1A0A0F1A),
        overlaySoft = Color(0x33000000),
        overlayStrong = Color(0x4D0A0F1A)
    )

private val PinkAppColors =
    AppColors(
        background = Color(0xFF140B12),
        backgroundAlt = Color(0xFF1A0F18),
        panelBackground = Color(0xFF170E17),
        panelBorder = Color(0xFF2B1A2C),
        surface = Color(0xFF1C1120),
        surfaceAlt = Color(0xFF27162C),
        surfaceMuted = Color(0xFF140C1A),
        surfaceFocused = Color(0xFF2C1A32),
        border = Color(0xFF2C1A31),
        borderStrong = Color(0xFF3A233E),
        focus = Color(0xFFFFB6DD),
        accent = Color(0xFFE0548E),
        accentAlt = Color(0xFFF07DB3),
        accentMuted = Color(0xFF3A2738),
        accentMutedAlt = Color(0xFF2C1F2D),
        accentSelected = Color(0xFF4A2A4D),
        accentSelectedAlt = Color(0xFF3A2140),
        textPrimary = Color(0xFFF6EAF1),
        textSecondary = Color(0xFFD7B9C8),
        textTertiary = Color(0xFFB89BAD),
        navText = Color(0xFFF6EAF1),
        textOnAccent = Color(0xFF2A0D1D),
        warning = Color(0xFFFFD59A),
        success = Color(0xFF3CCB7F),
        error = Color(0xFFF2A3B0),
        overlay = Color(0x99140B12),
        overlaySoft = Color(0x66000000),
        overlayStrong = Color(0xB2160E1A)
    )

private val PinkLightAppColors =
    AppColors(
        background = Color(0xFFFFF5F8),
        backgroundAlt = Color(0xFFFDECF2),
        panelBackground = Color(0xFFFFF7FA),
        panelBorder = Color(0xFFE6C8D6),
        surface = Color(0xFFFFF9FB),
        surfaceAlt = Color(0xFFF8E7EE),
        surfaceMuted = Color(0xFFF4E0E8),
        surfaceFocused = Color(0xFFF0D5E1),
        border = Color(0xFFE3C3D2),
        borderStrong = Color(0xFFD6B1C2),
        focus = Color(0xFFD64583),
        accent = Color(0xFFD64583),
        accentAlt = Color(0xFFF07DB3),
        accentMuted = Color(0xFFF4DDE7),
        accentMutedAlt = Color(0xFFEED1DE),
        accentSelected = Color(0xFFB9346F),
        accentSelectedAlt = Color(0xFF9E2B60),
        textPrimary = Color(0xFF3A1D2D),
        textSecondary = Color(0xFF6A3F54),
        textTertiary = Color(0xFF8C5B72),
        navText = Color(0xFF3A1D2D),
        textOnAccent = Color(0xFFFFFFFF),
        warning = Color(0xFFB86B1B),
        success = Color(0xFF1B8F62),
        error = Color(0xFFC6505D),
        overlay = Color(0x1A140B12),
        overlaySoft = Color(0x33000000),
        overlayStrong = Color(0x4D140B12)
    )

private val GreenAppColors =
    AppColors(
        background = Color(0xFF0B1410),
        backgroundAlt = Color(0xFF0F1B14),
        panelBackground = Color(0xFF0D1712),
        panelBorder = Color(0xFF1F2F24),
        surface = Color(0xFF121F18),
        surfaceAlt = Color(0xFF1A2B22),
        surfaceMuted = Color(0xFF0E1712),
        surfaceFocused = Color(0xFF22352A),
        border = Color(0xFF243427),
        borderStrong = Color(0xFF2E3F33),
        focus = Color(0xFF9BE6C1),
        accent = Color(0xFF2AC57D),
        accentAlt = Color(0xFF5FE3A1),
        accentMuted = Color(0xFF2A3A32),
        accentMutedAlt = Color(0xFF1A2720),
        accentSelected = Color(0xFF1E4D38),
        accentSelectedAlt = Color(0xFF164030),
        textPrimary = Color(0xFFE7F5ED),
        textSecondary = Color(0xFFB4C6BB),
        textTertiary = Color(0xFF92A79A),
        navText = Color(0xFFE7F5ED),
        textOnAccent = Color(0xFF052014),
        warning = Color(0xFFFFD59A),
        success = Color(0xFF3CCB7F),
        error = Color(0xFFF0A1A8),
        overlay = Color(0x990A1110),
        overlaySoft = Color(0x66000000),
        overlayStrong = Color(0xB20A1110)
    )

private val GreenLightAppColors =
    AppColors(
        background = Color(0xFFF2FBF6),
        backgroundAlt = Color(0xFFE9F6F0),
        panelBackground = Color(0xFFF6FCF8),
        panelBorder = Color(0xFFC6E2D1),
        surface = Color(0xFFF8FDFB),
        surfaceAlt = Color(0xFFE3F2EA),
        surfaceMuted = Color(0xFFDAEDE3),
        surfaceFocused = Color(0xFFD0E6DA),
        border = Color(0xFFBFDCCD),
        borderStrong = Color(0xFFA7C9B6),
        focus = Color(0xFF1DAA6A),
        accent = Color(0xFF1DAA6A),
        accentAlt = Color(0xFF5FE3A1),
        accentMuted = Color(0xFFD7EEE4),
        accentMutedAlt = Color(0xFFCBE5D8),
        accentSelected = Color(0xFF168B58),
        accentSelectedAlt = Color(0xFF13734A),
        textPrimary = Color(0xFF153128),
        textSecondary = Color(0xFF355547),
        textTertiary = Color(0xFF4E6D60),
        navText = Color(0xFF153128),
        textOnAccent = Color(0xFFFFFFFF),
        warning = Color(0xFFB86B1B),
        success = Color(0xFF1B8F62),
        error = Color(0xFFC6505D),
        overlay = Color(0x1A0A1110),
        overlaySoft = Color(0x33000000),
        overlayStrong = Color(0x4D0A1110)
    )

private val CopperAppColors =
    AppColors(
        background = Color(0xFF141012),
        backgroundAlt = Color(0xFF1A1417),
        panelBackground = Color(0xFF1A1416),
        panelBorder = Color(0xFF2F2A2D),
        surface = Color(0xFF1F1819),
        surfaceAlt = Color(0xFF2A2021),
        surfaceMuted = Color(0xFF171214),
        surfaceFocused = Color(0xFF2B3239),
        border = Color(0xFF2D3339),
        borderStrong = Color(0xFF3A424B),
        focus = Color(0xFFFFD1B0),
        accent = Color(0xFFC97744),
        accentAlt = Color(0xFFF0B07A),
        accentMuted = Color(0xFF3F2E25),
        accentMutedAlt = Color(0xFF261E1A),
        accentSelected = Color(0xFF6A3F28),
        accentSelectedAlt = Color(0xFF55311F),
        textPrimary = Color(0xFFF2E8E1),
        textSecondary = Color(0xFFC5B5A9),
        textTertiary = Color(0xFF9E8F85),
        navText = Color(0xFFF2E8E1),
        textOnAccent = Color(0xFF1F120C),
        warning = Color(0xFFFFD59A),
        success = Color(0xFF3CCB7F),
        error = Color(0xFFF0A1A8),
        overlay = Color(0x990C0E10),
        overlaySoft = Color(0x66000000),
        overlayStrong = Color(0xB20C0E10)
    )

private val CopperLightAppColors =
    AppColors(
        background = Color(0xFFFBF3EE),
        backgroundAlt = Color(0xFFF7E9E1),
        panelBackground = Color(0xFFFDF6F1),
        panelBorder = Color(0xFFE3CFC3),
        surface = Color(0xFFFFF8F3),
        surfaceAlt = Color(0xFFF1DED3),
        surfaceMuted = Color(0xFFE9D3C7),
        surfaceFocused = Color(0xFFE2C7B9),
        border = Color(0xFFDDC5B8),
        borderStrong = Color(0xFFCBB1A3),
        focus = Color(0xFFC97744),
        accent = Color(0xFFC97744),
        accentAlt = Color(0xFFF0B07A),
        accentMuted = Color(0xFFEAD6CB),
        accentMutedAlt = Color(0xFFE0C8BC),
        accentSelected = Color(0xFFA75E35),
        accentSelectedAlt = Color(0xFF8C4D2B),
        textPrimary = Color(0xFF2E1C14),
        textSecondary = Color(0xFF5B3F33),
        textTertiary = Color(0xFF7A5A4B),
        navText = Color(0xFF2E1C14),
        textOnAccent = Color(0xFFFFFFFF),
        warning = Color(0xFFB86B1B),
        success = Color(0xFF1B8F62),
        error = Color(0xFFC6505D),
        overlay = Color(0x1A0C0E10),
        overlaySoft = Color(0x33000000),
        overlayStrong = Color(0x4D0C0E10)
    )

private val LocalAppColors = staticCompositionLocalOf { DefaultAppColors }

object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}

@Composable
fun XtreamPlayerTheme(
    appTheme: AppThemeOption = AppThemeOption.DEFAULT,
    content: @Composable () -> Unit
) {
    val colors =
        when (appTheme) {
            AppThemeOption.DEFAULT_LIGHT -> DefaultLightAppColors
            AppThemeOption.DARK_PINK -> PinkAppColors
            AppThemeOption.DARK_PINK_LIGHT -> PinkLightAppColors
            AppThemeOption.DARK_GREEN -> GreenAppColors
            AppThemeOption.DARK_GREEN_LIGHT -> GreenLightAppColors
            AppThemeOption.DUSK_COPPER -> CopperAppColors
            AppThemeOption.DUSK_COPPER_LIGHT -> CopperLightAppColors
            else -> DefaultAppColors
        }
    val colorScheme =
        darkColorScheme(
            primary = colors.accent,
            secondary = colors.accentAlt,
            background = colors.background,
            surface = colors.surface,
            onPrimary = colors.textOnAccent,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            error = colors.error
        )

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
