
package com.example.tripplan.style

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val suit = FontFamily.SansSerif

class Style(base: TextStyle) {
    val heavy: TextStyle = base.copy(
        fontWeight = FontWeight.Black
    )
    val extraBold: TextStyle = base.copy(
        fontWeight = FontWeight.ExtraBold
    )
    val bold: TextStyle = base.copy(
        fontWeight = FontWeight.Bold
    )
    val semiBold: TextStyle = base.copy(
        fontWeight = FontWeight.SemiBold
    )
    val medium: TextStyle = base.copy(
        fontWeight = FontWeight.Medium
    )
    val regular: TextStyle = base.copy(
        fontWeight = FontWeight.Normal
    )
    val light: TextStyle = base.copy(
        fontWeight = FontWeight.Light
    )
    val extraLight: TextStyle = base.copy(
        fontWeight = FontWeight.ExtraLight
    )
    val thin: TextStyle = base.copy(
        fontWeight = FontWeight.Thin
    )
}

object TextStyles {
    val chargeValue = Style(
        base = TextStyle(
            fontFamily = suit,
            fontSize = 120.sp,
            lineHeight = 100.sp,
            letterSpacing = (-0.8).sp
        )
    )
    val chargeValue_2 = Style(
        base = TextStyle(
            fontFamily = suit,
            fontSize = 60.sp,
            lineHeight = 100.sp,
            letterSpacing = (-0.8).sp
        )
    )
    val chargePercentage = Style(
        base = TextStyle(
            fontFamily = suit,
            fontSize = 38.sp,
            lineHeight = 100.sp,
            letterSpacing = (-0.8).sp
        )
    )
    val display00 = Style(
        TextStyle(
            fontFamily = suit,
            fontSize = 40.sp,
            lineHeight = 52.sp,
            letterSpacing = (-0.8).sp
        )
    )

    val display01 = Style(
        TextStyle(
            fontFamily = suit,
            fontSize = 32.sp,
            lineHeight = 46.sp,
            letterSpacing = (-0.64).sp
        )
    )

    val display02 = Style(
        TextStyle(
            fontFamily = suit,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.56).sp
        )
    )

    val display03 = Style(
        TextStyle(
            fontFamily = suit,
            fontSize = 40.sp,
            lineHeight = 46.sp,
            letterSpacing = (-0.8).sp
        )
    )

    val title01 = Style(
        TextStyle(
            fontFamily = suit,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.48).sp
        )
    )

    val header01 = Style(
        TextStyle(
            fontFamily = suit,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.4).sp
        )
    )

    val header02 = Style(
        TextStyle(
            fontFamily = suit,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.36).sp
        )
    )

    val body01 = Style(
        TextStyle(
            fontFamily = suit,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.32).sp
        )
    )

    val body02 = Style(
        TextStyle(
            fontFamily = suit,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.28).sp
        )
    )

    val body03 = Style(
        TextStyle(
            fontFamily = suit,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = (-0.24).sp
        )
    )

    val caption01 = Style(
        TextStyle(
            fontFamily = suit,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = (-0.22).sp
        )
    )

    val caption02 = Style(
        TextStyle(
            fontFamily = suit,
            fontSize = 10.sp,
            lineHeight = 16.sp,
            letterSpacing = (-0.2).sp
        )
    )
}
