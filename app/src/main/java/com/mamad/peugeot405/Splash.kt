package com.mamad.peugeot405

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun seg(p: Float, a: Float, b: Float): Float = ((p - a) / (b - a)).coerceIn(0f, 1f)
private fun easeOut(x: Float): Float = 1f - (1f - x) * (1f - x) * (1f - x)
private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val y = x - 1f
    return 1f + c3 * y * y * y + c1 * y * y
}

/**
 * صفحه‌ی شروع: اول آرم شیر پژو با برق نور می‌آید، بعد اسم PEUGEOT و بعد آرم «۴۰۵ GLX».
 * با یک لمس رد می‌شود.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val prog = remember { Animatable(0f) }
    var done by remember { mutableStateOf(false) }
    val finish = {
        if (!done) {
            done = true
            onFinished()
        }
    }
    LaunchedEffect(Unit) {
        prog.animateTo(1f, tween(3200, easing = LinearEasing))
        finish()
    }

    val p = prog.value
    val lionIn = seg(p, 0.04f, 0.40f)
    val sweep = seg(p, 0.38f, 0.64f)
    val nameIn = easeOut(seg(p, 0.40f, 0.64f))
    val glxIn = easeOut(seg(p, 0.56f, 0.80f))
    val lineIn = easeOut(seg(p, 0.64f, 0.90f))
    val outA = 1f - seg(p, 0.94f, 1f)

    Backdrop(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { finish() } }) {
        Column(
            Modifier.align(Alignment.Center).graphicsLayer { alpha = outA },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(270.dp)) {
                    val a = easeOut(seg(p, 0.08f, 0.50f))
                    val c = center
                    val r = size.minDimension * 0.40f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Cyan.copy(alpha = 0.28f * a), Color.Transparent), center = c, radius = r * 1.7f
                        ),
                        radius = r * 1.7f, center = c
                    )
                    drawCircle(Cyan.copy(alpha = 0.30f * a), r, c, style = Stroke(1.dp.toPx()))
                    drawArc(
                        Cyan.copy(alpha = a), p * 720f, 80f, false,
                        Offset(c.x - r, c.y - r), Size(r * 2f, r * 2f),
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        Silver.copy(alpha = 0.6f * a), p * 720f + 180f, 40f, false,
                        Offset(c.x - r, c.y - r), Size(r * 2f, r * 2f),
                        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Image(
                    painterResource(R.drawable.peugeot_lion), null,
                    Modifier.size(width = 150.dp, height = 150.dp * LION_RATIO)
                        .graphicsLayer {
                            val sc = 0.55f + 0.45f * easeOutBack(lionIn)
                            scaleX = sc
                            scaleY = sc
                            alpha = easeOut(lionIn)
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            if (sweep > 0.001f && sweep < 0.999f) {
                                val w = size.width
                                val x = -w * 0.5f + w * 2f * sweep
                                drawRect(
                                    Brush.linearGradient(
                                        listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent),
                                        start = Offset(x - w * 0.25f, 0f),
                                        end = Offset(x + w * 0.25f, size.height * 0.35f)
                                    ),
                                    blendMode = BlendMode.SrcAtop
                                )
                            }
                        },
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "PEUGEOT", color = Silver, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                letterSpacing = (26f - 18f * nameIn).sp,
                modifier = Modifier.graphicsLayer { alpha = nameIn }
            )
            Spacer(Modifier.height(12.dp))
            GlxBadge(
                1.5f,
                Modifier.graphicsLayer {
                    alpha = glxIn
                    translationY = (1f - glxIn) * 24.dp.toPx()
                }
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.width((150f * lineIn).dp).height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, Cyan, Color.Transparent)))
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "PNEUMATIC CONTROL", color = Steel, fontSize = 10.sp, letterSpacing = 4.sp,
                modifier = Modifier.graphicsLayer { alpha = lineIn }
            )
        }
    }
}
