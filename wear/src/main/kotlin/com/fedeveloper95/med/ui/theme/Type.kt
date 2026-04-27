package com.fedeveloper95.med.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material3.Typography
import com.fedeveloper95.med.R

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(
    Font(
        resId = R.font.sans_flex,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f),
            FontVariation.Setting("ROND", 100f)
        )
    )
)

val Typography = Typography().let {
    it.copy(
        displayLarge = it.displayLarge.copy(fontFamily = GoogleSansFlex),
        displayMedium = it.displayMedium.copy(fontFamily = GoogleSansFlex),
        displaySmall = it.displaySmall.copy(fontFamily = GoogleSansFlex),
        titleLarge = it.titleLarge.copy(fontFamily = GoogleSansFlex),
        titleMedium = it.titleMedium.copy(fontFamily = GoogleSansFlex),
        titleSmall = it.titleSmall.copy(fontFamily = GoogleSansFlex),
        bodyLarge = it.bodyLarge.copy(fontFamily = GoogleSansFlex),
        bodyMedium = it.bodyMedium.copy(fontFamily = GoogleSansFlex),
        bodySmall = it.bodySmall.copy(fontFamily = GoogleSansFlex),
        labelLarge = it.labelLarge.copy(fontFamily = GoogleSansFlex),
        labelMedium = it.labelMedium.copy(fontFamily = GoogleSansFlex),
        labelSmall = it.labelSmall.copy(fontFamily = GoogleSansFlex)
    )
}