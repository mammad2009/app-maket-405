package com.mamad.peugeot405

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = CommandStore(app)
    private val bt = BluetoothClient(app)
    private val prefs = app.getSharedPreferences("bt", Context.MODE_PRIVATE)
    private val settings = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    // شبیه‌ساز کد آردوینو؛ نوار ارتفاع و کج شدن آرم از روی آن می‌آید
    val sim = SketchSim()

    var commands by mutableStateOf(store.load()); private set
    var devices by mutableStateOf<List<BluetoothDevice>>(emptyList()); private set
    var connected by mutableStateOf(false); private set
    var connecting by mutableStateOf(false); private set
    // وضعیت قفل ماکت از روی آخرین دکمه‌ی ریموت (null = هنوز چیزی نفرستاده‌ایم)
    var locked by mutableStateOf<Boolean?>(null); private set
    var statusType by mutableStateOf(St.IDLE); private set
    var statusArg by mutableStateOf(""); private set
    var btSupported by mutableStateOf(true); private set
    var btPermission by mutableStateOf(true); private set
    var btEnabled by mutableStateOf(true); private set
    var lastAddress by mutableStateOf(prefs.getString("last", "") ?: ""); private set

    // زبان برنامه: "fa" یا "en"
    var lang by mutableStateOf(settings.getString("lang", "fa") ?: "fa"); private set
    val strings: Strings get() = if (lang == "en") EnStrings else FaStrings

    // درصد باتری ماکت (فقط وقتی ESP عدد بفرستد؛ در کد ماکت ENABLE_BATTERY = 1 باشد)
    var battery by mutableStateOf<Int?>(null); private set

    // تب فعلی منو: 0 کنترل ، 1 کارخانه ، 2 فرمان‌ها
    var tab by mutableStateOf(0); private set
    // true = فرمان «ورود به حالت کارخانه» برای ماکت فرستاده شده و هنوز خارج نشده‌ایم
    var factoryActive by mutableStateOf(false); private set
    var showroomOn by mutableStateOf(false); private set

    private var factoryScreen = false     // الان تب کارخانه باز است؟
    private var foreground = true         // برنامه جلوی چشم کاربر است؟
    private var readJob: Job? = null

    // صف ارسال: بایت‌ها دقیقاً به ترتیب فشردن/رها کردن فرستاده می‌شوند
    private val outbox = Channel<String>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (t in outbox) {
                bt.send(t).onFailure { lostConnection(St.SEND_FAILED) }
            }
        }
        viewModelScope.launch {
            while (true) {
                sim.tick(SystemClock.uptimeMillis())
                delay(10)
            }
        }
        refreshDevices()
    }

    private fun setStatus(t: St, arg: String = "") {
        statusType = t
        statusArg = arg
    }

    private fun lostConnection(why: St) {
        readJob?.cancel()
        connected = false
        battery = null
        factoryActive = false
        showroomOn = false
        setStatus(why)
    }

    // ---------------- زبان ----------------
    fun setLang(l: String) {
        lang = if (l == "en") "en" else "fa"
        settings.edit().putString("lang", lang).apply()
    }

    // ---------------- اتصال ----------------
    fun refreshDevices() {
        btSupported = bt.isSupported
        btPermission = bt.hasPermission()
        btEnabled = bt.isEnabled
        devices = bt.pairedDevices().sortedByDescending { it.address == lastAddress }
    }

    private fun failStatus(e: Throwable) {
        val m = e.message ?: e.javaClass.simpleName
        when {
            e is SecurityException -> setStatus(St.FAIL_PERM)
            m.contains("read failed", true) || m.contains("timeout", true) || m.contains("closed", true) ->
                setStatus(St.FAIL_NORESP, m)
            else -> setStatus(St.FAIL_RAW, m)
        }
    }

    fun connect(d: BluetoothDevice) = viewModelScope.launch {
        if (connecting) return@launch
        connecting = true
        setStatus(St.CONNECTING)
        bt.connect(d)
            .onSuccess {
                connected = true
                locked = null
                setStatus(St.CONNECTED, d.name ?: d.address)
                lastAddress = d.address
                prefs.edit().putString("last", d.address).apply()
                startReader()
                // مطمئن شو ماکت از جلسه‌ی قبلی تو حالت کارخانه گیر نکرده باشد
                factoryActive = factoryScreen && foreground
                outbox.trySend(if (factoryActive) factoryOnCmd() else factoryOffCmd())
            }
            .onFailure {
                connected = false
                failStatus(it)
            }
        connecting = false
    }

    // اتصال سریع به آخرین دستگاه (یا تنها دستگاه جفت‌شده). اگر نشد false برمی‌گرداند تا لیست نمایش داده شود
    fun connectQuick(): Boolean {
        refreshDevices()
        if (!btSupported || !btPermission || !btEnabled) return false
        val d = devices.firstOrNull { it.address == lastAddress } ?: devices.singleOrNull() ?: return false
        connect(d)
        return true
    }

    fun disconnect() = viewModelScope.launch {
        if (connected && factoryActive) {      // قبل از قطع شدن، ماکت را از حالت کارخانه بیرون بیاور
            bt.send(factoryOffCmd())
            delay(150)
        }
        readJob?.cancel()
        bt.disconnect()
        connected = false
        battery = null
        factoryActive = false
        showroomOn = false
        setStatus(St.IDLE)
    }

    // خطوطی که ماکت می‌فرستد؛ مثلا BAT:87
    private fun startReader() {
        readJob?.cancel()
        readJob = viewModelScope.launch {
            bt.lines().collect { line ->
                if (line.startsWith("BAT:")) {
                    line.substring(4).trim().toIntOrNull()?.let { battery = it.coerceIn(0, 100) }
                }
            }
            // جریان تمام شد = اتصال قطع شده (مثلا ماژول خاموش شد)
            if (connected) lostConnection(St.IDLE)
        }
    }

    fun send(text: String) {
        if (!connected) { setStatus(St.NEED_CONNECT); return }
        outbox.trySend(text)
    }

    // کاراکتری که شبیه‌ساز باید ببیند (مطابق کد آردوینو، مستقل از فرمانی که کاربر ویرایش کرده)
    private fun canon(c: Cmd): Char = when (c.action) {
        "F_UP_S" -> 'F'
        "F_DN_S" -> 'B'
        "R_UP_S" -> 'R'
        "R_DN_S" -> 'L'
        "F_UP_F" -> '2'
        "F_DN_F" -> '1'
        "R_UP_F" -> 'Z'
        "R_DN_F" -> 'X'
        "M_ON" -> 'M'
        "M_OFF" -> 'N'
        "STOP" -> '0'
        else -> c.command.firstOrNull() ?: '\u0000'
    }

    // فشردن یک دکمه: هم فرمان می‌فرستد، هم همان را در شبیه‌ساز اجرا می‌کند (حتی بدون اتصال)
    fun press(c: Cmd) {
        send(c.command)
        sim.receive(canon(c))
    }

    // رها کردن دکمه‌ی حرکت آرام: فرمان توقف (در کد آردوینو هر کاراکتر دیگری حرکت را متوقف می‌کند)
    fun pressUp() {
        val stop = commands.firstOrNull { it.action == "STOP" }
        send(stop?.command ?: "0")
        sim.receive('0')
    }

    private fun cmdFor(action: String): Cmd? = commands.firstOrNull { it.action == action }

    // فرمان‌های چندبایتی (مثل «هر دو بالا»: هر بایت جدا و با فاصله‌ی کوتاه فرستاده می‌شود)
    fun pressMulti(c: Cmd) {
        viewModelScope.launch {
            for (ch in c.command) {
                send(ch.toString())
                sim.receive(ch)
                delay(40)
            }
        }
    }

    // ارتفاع فابریک = وضعیت استراحتِ سرووها (جلو 80، عقب 84). اگر میباخ روشن است با فرمان توقفش برمی‌گردد
    fun factory(c: Cmd) {
        if (sim.maybachOn) cmdFor("M_OFF")?.let { press(it) } else pressMulti(c)
    }

    // ---------------- ریموت پژو ----------------
    fun remoteLock() {
        send(cmdFor("LOCK")?.command ?: "K")
        if (connected) locked = true
    }

    fun remoteUnlock() {
        send(cmdFor("UNLOCK")?.command ?: "U")
        if (connected) locked = false
    }

    // ---------------- حالت کارخانه ----------------
    // وارد تب «کارخانه» که شدی فرمان ورود (Y) و وقتی از آن خارج شدی یا برنامه را بستی فرمان خروج (V) فرستاده می‌شود
    private fun factoryOnCmd(): String = cmdFor("FACTORY_ON")?.command ?: "Y"
    private fun factoryOffCmd(): String = cmdFor("FACTORY_OFF")?.command ?: "V"

    fun selectTab(i: Int) {
        tab = i
        factoryScreen = (i == 1)
        syncFactory()
    }

    fun setForeground(fg: Boolean) {
        foreground = fg
        syncFactory()
    }

    private fun syncFactory() {
        val want = factoryScreen && connected && foreground
        if (want == factoryActive) return
        factoryActive = want
        if (want) showroomOn = false           // ماکت موقع ورود به کارخانه نمایشگاهی را می‌بندد
        if (connected) outbox.trySend(if (want) factoryOnCmd() else factoryOffCmd())
    }

    // ---------------- حالت نمایشگاهی ----------------
    fun toggleShowroom() {
        if (!connected) { setStatus(St.NEED_CONNECT); return }
        if (showroomOn) {
            outbox.trySend(cmdFor("SHOW_OFF")?.command ?: "E")
            showroomOn = false
        } else {
            outbox.trySend(cmdFor("SHOW_ON")?.command ?: "S")
            showroomOn = true
        }
    }

    // ---------------- ویرایش فرمان‌ها ----------------
    fun upsert(c: Cmd) {
        val i = commands.indexOfFirst { it.id == c.id }
        commands = if (i >= 0) commands.toMutableList().also { it[i] = c } else commands + c
        store.save(commands)
    }

    fun delete(id: String) { commands = commands.filter { it.id != id }; store.save(commands) }

    fun resetDefaults() { store.reset(); commands = DEFAULT_COMMANDS }

    override fun onCleared() { bt.disconnect() }
}
