package com.mamad.peugeot405

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ======================= آیکن‌ها (رسم دستی، چون بسته‌ی آیکن کامل نداریم) =======================

fun DrawScope.drawBluetooth(c: Offset, s: Float, color: Color, strokeW: Float) {
    val p = Path().apply {
        moveTo(c.x - 0.5f * s, c.y - 0.5f * s)
        lineTo(c.x + 0.5f * s, c.y + 0.5f * s)
        lineTo(c.x, c.y + s)
        lineTo(c.x, c.y - s)
        lineTo(c.x + 0.5f * s, c.y - 0.5f * s)
        lineTo(c.x - 0.5f * s, c.y + 0.5f * s)
    }
    drawPath(p, color, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// قفل بسته یا باز (خطی، مثل آیکن روی ریموت)
fun DrawScope.drawPadlock(c: Offset, s: Float, color: Color, open: Boolean, strokeW: Float) {
    val r = 0.27f * s
    val bodyTop = c.y - 0.12f * s
    val bodyH = 0.78f * s
    val arcTop = c.y - 0.65f * s + (if (open) -0.16f * s else 0f)
    val left = c.x - r
    val right = c.x + r
    val shackle = Path().apply {
        moveTo(left, bodyTop)
        lineTo(left, arcTop + r)
        arcTo(Rect(left, arcTop, right, arcTop + 2f * r), 180f, 180f, false)
        lineTo(right, if (open) arcTop + r + 0.2f * s else bodyTop)
    }
    val st = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(shackle, color, style = st)
    drawRoundRect(
        color, topLeft = Offset(c.x - 0.5f * s, bodyTop), size = Size(s, bodyH),
        cornerRadius = CornerRadius(0.14f * s), style = Stroke(width = strokeW)
    )
    drawCircle(color, 0.07f * s, Offset(c.x, bodyTop + 0.30f * s))
    drawLine(color, Offset(c.x, bodyTop + 0.34f * s), Offset(c.x, bodyTop + 0.52f * s), strokeW, StrokeCap.Round)
}

// ======================= صفحه‌ی اتصال =======================

@Composable
fun ConnectScreen(vm: MainViewModel, onPick: () -> Unit, onSkip: () -> Unit) {
    val S = LocalStrings.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(350); shown = true }
    // ظاهر شدن با فنر؛ محو شدن بعد از اتصال موفق
    val appear by animateFloatAsState(
        if (shown) 1f else 0f, spring(dampingRatio = 0.62f, stiffness = 60f), label = "cAppear"
    )
    val hide by animateFloatAsState(
        if (vm.connected) 1f else 0f, tween(800, easing = FastOutSlowInEasing), label = "cHide"
    )
    val busy by animateFloatAsState(if (vm.connecting) 1f else 0f, tween(500), label = "cBusy")
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed) 0.94f else 1f, tween(100), label = "cPress")
    val inf = rememberInfiniteTransition(label = "cInf")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3800, easing = LinearEasing)), label = "cT")
    val sweep by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "cSweep")

    BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
        val centerShift = maxHeight * 0.05f

        // حلقه‌های هم‌مرکز و موج‌های رادار
        Canvas(Modifier.fillMaxSize()) {
            val vis = appear.coerceIn(0f, 1f) * (1f - hide)
            if (vis > 0.001f) {
                val c = Offset(size.width / 2f, size.height / 2f - centerShift.toPx())
                val base = 82.dp.toPx()
                val ph = t * 2f * PI.toFloat()
                val n = 26
                for (i in 0 until n) {
                    val f = i / (n - 1f)
                    val rad = base * 1.2f + f * size.width * 0.62f
                    val off = sin(ph + f * 6f) * f * 16.dp.toPx()
                    val a = (0.62f * (1f - f) + 0.07f) * vis * (0.55f + 0.45f * busy)
                    drawCircle(
                        lerp(Cyan, Color(0xFF0B3A66), f).copy(alpha = a), rad,
                        c + Offset(off, off * 0.45f), style = Stroke(0.8.dp.toPx())
                    )
                }
                for (k in 0..2) {
                    val p = (t + k / 3f) % 1f
                    drawCircle(
                        Cyan.copy(alpha = (1f - p) * 0.45f * vis * (0.35f + 0.65f * busy)),
                        base + p * size.width * 0.55f, c, style = Stroke(1.6.dp.toPx())
                    )
                }
                if (busy > 0.01f) {
                    drawArc(
                        Cyan.copy(alpha = busy * vis), sweep, 100f, false,
                        Offset(c.x - base, c.y - base), Size(base * 2f, base * 2f),
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // نام برنامه بالای صفحه
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
                .graphicsLayer { alpha = appear.coerceIn(0f, 1f) * (1f - hide) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("PEUGEOT", color = Silver, fontSize = 12.sp, letterSpacing = 8.sp, fontWeight = FontWeight.Medium)
            Text("405", color = Color.White, fontSize = 44.sp, letterSpacing = 6.sp, fontWeight = FontWeight.Black)
            Text("PNEUMATIC CONTROL", color = Steel, fontSize = 10.sp, letterSpacing = 4.sp)
        }

        // دکمه‌ی اتصال
        Box(
            Modifier.align(Alignment.Center).offset(y = -centerShift).size(150.dp)
                .graphicsLayer {
                    val sc = (0.55f + 0.45f * appear) * (1f + 0.5f * hide) * press *
                        (1f + 0.025f * sin(t * 2f * PI.toFloat()))
                    scaleX = sc
                    scaleY = sc
                    alpha = appear.coerceIn(0f, 1f) * (1f - hide)
                    translationY = (1f - appear) * 160.dp.toPx()
                }
                .clip(CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            try { tryAwaitRelease() } finally { pressed = false }
                        },
                        onTap = {
                            if (!vm.connecting && !vm.connected) {
                                if (!vm.connectQuick()) onPick()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFF0E202E), Color(0xFF04070A)), center = center, radius = r),
                    radius = r, center = center
                )
                drawCircle(
                    brush = Brush.sweepGradient(listOf(Cyan, Color(0xFF0B3A66), Cyan.copy(alpha = 0.2f), Cyan), center),
                    radius = r - 1.dp.toPx(), center = center, style = Stroke(2.dp.toPx())
                )
                drawCircle(Cyan.copy(alpha = 0.18f), r - 11.dp.toPx(), center, style = Stroke(1.dp.toPx()))
                drawBluetooth(center, 24.dp.toPx(), Cyan, 3.dp.toPx())
            }
        }

        // متن زیر دکمه
        val title = when {
            vm.connected -> S.cConnected
            vm.connecting -> S.cConnecting
            else -> S.cIdle
        }
        val sub = if (vm.statusType == St.IDLE) S.cSubIdle else S.status(vm.statusType, vm.statusArg)
        Column(
            Modifier.align(Alignment.Center).offset(y = -centerShift + 125.dp)
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = appear.coerceIn(0f, 1f) * (1f - hide)
                    translationY = (1f - appear) * 160.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(sub, color = Steel, fontSize = 12.sp)
        }

        // لینک‌های پایین صفحه
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp)
                .graphicsLayer { alpha = appear.coerceIn(0f, 1f) * (1f - hide) },
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                S.pickDevice, color = Steel, fontSize = 12.sp,
                modifier = Modifier.clickable { vm.refreshDevices(); onPick() }.padding(12.dp)
            )
            Text(
                S.demoMode, color = Steel, fontSize = 12.sp,
                modifier = Modifier.clickable { onSkip() }.padding(12.dp)
            )
        }
    }
}

// ======================= ریموت پژو =======================

private fun DrawScope.drawRemoteBody(locked: Boolean?, connected: Boolean) {
    val w = size.width
    val h = size.height
    val left = w * 0.05f
    val right = w * 0.95f
    val top = h * 0.01f
    val bottom = h * 0.80f
    val cr = CornerRadius(w * 0.20f)
    val glowCol = when (locked) {
        true -> Cyan
        false -> Gold
        null -> Steel
    }

    // هاله‌ی نرم دور بدنه
    for (i in 1..8) {
        val d = w * 0.007f * i
        drawRoundRect(
            glowCol.copy(alpha = 0.055f * (9 - i) / 8f),
            topLeft = Offset(left - d, top - d),
            size = Size(right - left + 2f * d, bottom - top + 2f * d),
            cornerRadius = CornerRadius(w * 0.20f + d),
            style = Stroke(w * 0.007f)
        )
    }

    // تیغه‌ی کروم پایین
    val chrome = Brush.horizontalGradient(
        listOf(Color(0xFF5A606C), Color(0xFFF1F4F9), Color(0xFF8A92A1), Color(0xFFE5E9F0), Color(0xFF4B515C)),
        startX = w * 0.30f, endX = w * 0.70f
    )
    drawRoundRect(chrome, Offset(w * 0.30f, h * 0.775f), Size(w * 0.40f, h * 0.21f), CornerRadius(w * 0.10f))
    drawRoundRect(Color(0xFF050608), Offset(w * 0.375f, h * 0.865f), Size(w * 0.25f, h * 0.095f), CornerRadius(w * 0.06f))

    // بدنه
    drawRoundRect(
        Brush.verticalGradient(
            listOf(Color(0xFF2B303A), Color(0xFF171A20), Color(0xFF0B0D11)),
            startY = top, endY = bottom
        ),
        Offset(left, top), Size(right - left, bottom - top), cr
    )
    // برق مورب روی بدنه
    drawRoundRect(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.11f), Color.Transparent),
            start = Offset(left, top), end = Offset(w * 0.65f, h * 0.38f)
        ),
        Offset(left, top), Size(right - left, bottom - top), cr
    )
    // لبه‌ی فلزی
    drawRoundRect(
        Brush.linearGradient(
            listOf(Color(0xFFEFF2F8), Color(0xFF4A5160), Color(0xFF14171C), Color(0xFF9AA3B5)),
            start = Offset(0f, 0f), end = Offset(w, h)
        ),
        Offset(left, top), Size(right - left, bottom - top), cr,
        style = Stroke(w * 0.012f)
    )
    drawRoundRect(
        Color.Black.copy(alpha = 0.55f),
        Offset(left + w * 0.018f, top + w * 0.018f),
        Size(right - left - w * 0.036f, bottom - top - w * 0.036f),
        CornerRadius(w * 0.18f), style = Stroke(w * 0.006f)
    )

    // درز وسط (جایی که ریموت تا می‌شود)
    val seamY = h * 0.527f
    drawLine(Color.Black.copy(alpha = 0.85f), Offset(left + w * 0.03f, seamY), Offset(right - w * 0.03f, seamY), w * 0.012f)
    drawLine(Color.White.copy(alpha = 0.13f), Offset(left + w * 0.03f, seamY + w * 0.012f), Offset(right - w * 0.03f, seamY + w * 0.012f), w * 0.006f)

    // کلاهک کروم بالا (محل حلقه‌ی کلید)
    val cx = w * 0.285f
    val cy = h * 0.078f
    val cr2 = w * 0.105f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White, Color(0xFFC3C9D4), Color(0xFF5E6573), Color(0xFF20242B)),
            center = Offset(cx - cr2 * 0.3f, cy - cr2 * 0.3f), radius = cr2 * 1.5f
        ),
        radius = cr2, center = Offset(cx, cy)
    )
    drawCircle(Color.Black.copy(alpha = 0.6f), cr2, Offset(cx, cy), style = Stroke(w * 0.010f))
    drawCircle(Color(0xFF15171C), cr2 * 0.28f, Offset(cx + cr2 * 0.1f, cy + cr2 * 0.15f))

    // چراغ وضعیت
    val ledCol = if (!connected) Color(0xFF8A3B3B) else when (locked) {
        true -> Cyan
        false -> Gold
        null -> Color(0xFF3A404C)
    }
    val lx = w * 0.78f
    val ly = h * 0.078f
    drawCircle(
        brush = Brush.radialGradient(listOf(ledCol.copy(alpha = 0.55f), Color.Transparent), center = Offset(lx, ly), radius = w * 0.09f),
        radius = w * 0.09f, center = Offset(lx, ly)
    )
    drawCircle(ledCol, w * 0.022f, Offset(lx, ly))
}

@Composable
private fun RemoteKey(
    open: Boolean, active: Boolean, accent: Color, label: String,
    onPress: () -> Unit, modifier: Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }
    val wave = remember { Animatable(1f) }
    val haptic = LocalHapticFeedback.current
    val onP by rememberUpdatedState(onPress)
    LaunchedEffect(tick) {
        if (tick > 0) {
            wave.snapTo(0f)
            wave.animateTo(1f, tween(800, easing = LinearOutSlowInEasing))
        }
    }
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(110), label = "rkPress")
    val act by animateFloatAsState(if (active) 1f else 0f, tween(450), label = "rkAct")
    Box(
        modifier
            .graphicsLayer {
                val s = 1f - 0.035f * press
                scaleX = s
                scaleY = s
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onP()
                    tick++
                    try {
                        tryAwaitRelease()
                    } finally {
                        pressed = false
                    }
                })
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = CornerRadius(size.width * 0.20f)
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF2A2F39), Color(0xFF12151A))), cornerRadius = r)
            drawRoundRect(accent.copy(alpha = 0.10f * act + 0.30f * press), cornerRadius = r)
            drawRoundRect(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.04f), Color.Black.copy(alpha = 0.7f)),
                    start = Offset.Zero, end = Offset(size.width, size.height)
                ),
                cornerRadius = r, style = Stroke(1.6.dp.toPx())
            )
            val glow = 0.55f * act + 0.45f * press
            if (glow > 0.01f) {
                drawRoundRect(accent.copy(alpha = glow), cornerRadius = r, style = Stroke(1.5.dp.toPx()))
                for (i in 1..4) {
                    val d = 2.5.dp.toPx() * i
                    drawRoundRect(
                        accent.copy(alpha = glow * 0.22f * (5 - i) / 4f),
                        topLeft = Offset(-d, -d), size = Size(size.width + 2f * d, size.height + 2f * d),
                        cornerRadius = CornerRadius(size.width * 0.20f + d), style = Stroke(2.5.dp.toPx())
                    )
                }
            }
            val wv = wave.value
            if (wv < 1f) {
                val d = wv * 26.dp.toPx()
                drawRoundRect(
                    accent.copy(alpha = (1f - wv) * 0.75f),
                    topLeft = Offset(-d, -d), size = Size(size.width + 2f * d, size.height + 2f * d),
                    cornerRadius = CornerRadius(size.width * 0.20f + d), style = Stroke(2.dp.toPx())
                )
            }
            val ic = lerp(Silver.copy(alpha = 0.92f), accent, (act * 0.7f + press).coerceIn(0f, 1f))
            drawPadlock(Offset(size.width / 2f, size.height * 0.45f), size.width * 0.34f, ic, open, 2.4.dp.toPx())
        }
        Text(
            label, color = Steel, fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
        )
    }
}

@Composable
fun PeugeotRemote(
    locked: Boolean?,
    connected: Boolean,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 220.dp
) {
    val S = LocalStrings.current
    val h = width * 2.1f
    Box(modifier.size(width, h)) {
        Canvas(Modifier.fillMaxSize()) { drawRemoteBody(locked, connected) }
        Image(
            painterResource(R.drawable.peugeot_lion), null,
            Modifier.align(Alignment.TopCenter).offset(y = h * 0.105f)
                .size(width * 0.32f, width * 0.32f * LION_RATIO),
            contentScale = ContentScale.Fit
        )
        // دکمه‌ی قفل (K)
        RemoteKey(
            open = false, active = locked == true, accent = Cyan, label = S.rLock, onPress = onLock,
            modifier = Modifier.offset(x = width * 0.20f, y = h * 0.27f).size(width * 0.60f, h * 0.235f)
        )
        // دکمه‌ی باز (U)
        RemoteKey(
            open = true, active = locked == false, accent = Gold, label = S.rUnlock, onPress = onUnlock,
            modifier = Modifier.offset(x = width * 0.20f, y = h * 0.555f).size(width * 0.60f, h * 0.215f)
        )
    }
}

// ======================= ظاهر شدن ریموت بعد از اتصال =======================

@Composable
fun RemoteIntro(vm: MainViewModel, onLeaving: () -> Unit, onDone: () -> Unit) {
    val S = LocalStrings.current
    // سمتی که دکمه‌ی ریموت منو در آن است (فارسی: چپ ، انگلیسی: راست)
    val sx = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    var appear by remember { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { delay(200); appear = true }
    val ap by animateFloatAsState(
        if (appear) 1f else 0f, spring(dampingRatio = 0.68f, stiffness = 55f), label = "rAppear"
    )
    val lp by animateFloatAsState(
        if (leave) 1f else 0f, tween(800, easing = FastOutSlowInEasing), label = "rLeave",
        finishedListener = { if (leave && it >= 0.999f) onDone() }
    )

    fun startLeave() {
        if (!leave) {
            leave = true
            onLeaving()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Bg.copy(alpha = 1f - lp))) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val rw = minOf(maxWidth * 0.62f, (maxHeight - 190.dp) / 2.1f, 235.dp)

        Canvas(Modifier.fillMaxSize()) {
            val a = ap.coerceIn(0f, 1f) * (1f - lp)
            drawRect(
                Brush.radialGradient(
                    listOf(Cyan.copy(alpha = 0.20f * a), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.46f), radius = size.width * 0.95f
                )
            )
        }

        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.graphicsLayer {
                    alpha = ap.coerceIn(0f, 1f) * (1f - lp)
                    val sc = (0.55f + 0.45f * ap) * (1f - 0.88f * lp)
                    scaleX = sc
                    scaleY = sc
                    translationY = (1f - ap) * 280.dp.toPx() - lp * hPx * 0.42f
                    translationX = sx * lp * wPx * 0.34f
                    rotationZ = (1f - ap) * -16f + lp * 28f
                }
            ) {
                PeugeotRemote(
                    locked = vm.locked, connected = vm.connected, width = rw,
                    onLock = { vm.remoteLock() },
                    onUnlock = {
                        vm.remoteUnlock()
                        // بعد از «باز»، ریموت کوچک می‌شود و می‌رود داخل منوی برنامه
                        if (vm.connected) scope.launch { delay(550); startLeave() }
                    }
                )
            }
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier.graphicsLayer { alpha = ap.coerceIn(0f, 1f) * (1f - lp) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(S.lockText(vm.locked), color = Silver, fontSize = 13.sp)
                Text(
                    S.enterMenu, color = Steel, fontSize = 12.sp,
                    modifier = Modifier.clickable { startLeave() }.padding(12.dp)
                )
            }
        }
    }
}

// ======================= ریموت داخل منوی برنامه =======================

@Composable
fun RemoteOverlay(vm: MainViewModel, visible: Boolean, onClose: () -> Unit) {
    val S = LocalStrings.current
    val sx = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val p by animateFloatAsState(
        if (visible) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = 140f), label = "ov"
    )
    if (!visible && p < 0.01f) return
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = p.coerceIn(0f, 1f) }
            .background(Color(0xEB000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
    ) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val rw = minOf(maxWidth * 0.62f, (maxHeight - 190.dp) / 2.1f, 235.dp)
        Column(
            Modifier.align(Alignment.Center)
                .graphicsLayer {
                    val sc = 0.25f + 0.75f * p
                    scaleX = sc
                    scaleY = sc
                    translationX = sx * (1f - p) * wPx * 0.30f
                    translationY = -(1f - p) * hPx * 0.40f
                }
                .pointerInput(Unit) { detectTapGestures { } },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PeugeotRemote(
                locked = vm.locked, connected = vm.connected, width = rw,
                onLock = { vm.remoteLock() }, onUnlock = { vm.remoteUnlock() }
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (vm.connected) (if (vm.locked == null) S.readyOverlay else S.lockText(vm.locked))
                else S.noConnection,
                color = if (vm.connected) Silver else Color(0xFFF87171), fontSize = 13.sp
            )
        }
        Box(
            Modifier.align(Alignment.TopStart).padding(16.dp).size(42.dp).clip(CircleShape)
                .background(Panel).clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }
    }
}
