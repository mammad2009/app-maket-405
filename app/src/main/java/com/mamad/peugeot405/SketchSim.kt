package com.mamad.peugeot405

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * شبیه‌ساز دقیق کد آردوینو (car_control2.ino).
 * همان منطق: input آخرین فرمان را نگه می‌دارد؛ حرکت آرام هر 40ms یک درجه؛
 * حرکت سریع یک‌باره؛ میباخ با همان بخش‌ها و ضرب آهنگ.
 */
class SketchSim {
    private var posF = 80
    private var posB = 84
    private var input = '\u0000'
    private var lastInput = '\u0000'
    private var lastMove = 0L

    private var maybachMode = false
    private var maybachReturn = false
    private var maybachStart = 0L
    private var maybachLast = 0L
    private val syncOffset = 0L

    private val queue = ArrayDeque<Char>()
    private var lastTick = 0L

    // مقدار نمایشی (نرم‌شده) سروو جلو و عقب، برای انیمیشن
    var dispF by mutableStateOf(80f); private set
    var dispB by mutableStateOf(84f); private set
    var maybachOn by mutableStateOf(false); private set

    private class Seg(val ms: Long, val bpc: Int, val a0: Int, val a1: Int, val mode: Int)

    private val segs = listOf(
        Seg(0, 4, 25, 40, 0), Seg(20400, 2, 60, 60, 1), Seg(41200, 2, 85, 85, 2),
        Seg(61300, 4, 55, 55, 0), Seg(81600, 1, 70, 70, 1), Seg(101400, 4, 30, 30, 0),
        Seg(121700, 8, 10, 10, 0), Seg(138000, 2, 15, 100, 2), Seg(142500, 2, 90, 90, 2),
        Seg(161500, 1, 75, 75, 1), Seg(180000, 4, 30, 30, 0), Seg(182000, 1, 90, 90, 1),
        Seg(202800, 2, 100, 100, 2), Seg(222700, 4, 40, 0, 0)
    )
    private val beatMs = 631.4f
    private val beatOrigin = 420f
    private val songEnd = 227500L
    private val cF = 96f
    private val aF = 16f
    private val cR = 104f
    private val aR = 16f
    private val restF = 80
    private val restB = 84

    /** دریافت یک کاراکتر (مثل بلوتوث) */
    fun receive(c: Char) { queue.addLast(c) }

    fun tick(now: Long) {
        // هر دور حلقه یک بایت خوانده می‌شود
        if (queue.isNotEmpty()) {
            input = queue.removeFirst()
            if (input == 'M' && !maybachMode) startMaybach(now)
            else if (input == 'N') { if (maybachMode) maybachReturn = true }
        }

        runMaybach(now)

        if (!maybachMode && now - lastMove >= 40) {
            lastMove = now
            when (input) {
                'F' -> if (posF < 112) posF++
                'B' -> if (posF > 80) posF--
                'R' -> if (posB < 115) posB++
                'L' -> if (posB > 84) posB--
            }
        }

        if (!maybachMode && input == '1' && lastInput != '1') posF = 80
        if (!maybachMode && input == '2' && lastInput != '2') posF = 112
        if (!maybachMode && input == 'X' && lastInput != 'X') posB = 84
        if (!maybachMode && input == 'Z' && lastInput != 'Z') posB = 115
        lastInput = input

        // نرم‌کردن برای نمایش (سرووی واقعی هم لحظه‌ای نمی‌رسد)
        val dt = if (lastTick == 0L) 0f else (now - lastTick).coerceIn(0L, 100L) / 1000f
        lastTick = now
        val k = 1f - exp(-dt / 0.11f)
        dispF += (posF - dispF) * k
        dispB += (posB - dispB) * k
    }

    private fun startMaybach(now: Long) {
        maybachMode = true
        maybachReturn = false
        maybachStart = now
        maybachLast = 0L
        maybachOn = true
    }

    private fun runMaybach(now: Long) {
        if (!maybachMode) return
        if (now - maybachLast < 15) return
        maybachLast = now

        val t = (now - maybachStart) + syncOffset
        if (!maybachReturn && t >= songEnd) maybachReturn = true

        val tgtF: Float
        val tgtB: Float
        val step: Int

        if (maybachReturn) {
            tgtF = restF.toFloat()
            tgtB = restB.toFloat()
            step = 1
            if (posF == restF && posB == restB) {
                maybachMode = false
                maybachReturn = false
                maybachOn = false
                return
            }
        } else {
            step = 5
            var i = 0
            while (i + 1 < segs.size && segs[i + 1].ms <= t) i++
            val seg = segs[i]
            val segEnd = if (i + 1 < segs.size) segs[i + 1].ms else songEnd
            var frac = (t - seg.ms).toFloat() / (segEnd - seg.ms).toFloat()
            if (frac < 0f) frac = 0f
            if (frac > 1f) frac = 1f
            val amp = (seg.a0 + (seg.a1 - seg.a0) * frac) / 100f
            val ph = 2f * PI.toFloat() * (t.toFloat() - beatOrigin) / (beatMs * seg.bpc)
            val c = cos(ph)
            val s = sin(ph)
            when (seg.mode) {
                0 -> { tgtF = cF - aF * amp * c; tgtB = cR + aR * amp * c }
                1 -> { tgtF = cF - aF * amp * c; tgtB = cR - aR * amp * c }
                else -> { tgtF = cF - aF * amp * c; tgtB = cR - aR * amp * s }
            }
        }

        var dF = (tgtF + 0.5f).toInt() - posF
        var dB = (tgtB + 0.5f).toInt() - posB
        if (dF > step) dF = step
        if (dF < -step) dF = -step
        if (dB > step) dB = step
        if (dB < -step) dB = -step

        posF += dF
        posB += dB
        if (posF < 80) posF = 80
        if (posF > 112) posF = 112
        if (posB < 78) posB = 78
        if (posB > 130) posB = 130
    }
}
