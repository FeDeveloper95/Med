package com.fedeveloper95.med.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.fedeveloper95.med.PREF_THEME
import com.fedeveloper95.med.R
import com.fedeveloper95.med.THEME_DARK
import com.fedeveloper95.med.THEME_LIGHT
import com.fedeveloper95.med.THEME_SYSTEM

// --- Fonts ---
@androidx.compose.ui.text.ExperimentalTextApi
val GoogleSansFlex = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f),
            FontVariation.Setting("ROND", 100f)
        )
    )
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable
fun MedTheme(
    themeOverride: Int? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }
    // Usa l'override se presente (per SettingsActivity), altrimenti leggi dalle preferenze
    val themePref = themeOverride ?: prefs.getInt(PREF_THEME, THEME_SYSTEM)

    val darkTheme = when(themePref) {
        THEME_LIGHT -> false
        THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) darkColorScheme() else lightColorScheme()
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = GoogleSansFlex),
            displayMedium = MaterialTheme.typography.displayMedium.copy(fontFamily = GoogleSansFlex),
            displaySmall = MaterialTheme.typography.displaySmall.copy(fontFamily = GoogleSansFlex),
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = GoogleSansFlex),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = GoogleSansFlex),
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontFamily = GoogleSansFlex),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = GoogleSansFlex),
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = GoogleSansFlex),
            titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = GoogleSansFlex),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = GoogleSansFlex),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = GoogleSansFlex),
            bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = GoogleSansFlex),
            labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = GoogleSansFlex),
            labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = GoogleSansFlex),
            labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = GoogleSansFlex)
        ),
        content = content
    )
}