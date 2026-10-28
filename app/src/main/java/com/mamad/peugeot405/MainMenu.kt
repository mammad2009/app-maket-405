package com.mamad.peugeot405

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val Ink = Color(0xFF07080B)

// ---------- دکمه‌ی پایه: فشردن = onDown ، رها کردن = onUp ----------

@Composable
fun PressBox(
    modifier: Modifier,
    accent: Color,
    filled: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(50),
    onDown: () -> Unit = {},
    onUp: () -> Unit = {},
    content: @Composable BoxScope.(Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(90), label = "pressScale")
    val top = when {
        pressed -> 0.55f
        filled -> 0.92f
        else -> 0.16f
    }
    val bottom = when {
        pressed -> 0.30f
        filled -> 0.62f
        else -> 0.04f
    }
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = top), accent.copy(alpha = bottom))))
            .border(1.dp, accent.copy(alpha = if (pressed) 1f else 0.55f), shape)
            .pointerInput(enabled) {
                detectTapGestures(onPress = {
                    if (enabled) {
                        pressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        down()
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                            up()
                        }
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        content(pressed)
    }
}

@Composable
fun Chevrons(up: Boolean, count: Int, tint: Color, size: Dp = 30.dp) {
    Canvas(Modifier.size(size, size)) {
        val w = this.size.width
        val h = this.size.height
        val gap = h * 0.30f
        val dy = h * 0.17f
        val startY = h / 2f - gap * (count - 1) / 2f
        for (i in 0 until count) {
            val cy = startY + i * gap
            val path = Path().apply {
                if (up) {
                    moveTo(w * 0.18f, cy + dy); lineTo(w * 0.5f, cy - dy); lineTo(w * 0.82f, cy + dy)
                } else {
                    moveTo(w * 0.18f, cy - dy); lineTo(w * 0.5f, cy + dy); lineTo(w * 0.82f, cy - dy)
                }
            }
            drawPath(
                path, tint,
                style = Stroke(width = h * 0.105f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

// ---------- هدر ----------

@Composable
fun Header(vm: MainViewModel, onConnect: () -> Unit, onRemote: () -> Unit) {
    val S = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(S.appTitle, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White, maxLines = 1)
                Text(S.appSubtitle, fontSize = 11.sp, color = Steel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // تغییر سریع زبان
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Panel)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { vm.setLang(if (vm.lang == "fa") "en" else "fa") },
                contentAlignment = Alignment.Center
            ) {
                Text(if (vm.lang == "fa") "EN" else "فا", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Silver)
            }
            Spacer(Modifier.width(8.dp))
            // دکمه‌ی ریموت: ریموت دوباره از اینجا باز می‌شود
            val tint = when (vm.locked) {
                true -> Cyan
                false -> Gold
                null -> Steel
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Panel)
                    .border(1.dp, tint.copy(alpha = 0.6f), CircleShape)
                    .clickable { onRemote() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(22.dp)) {
                    drawPadlock(Offset(size.width / 2f, size.height / 2f), size.width * 0.62f, tint, vm.locked == false, 1.8.dp.toPx())
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Panel)
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(50))
                    .clickable { onConnect() }.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (vm.connected) Ok else Danger))
                Spacer(Modifier.width(7.dp))
                Text(if (vm.connected) S.connectedChip else S.connectChip, fontSize = 12.sp, color = Color.White, maxLines = 1)
            }
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                S.status(vm.statusType, vm.statusArg), fontSize = 11.sp, color = Color(0xFF6B7385),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
            )
            vm.battery?.let { b ->
                Spacer(Modifier.width(10.dp))
                BatteryChip(b)
            }
        }
    }
}

// آیکون و درصد باتری ماکت (وقتی ESP درصد بفرستد)
@Composable
fun BatteryChip(percent: Int) {
    val col = when {
        percent <= 20 -> Danger
        percent <= 40 -> Gold
        else -> Ok
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 20.dp, height = 10.dp)) {
            val w = size.width
            val h = size.height
            val nub = 2.dp.toPx()
            val pad = 2.dp.toPx()
            drawRoundRect(col.copy(alpha = 0.8f), Offset.Zero, Size(w - nub, h), CornerRadius(2.dp.toPx()), style = Stroke(1.2.dp.toPx()))
            drawRoundRect(col, Offset(pad, pad), Size((w - nub - 2f * pad) * percent / 100f, h - 2f * pad), CornerRadius(1.dp.toPx()))
            drawRoundRect(col.copy(alpha = 0.8f), Offset(w - nub, h * 0.3f), Size(nub, h * 0.4f), CornerRadius(1.dp.toPx()))
        }
        Spacer(Modifier.width(5.dp))
        Text("$percent%", fontSize = 11.sp, color = col, fontWeight = FontWeight.SemiBold)
    }
}

// ---------- صفحه‌ی کنترل ----------

@Composable
fun ControlScreen(vm: MainViewModel) {
    val S = LocalStrings.current
    val cmds = vm.commands
    fun find(a: String): Cmd? = cmds.firstOrNull { it.action == a }
    val groups = cmds.filter { it.action == "NONE" }.groupBy { it.group }
    // درصد فقط وقتی عدد صحیحش عوض شود باعث ترکیب مجدد می‌شود
    val pctF by remember { derivedStateOf { (((vm.sim.dispF - 80f) / 32f) * 100f).roundToInt().coerceIn(0, 100) } }
    val pctB by remember { derivedStateOf { (((vm.sim.dispB - 84f) / 31f) * 100f).roundToInt().coerceIn(0, 100) } }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { LogoStage({ vm.sim.dispF }, { vm.sim.dispB }, vm.sim.maybachOn) }
            item {
                // چیدمان راست به چپ: جلو (سمت راست مثل ماشین)، عقب، و ستون «هر دو» سمت چپ
                Row(
                    Modifier.fillMaxWidth().height(392.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AxleCapsule(
                        S.front, "FRONT", Cyan,
                        find("F_UP_S"), find("F_UP_F"), find("F_DN_S"), find("F_DN_F"),
                        { (vm.sim.dispF - 80f) / 32f }, pctF,
                        vm::press, vm::press, vm::pressUp,
                        Modifier.weight(1f).fillMaxHeight()
                    )
                    AxleCapsule(
                        S.rear, "REAR", Violet,
                        find("R_UP_S"), find("R_UP_F"), find("R_DN_S"), find("R_DN_F"),
                        { (vm.sim.dispB - 84f) / 31f }, pctB,
                        vm::press, vm::press, vm::pressUp,
                        Modifier.weight(1f).fillMaxHeight()
                    )
                    AllCapsule(
                        find("BOTH_UP"), find("BOTH_DN"), find("FACTORY"),
                        find("M_ON"), find("M_OFF"), vm.sim.maybachOn,
                        vm::pressMulti, vm::factory, vm::press,
                        Modifier.weight(0.9f).fillMaxHeight()
                    )
                }
            }
            item { ShowroomCard(vm) }
            groups.forEach { (name, list) ->
                item(key = "group_$name") { GroupCard(S.group(name), list, vm::press) }
            }
        }
    }
}

// ---------- کپسول یک محور (جلو یا عقب) ----------

@Composable
private fun AxleButton(
    cmd: Cmd?, accent: Color, up: Boolean, fast: Boolean,
    onPress: (Cmd) -> Unit, onHoldStart: (Cmd) -> Unit, onHoldEnd: () -> Unit,
    modifier: Modifier
) {
    val S = LocalStrings.current
    PressBox(
        modifier = modifier, accent = accent, filled = fast, enabled = cmd != null,
        // سریع: همان لحظه‌ی لمس یک‌بار فرمان می‌رود؛ آرام: تا وقتی دست روی دکمه است حرکت می‌کند
        onDown = { cmd?.let { if (fast) onPress(it) else onHoldStart(it) } },
        onUp = { if (!fast) onHoldEnd() }
    ) { _ ->
        val fg = if (fast) Ink else accent
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Chevrons(up, if (fast) 2 else 1, fg, 24.dp)
            Spacer(Modifier.width(3.dp))
            Text(
                cmd?.let { S.cmdLabel(it) } ?: "—", color = fg, fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false
            )
        }
    }
}

// نوار عمودی ارتفاع فعلی + درصد (نوار فقط دوباره‌کشی می‌شود، نه ترکیب مجدد)
@Composable
private fun VerticalLevel(level: () -> Float, percent: Int, accent: Color, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val trackW = 10.dp.toPx()
            val x = size.width / 2f - trackW / 2f
            val r = CornerRadius(trackW / 2f)
            drawRoundRect(accent.copy(alpha = 0.14f), Offset(x, 0f), Size(trackW, size.height), r)
            val fh = size.height * level().coerceIn(0f, 1f)
            if (fh > 0f) {
                drawRoundRect(
                    Brush.verticalGradient(
                        listOf(accent, accent.copy(alpha = 0.40f)),
                        startY = size.height - fh, endY = size.height
                    ),
                    Offset(x, size.height - fh), Size(trackW, fh), r
                )
            }
            for (i in 0..4) {
                val y = size.height * i / 4f
                drawLine(accent.copy(alpha = 0.28f), Offset(x - 9.dp.toPx(), y), Offset(x - 3.dp.toPx(), y), 1.dp.toPx())
                drawLine(accent.copy(alpha = 0.28f), Offset(x + trackW + 3.dp.toPx(), y), Offset(x + trackW + 9.dp.toPx(), y), 1.dp.toPx())
            }
        }
        Text(
            "$percent%", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Bg.copy(alpha = 0.8f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun AxleCapsule(
    title: String, tag: String, accent: Color,
    upSlow: Cmd?, upFast: Cmd?, downSlow: Cmd?, downFast: Cmd?,
    level: () -> Float, percent: Int,
    onPress: (Cmd) -> Unit, onHoldStart: (Cmd) -> Unit, onHoldEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(36.dp)
    Column(
        modifier.clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF15181E), Color(0xFF08090C))))
            .border(1.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.75f), accent.copy(alpha = 0.10f))), shape)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        Text(tag, fontSize = 9.sp, letterSpacing = 2.sp, color = accent.copy(alpha = 0.75f))
        val bm = Modifier.fillMaxWidth().height(50.dp)
        AxleButton(upFast, accent, true, true, onPress, onHoldStart, onHoldEnd, bm)
        AxleButton(upSlow, accent, true, false, onPress, onHoldStart, onHoldEnd, bm)
        VerticalLevel(level, percent, accent, Modifier.weight(1f).fillMaxWidth())
        AxleButton(downSlow, accent, false, false, onPress, onHoldStart, onHoldEnd, bm)
        AxleButton(downFast, accent, false, true, onPress, onHoldStart, onHoldEnd, bm)
    }
}

// ---------- ستون «هر دو»: بالا/پایین همزمان، میباخ، فابریک ----------

@Composable
fun AllCapsule(
    bothUp: Cmd?, bothDown: Cmd?, factory: Cmd?, mOn: Cmd?, mOff: Cmd?, maybachOn: Boolean,
    onMulti: (Cmd) -> Unit, onFactory: (Cmd) -> Unit, onPress: (Cmd) -> Unit,
    modifier: Modifier = Modifier
) {
    val S = LocalStrings.current
    val shape = RoundedCornerShape(36.dp)
    Column(
        modifier.clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF16181D), Color(0xFF08090C))))
            .border(1.dp, Brush.verticalGradient(listOf(Silver.copy(alpha = 0.6f), Silver.copy(alpha = 0.08f))), shape)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(S.both, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text("SYNC", fontSize = 9.sp, letterSpacing = 2.sp, color = Silver.copy(alpha = 0.7f))
        }
        PressBox(
            Modifier.fillMaxWidth().height(70.dp), Silver, filled = true, enabled = bothUp != null,
            onDown = { bothUp?.let(onMulti) }
        ) { _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Chevrons(true, 2, Ink, 28.dp)
                Text(bothUp?.let { S.cmdLabel(it) } ?: "—", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
        // دکمه‌ی M (میباخ): شروع، و وقتی فعال است توقف
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PressBox(
                Modifier.size(70.dp), Gold, filled = maybachOn, shape = CircleShape,
                enabled = mOn != null,
                onDown = { if (maybachOn) mOff?.let(onPress) else mOn?.let(onPress) }
            ) { _ ->
                Text("M", color = if (maybachOn) Ink else Gold, fontWeight = FontWeight.Black, fontSize = 28.sp)
            }
            Text(
                if (maybachOn) S.stop else S.maybach, fontSize = 10.sp,
                color = Gold.copy(alpha = 0.85f), modifier = Modifier.padding(top = 3.dp)
            )
        }
        PressBox(
            Modifier.fillMaxWidth().height(70.dp), Silver, filled = true, enabled = bothDown != null,
            onDown = { bothDown?.let(onMulti) }
        ) { _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Chevrons(false, 2, Ink, 28.dp)
                Text(bothDown?.let { S.cmdLabel(it) } ?: "—", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
        PressBox(
            Modifier.fillMaxWidth().height(44.dp), Silver, enabled = factory != null,
            onDown = { factory?.let(onFactory) }
        ) { _ ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, tint = Silver, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(S.stock, color = Silver, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        }
    }
}

// ---------- حالت نمایشگاهی: شروع / توقف ----------

@Composable
fun ShowroomCard(vm: MainViewModel) {
    val S = LocalStrings.current
    CardBox(Gold, active = vm.showroomOn) {
        CardTitle(S.showTitle, "SHOWROOM", Gold)
        Text(S.showDesc, color = Steel, fontSize = 12.sp)
        if (vm.showroomOn) Text(S.showPlaying, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        PillButton(
            label = if (vm.showroomOn) S.showStop else S.showStart,
            accent = Gold, filled = !vm.showroomOn, enabled = true,
            modifier = Modifier.fillMaxWidth(), height = 52.dp,
            onClick = { vm.toggleShowroom() }
        )
    }
}
