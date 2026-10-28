package com.mamad.peugeot405

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---------- رنگ‌های برنامه ----------
val Bg = Color(0xFF060709)
val Panel = Color(0xFF101318)
val Silver = Color(0xFFE6E9F0)
val Steel = Color(0xFF8B93A5)
val Cyan = Color(0xFF4FC3F7)
val Violet = Color(0xFFB388FF)
val Gold = Color(0xFFFFC857)
val Mint = Color(0xFF7DD3C0)
val Ok = Color(0xFF4ADE80)
val Danger = Color(0xFFF87171)

// نسبت ارتفاع به عرض عکس شیر (peugeot_lion.png)
const val LION_RATIO = 1.058f

// پس‌زمینه‌ی کل برنامه: مشکی با دو هاله‌ی نرم رنگی گوشه‌ها
@Composable
fun Backdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.background(Bg).drawBehind {
            drawRect(
                Brush.radialGradient(
                    listOf(Cyan.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.85f, 0f), radius = size.width
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Violet.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(0f, size.height), radius = size.width
                )
            )
        },
        content = content
    )
}
