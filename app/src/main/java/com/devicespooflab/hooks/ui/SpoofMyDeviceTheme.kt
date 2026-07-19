package com.devicespooflab.hooks.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.view.WindowCompat
import com.devicespooflab.hooks.data.AppSettingsStore
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicColorScheme

private val mintSeed = Color(0xFF006B5E)
private val blueSeed = Color(0xFF315DA8)
private val roseSeed = Color(0xFF9B405E)
private val amberSeed = Color(0xFF825500)

@Composable
fun SpoofMyDeviceTheme(
    themeMode: String? = null,
    languageMode: String? = null,
    useSystemColor: Boolean? = null,
    colorStyle: String? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val systemConfiguration = LocalConfiguration.current
    val selectedThemeMode = themeMode ?: AppSettingsStore.getThemeMode(context)
    val selectedLanguageMode = languageMode ?: AppSettingsStore.getLanguageMode(context)
    val selectedSystemColor = useSystemColor ?: AppSettingsStore.isSystemColorEnabled(context)
    val selectedColorStyle = colorStyle ?: AppSettingsStore.getColorStyle(context)
    val systemIsDark = isSystemInDarkTheme()
    val isDark = when (selectedThemeMode) {
        AppSettingsStore.THEME_LIGHT -> false
        AppSettingsStore.THEME_DARK -> true
        else -> systemIsDark
    }
    val seedColor = remember(selectedSystemColor, selectedColorStyle) {
        if (selectedSystemColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Color(resources.getColor(android.R.color.system_accent1_500, context.theme))
        } else {
            when (selectedColorStyle) {
                AppSettingsStore.COLOR_STYLE_BLUE -> blueSeed
                AppSettingsStore.COLOR_STYLE_ROSE -> roseSeed
                AppSettingsStore.COLOR_STYLE_AMBER -> amberSeed
                else -> mintSeed
            }
        }
    }
    val generatedColorScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )
    val colorScheme = animateColorScheme(generatedColorScheme)
    val localizedConfiguration = remember(systemConfiguration, selectedLanguageMode) {
        Configuration(systemConfiguration).apply {
            if (selectedLanguageMode != AppSettingsStore.LANGUAGE_DEFAULT) {
                val locales = LocaleList.forLanguageTags(selectedLanguageMode)
                setLocales(locales)
                setLayoutDirection(locales[0])
            }
        }
    }
    val localizedContext = remember(context, localizedConfiguration) {
        val configurationContext = context.createConfigurationContext(localizedConfiguration)
        object : ContextWrapper(context) {
            override fun getResources() = configurationContext.resources
        }
    }

    SideEffect {
        context.findActivity()?.let { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        LocalResources provides localizedContext.resources,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
