package com.mamad.peugeot405

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFFE6F3FF)

/**
 * بخش «کارخانه»: وقتی واردش می‌شوی ماکت کف‌خواب می‌شود و همین صحنه‌ی کارخانه روی نمایشگرش می‌آید.
 * (ورود/خروج فرمان Y و V را MainViewModel می‌فرستد)
 */
@Composable
fun FactoryScreen(vm: MainViewModel, onExit: () -> Unit) {
    val S = LocalStrings.current
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val t0 = withFrameNanos { it }
        while (true) {
            withFrameNanos { now = (it - t0) / 1_000_000L }
        }
    }
    val typed by remember { derivedStateOf { (((now - 1700L) / 70L).toInt()).coerceIn(0, 12) } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // پیش‌نمایش نمایشگر OLED ماکت
        val shape = RoundedCornerShape(24.dp)
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f).clip(shape).background(Color.Black)
                .border(1.dp, Cyan.copy(alpha = 0.45f), shape)
        ) {
            Canvas(Modifier.fillMaxSize()) { drawFactoryScene(now) }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)) {
                Text(
                    "FACTORY MODE".take(typed), color = Ink, fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 2.sp
                )
            }
        }

        CardBox(Cyan, active = vm.factoryActive) {
            CardTitle(S.facTitle, "FACTORY", Cyan)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).clip(CircleShape)
                        .background(if (vm.factoryActive) Ok else Steel)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (vm.factoryActive) S.facActive else S.facInactive,
                    color = if (vm.factoryActive) Ok else Steel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Text(S.facDesc, color = Steel, fontSize = 12.sp, lineHeight = 18.sp)
            if (!vm.connected) Text(S.facNeedConnect, color = Gold, fontSize = 12.sp)
            PillButton(
                label = S.facExit, accent = Cyan, filled = true, enabled = true,
                modifier = Modifier.fillMaxWidth(), height = 52.dp, onClick = onExit
            )
        }
    }
}

// ---------- رسم صحنه (همان صحنه‌ی کد ESP روی نمایشگر 128x64) ----------

private fun DrawScope.drawFactoryScene(t: Long) {
    val u = size.width / 128f
    fun rect(x: Float, y: Float, w: Float, h: Float, c: Color = Ink) {
        drawRect(c, Offset(x * u, y * u), Size(w * u, h * u))
    }
    fun circle(cx: Float, cy: Float, r: Float, c: Color = Ink) {
        drawCircle(c, r * u, Offset(cx * u, cy * u))
    }
    fun tri(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
        val p = Path().apply {
            moveTo(x0 * u, y0 * u)
            lineTo(x1 * u, y1 * u)
            lineTo(x2 * u, y2 * u)
            close()
        }
        drawPath(p, Ink)
    }
    fun gear(cx: Float, cy: Float, r: Float, ang: Float, teeth: Int) {
        circle(cx, cy, r)
        for (k in 0 until teeth) {
            val a = ang + k * 6.2832f / teeth
            val c = cos(a)
            val s = sin(a)
            val bx = cx + c * (r - 1f)
            val by = cy + s * (r - 1f)
            val qx = -s * 2.8f
            val qy = c * 2.8f
            tri(bx + qx, by + qy, bx - qx, by - qy, cx + c * (r + 3f), cy + s * (r + 3f))
        }
        circle(cx, cy, r / 2f, Color.Black)
    }

    val g = 46f
    var r = 0f
    if (t > 300L) {
        r = ((t - 300L) / 1500f).coerceAtMost(1f)
        r = 1f - (1f - r) * (1f - r)
    }
    val h = Math.round(16f * r).toFloat()
    val top = g - h
    rect(24f, g, 80f, 1f)
    if (h > 1f) {
        rect(35f, top, 58f, 1f)
        rect(35f, top, 1f, h)
        rect(92f, top, 1f, h)
        if (r > 0.55f) for (i in 0 until 4) rect(40f + i * 11f, top + 3f, 7f, 4f)
        if (r > 0.8f) {
            rect(82f, g - 9f, 8f, 1f)
            rect(82f, g - 9f, 1f, 9f)
            rect(89f, g - 9f, 1f, 9f)
        }
    }
    if (r > 0.5f) {                                   // سقف دندانه‌ای
        val rf = 8f * (r - 0.5f) / 0.5f
        for (i in 0 until 3) tri(35f + i * 19f, top, 54f + i * 19f, top, 54f + i * 19f, top - rf)
    }
    if (r > 0.7f) {                                   // دودکش‌ها
        val cp = (r - 0.7f) / 0.3f
        rect(42f, 28f - 14f * cp, 6f, 14f * cp + 1f)
        rect(61f, 28f - 11f * cp, 6f, 11f * cp + 1f)
    }
    if (r > 0.92f) {                                  // دود
        for (ch in 0..1) {
            val cx = if (ch == 0) 45f else 64f
            val ty = if (ch == 0) 14f else 17f
            for (k in 0..3) {
                val ph = ((t / 45L + k * 18L + ch * 9L) % 72L).toInt()
                val y = ty - 2f - ph / 6f
                if (y < 1f) continue
                circle(cx + sin(ph / 9f + k * 1.7f) * 2.5f, y, 1f + (ph / 28))
            }
        }
    }
    gear(14f, 24f, 8f, t / 400f, 8)                   // چرخ‌دنده‌ها
    gear(114f, 37f, 6f, -(t / 260f), 6)

    rect(0f, 55f, 128f, 1f)                           // نوار نقاله
    val off = ((t / 30L) % 6L).toFloat()
    var x = off
    while (x < 128f) {
        rect(x, 56f, 1f, 1f)
        x += 6f
    }
    if (t > 1200L) {                                  // ماشین روی نوار نقاله
        val cx = (((t - 1200L) * 150L / 3000L) % 150L).toFloat() - 20f
        val y = 55f
        drawRoundRect(Ink, Offset(cx * u, (y - 9f) * u), Size(18f * u, 5f * u), CornerRadius(2f * u))
        drawRoundRect(Ink, Offset((cx + 4f) * u, (y - 13f) * u), Size(10f * u, 5f * u), CornerRadius(2f * u))
        rect(cx + 6f, y - 12f, 6f, 2f, Color.Black)
        circle(cx + 4f, y - 3f, 2f)
        circle(cx + 4f, y - 3f, 0.7f, Color.Black)
        circle(cx + 14f, y - 3f, 2f)
        circle(cx + 14f, y - 3f, 0.7f, Color.Black)
    }
    if (t < 600L) {                                   // اسکن اولیه‌ی صفحه
        val rev = t * 64f / 600f
        rect(0f, rev, 128f, 64f - rev, Color.Black)
        rect(0f, rev, 128f, 1f)
    }
}
