package com.ritesh.cashiro.presentation.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ritesh.cashiro.data.preferences.AccentColor
import com.ritesh.cashiro.presentation.ui.theme.*

fun AccentColor.toArgb(): Int = when (this) {
    AccentColor.ROSEWATER -> Latte_Rosewater
    AccentColor.FLAMINGO -> Latte_Flamingo
    AccentColor.PINK -> Latte_Pink
    AccentColor.MAUVE -> Latte_Mauve
    AccentColor.RED -> Latte_Red
    AccentColor.PEACH -> Latte_Peach
    AccentColor.YELLOW -> Latte_Yellow
    AccentColor.GREEN -> Latte_Green
    AccentColor.TEAL -> Latte_Teal
    AccentColor.SAPPHIRE -> Latte_Sapphire
    AccentColor.BLUE -> Latte_Blue
    AccentColor.LAVENDER -> Latte_Lavender
    AccentColor.PINE_ROSE -> Dawn_Rose
    AccentColor.PINE_IRIS -> Dawn_Iris
    AccentColor.PINE_PINE -> Dawn_Pine
    AccentColor.PINE_GOLD -> Dawn_Gold
    AccentColor.PINE_LOVE -> Dawn_Love
    AccentColor.PINE_FOAM -> Dawn_Foam
    AccentColor.PINE_MUTED -> Dawn_Muted
    AccentColor.PINE_SUBTLE -> Dawn_Subtle
    AccentColor.PINE_TEXT -> Dawn_Text
    AccentColor.PINE_HIGHLIGHT -> Dawn_Highlight
    AccentColor.PINE_SURFACE -> Dawn_Surface
    AccentColor.PINE_OVERLAY -> Dawn_Overlay
}.toArgb()
