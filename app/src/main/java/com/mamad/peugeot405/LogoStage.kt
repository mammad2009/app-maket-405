package com.mamad.peugeot405

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * آرم «۴۰۵ GLX» با رنگ کروم (برای صفحه‌ی شروع و بالای صفحه‌ی کنترل).
 * scale = اندازه‌ی نسبی (1 = کوچک)
 */
@Composable
fun GlxBadge(scale: Float = 1f, modifier: Modifier = Modifier) {
    val chrome = Brush.verticalGradient(
        listOf(Color(0xFFF7F9FC), Color(0xFF9199A9), Color(0xFFE9ECF2), Color(0xFF5E6573))
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "405",
            style = TextStyle(
                brush = chrome, fontSize = (26f * scale).sp, fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic, letterSpacing = (2f * scale).sp
            )
        )
        Spacer(Modifier.width((10f * scale).dp))
        Box(
            Modifier.border(1.dp, Silver.copy(alpha = 0.7f), RoundedCornerShape((6f * scale).dp))
                .padding(horizontal = (8f * scale).dp, vertical = (2f * scale).dp)
        ) {
            Text(
                "GLX",
                style = TextStyle(
                    brush = chrome, fontSize = (15f * scale).sp, fontWeight = FontWeight.Bold,
                    letterSpacing = (3f * scale).sp
                )
            )
        }
    }
}

/**
 * جای عکس ماشین: فقط آرم شیر پژو با هاله و حلقه‌های متحرک.
 * شیر با بالا/پایین رفتن جلو و عقب کمی کج می‌شود و موقع میباخ طلایی می‌شود.
 */
@Composable
fun LogoStage(
    posF: () -> Float,
    posB: () -> Float,
    maybach: Boolean,
    modifier: Modifier = Modifier
) {
    val gold by animateFloatAsState(if (maybach) 1f else 0f, tween(600), label = "gold")
    val inf = rememberInfiniteTransition(label = "stage")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "spin")
    val pulse by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )
    Box(modifier.fillMaxWidth().height(172.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val accent = lerp(Cyan, Gold, gold)
            val c = Offset(size.width / 2f, size.height * 0.46f)
            val glowR = size.height * 0.95f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.22f + 0.08f * pulse), Color.Transparent), center = c, radius = glowR
                ),
                radius = glowR, center = c
            )
            val r1 = size.height * 0.34f
            drawCircle(accent.copy(alpha = 0.22f), r1, c, style = Stroke(1.dp.toPx()))
            drawArc(
                accent.copy(alpha = 0.95f), spin, 70f, false,
                Offset(c.x - r1, c.y - r1), Size(r1 * 2f, r1 * 2f),
                style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                accent.copy(alpha = 0.55f), spin + 180f, 36f, false,
                Offset(c.x - r1, c.y - r1), Size(r1 * 2f, r1 * 2f),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
            )
            val r2 = r1 + 13.dp.toPx()
            drawCircle(
                accent.copy(alpha = 0.14f), r2, c,
                style = Stroke(
                    1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 8.dp.toPx()), spin)
                )
            )
        }
        Image(
            painterResource(R.drawable.peugeot_lion), null,
            Modifier.align(Alignment.Center).padding(bottom = 14.dp)
                .size(width = 80.dp, height = 80.dp * LION_RATIO)
                .graphicsLayer {
                    val nF = (posF() - 80f) / 32f
                    val nB = (posB() - 84f) / 31f
                    rotationZ = -(nF - nB) * 7f
                    val sc = 1f + 0.015f * pulse
                    scaleX = sc
                    scaleY = sc
                },
            contentScale = ContentScale.Fit
        )
        GlxBadge(1f, Modifier.align(Alignment.BottomCenter))
    }
}
