/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorPalette = darkColors(
    primary = DarkThemePrimary,
    primaryVariant = DarkThemePrimaryVariant,
    secondary = DarkThemeSecondary,
    secondaryVariant = DarkThemeSecondaryVariant,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onPrimary = DarkOnPrimary)

private val LightColorPalette = lightColors(
    primary = LightThemePrimary,
    primaryVariant = LightThemePrimaryVariant,
    secondary = LightThemeSecondary,
    secondaryVariant = LightThemeSecondaryVariant,
    background = LightBackground,
    surface = LightSurface,
    error = LightError,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onPrimary = LightOnPrimary)

@Composable fun BootyCrateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(Color.Transparent)

    val colors = if (darkTheme) DarkColorPalette
                 else           LightColorPalette
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content)
}