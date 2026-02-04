package com.radio.ccbes.util

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.content.res.Configuration

/**
 * Utilidades para detectar y adaptar la UI según el tamaño de pantalla
 */
object ScreenSizeUtils {
    
    /**
     * Determina si el dispositivo es una tablet basándose en el WindowSizeClass
     */
    fun isTablet(windowSizeClass: WindowSizeClass): Boolean {
        return windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded ||
               windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
    }
    
    /**
     * Determina si el dispositivo es una tablet grande (10" o más)
     */
    fun isLargeTablet(windowSizeClass: WindowSizeClass): Boolean {
        return windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    }
    
    /**
     * Retorna el padding adaptativo según el tamaño de pantalla
     */
    fun getAdaptivePadding(windowSizeClass: WindowSizeClass): Dp {
        return when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> 16.dp
            WindowWidthSizeClass.Medium -> 24.dp
            WindowWidthSizeClass.Expanded -> 32.dp
            else -> 16.dp
        }
    }
    
    /**
     * Retorna el ancho máximo del contenido para evitar líneas muy largas
     */
    fun getMaxContentWidth(windowSizeClass: WindowSizeClass): Dp {
        return when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> Dp.Infinity
            WindowWidthSizeClass.Medium -> 800.dp
            WindowWidthSizeClass.Expanded -> 1200.dp
            else -> Dp.Infinity
        }
    }
    
    /**
     * Retorna el número de columnas para grids adaptativos
     */
    fun getGridColumns(windowSizeClass: WindowSizeClass): Int {
        return when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> 1
            WindowWidthSizeClass.Medium -> 2
            WindowWidthSizeClass.Expanded -> 3
            else -> 1
        }
    }
}

/**
 * Composable para detectar si el dispositivo está en modo landscape
 */
@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}
