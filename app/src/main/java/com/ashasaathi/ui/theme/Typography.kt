package com.ashasaathi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    displayLarge  = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold,    lineHeight = 56.sp),
    displayMedium = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold,    lineHeight = 48.sp),
    displaySmall  = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.SemiBold,lineHeight = 44.sp),
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold,    lineHeight = 40.sp),
    headlineMedium= TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold,    lineHeight = 34.sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold,lineHeight = 30.sp),
    titleLarge    = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold,lineHeight = 28.sp),
    titleMedium   = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium,  lineHeight = 24.sp),
    titleSmall    = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium,  lineHeight = 22.sp),
    bodyLarge     = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal,  lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal,  lineHeight = 22.sp),
    bodySmall     = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal,  lineHeight = 20.sp),
    labelLarge    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold,lineHeight = 20.sp),
    labelMedium   = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium,  lineHeight = 18.sp),
    labelSmall    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium,  lineHeight = 16.sp),
)
