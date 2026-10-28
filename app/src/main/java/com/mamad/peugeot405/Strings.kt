package com.mamad.peugeot405

import androidx.compose.runtime.staticCompositionLocalOf

// وضعیت اتصال (متنش بر اساس زبان فعلی ساخته می‌شود)
enum class St { IDLE, CONNECTING, CONNECTED, FAIL_PERM, FAIL_NORESP, FAIL_RAW, SEND_FAILED, NEED_CONNECT }

/**
 * همه‌ی متن‌های برنامه. دو زبان: فارسی (FaStrings) و انگلیسی (EnStrings).
 * برای اضافه کردن یک متن جدید: اول اینجا abstract بنویس، بعد در هر دو آبجکت پایین override کن.
 */
abstract class Strings {
    abstract val lang: String

    // ---- عمومی ----
    abstract val appTitle: String
    abstract val appSubtitle: String
    abstract val navControl: String
    abstract val navFactory: String
    abstract val navCommands: String
    abstract val connectedChip: String
    abstract val connectChip: String
    abstract val batteryWord: String

    // ---- وضعیت اتصال ----
    abstract val stIdle: String
    abstract val stConnecting: String
    abstract val stSendFailed: String
    abstract val stNeedConnect: String
    abstract val errPerm: String
    abstract fun stConnected(name: String): String
    abstract fun stFailed(reason: String): String
    abstract fun errNoResponse(detail: String): String

    // ---- پنجره‌ی اتصال ----
    abstract val dlgTitle: String
    abstract val close: String
    abstract val disconnect: String
    abstract val noBluetooth: String
    abstract val needPermission: String
    abstract val grantPermission: String
    abstract val openAppSettings: String
    abstract val btOff: String
    abstract val turnOnBluetooth: String
    abstract val noPaired: String
    abstract val unnamed: String
    abstract val connectHint: String

    // ---- صفحه‌ی اتصال ----
    abstract val cConnected: String
    abstract val cConnecting: String
    abstract val cIdle: String
    abstract val cSubIdle: String
    abstract val pickDevice: String
    abstract val demoMode: String

    // ---- ریموت ----
    abstract val rLock: String
    abstract val rUnlock: String
    abstract val lockedText: String
    abstract val unlockedText: String
    abstract val readyIntro: String
    abstract val readyOverlay: String
    abstract val enterMenu: String
    abstract val noConnection: String

    // ---- کنترل ----
    abstract val front: String
    abstract val rear: String
    abstract val both: String
    abstract val maybach: String
    abstract val stop: String
    abstract val stock: String

    // ---- حالت نمایشگاهی ----
    abstract val showTitle: String
    abstract val showDesc: String
    abstract val showStart: String
    abstract val showStop: String
    abstract val showPlaying: String

    // ---- حالت کارخانه ----
    abstract val facTitle: String
    abstract val facActive: String
    abstract val facInactive: String
    abstract val facDesc: String
    abstract val facNeedConnect: String
    abstract val facExit: String

    // ---- تنظیمات و فرمان‌ها ----
    abstract val language: String
    abstract val sendCustom: String
    abstract val send: String
    abstract val newCommand: String
    abstract val defaults: String
    abstract val editCommand: String
    abstract val buttonName: String
    abstract val commandField: String
    abstract val groupField: String
    abstract val save: String
    abstract val cancel: String
    abstract fun sentCommand(c: String): String

    // ---------- توابع مشترک ----------
    fun status(t: St, arg: String): String = when (t) {
        St.IDLE -> stIdle
        St.CONNECTING -> stConnecting
        St.CONNECTED -> stConnected(arg)
        St.FAIL_PERM -> stFailed(errPerm)
        St.FAIL_NORESP -> stFailed(errNoResponse(arg))
        St.FAIL_RAW -> stFailed(arg)
        St.SEND_FAILED -> stSendFailed
        St.NEED_CONNECT -> stNeedConnect
    }

    fun lockText(locked: Boolean?): String = when (locked) {
        true -> lockedText
        false -> unlockedText
        null -> readyIntro
    }

    // اسم گروه‌های پیش‌فرض (گروه‌هایی که خودت می‌سازی همان‌طور که نوشتی می‌مانند)
    fun group(g: String): String {
        if (lang != "en") return g
        return when (g) {
            "جلو" -> "Front"
            "عقب" -> "Rear"
            "میباخ" -> "Maybach"
            "هر دو" -> "Both"
            "ریموت" -> "Remote"
            "سیستمی" -> "System"
            "سایر" -> "Other"
            "کارخانه" -> "Factory"
            "نمایشگاهی" -> "Showroom"
            else -> g
        }
    }

    // اسم دکمه‌های پیش‌فرض (اگه اسم دکمه رو خودت عوض کرده باشی همان می‌ماند)
    fun cmdLabel(c: Cmd): String {
        if (lang != "en") return c.label
        val def = DEFAULT_COMMANDS.firstOrNull { it.id == c.id }
        return if (def != null && def.label == c.label) enAction(c.action, c.label) else c.label
    }

    private fun enAction(a: String, fallback: String): String = when (a) {
        "F_UP_S", "R_UP_S" -> "Rise slow"
        "F_DN_S", "R_DN_S" -> "Lower slow"
        "F_UP_F", "R_UP_F" -> "Rise fast"
        "F_DN_F", "R_DN_F" -> "Lower fast"
        "M_ON" -> "Start Maybach"
        "M_OFF" -> "Stop"
        "BOTH_UP" -> "Both up"
        "BOTH_DN" -> "Both down"
        "FACTORY" -> "Stock height"
        "LOCK" -> "Lock"
        "UNLOCK" -> "Unlock"
        "STOP" -> "Release button (stop)"
        "FACTORY_ON" -> "Enter factory mode"
        "FACTORY_OFF" -> "Exit factory mode"
        "SHOW_ON" -> "Start showroom"
        "SHOW_OFF" -> "Stop showroom"
        else -> fallback
    }
}

object FaStrings : Strings() {
    override val lang = "fa"

    override val appTitle = "پژو ۴۰۵"
    override val appSubtitle = "کنترل تعلیق پنوماتیک"
    override val navControl = "کنترل"
    override val navFactory = "کارخانه"
    override val navCommands = "فرمان‌ها"
    override val connectedChip = "متصل"
    override val connectChip = "اتصال"
    override val batteryWord = "باتری"

    override val stIdle = "متصل نیست"
    override val stConnecting = "در حال اتصال..."
    override val stSendFailed = "ارسال ناموفق بود، اتصال قطع شد"
    override val stNeedConnect = "اول به HC-05 وصل شو"
    override val errPerm = "اجازه‌ی بلوتوث داده نشده"
    override fun stConnected(name: String) = "متصل به $name"
    override fun stFailed(reason: String) = "اتصال ناموفق: $reason"
    override fun errNoResponse(detail: String) =
        "HC-05 جواب نداد. روشن بودنش را چک کن و اینکه به گوشی دیگری وصل نباشد ($detail)"

    override val dlgTitle = "اتصال به HC-05"
    override val close = "بستن"
    override val disconnect = "قطع اتصال"
    override val noBluetooth = "این گوشی بلوتوث ندارد."
    override val needPermission = "برنامه اجازه‌ی «دستگاه‌های نزدیک» (بلوتوث) را ندارد."
    override val grantPermission = "دادن اجازه"
    override val openAppSettings = "باز کردن تنظیمات برنامه"
    override val btOff = "بلوتوث گوشی خاموش است."
    override val turnOnBluetooth = "روشن کردن بلوتوث"
    override val noPaired =
        "دستگاه جفت‌شده‌ای نیست. اول از تنظیمات بلوتوث گوشی، HC-05 را جفت کن (رمز 1234 یا 0000) و برگرد."
    override val unnamed = "بدون نام"
    override val connectHint = "اگر وصل نشد: چراغ HC-05 باید تند چشمک بزند و به گوشی دیگری وصل نباشد."

    override val cConnected = "متصل شد"
    override val cConnecting = "در حال اتصال..."
    override val cIdle = "اتصال به ماژول"
    override val cSubIdle = "بلوتوث ماژول را روشن کن و دکمه را لمس کن"
    override val pickDevice = "انتخاب دستگاه"
    override val demoMode = "حالت آزمایشی"

    override val rLock = "قفل"
    override val rUnlock = "باز"
    override val lockedText = "ماکت قفل شد"
    override val unlockedText = "ماکت باز شد"
    override val readyIntro = "ماژول متصل شد — ریموت آماده است"
    override val readyOverlay = "ریموت آماده است"
    override val enterMenu = "ورود به منو"
    override val noConnection = "اتصال برقرار نیست"

    override val front = "جلو"
    override val rear = "عقب"
    override val both = "هر دو"
    override val maybach = "میباخ"
    override val stop = "توقف"
    override val stock = "فابریک"

    override val showTitle = "حالت نمایشگاهی"
    override val showDesc = "نمایش پشت‌سرهم همه‌ی حالت‌های چشم، حرکت بدنه و چراغ‌ها؛ مثل رونمایی یک ماشین‌سازی"
    override val showStart = "شروع نمایش"
    override val showStop = "توقف نمایش"
    override val showPlaying = "در حال نمایش..."

    override val facTitle = "حالت کارخانه"
    override val facActive = "فعال — ماشین کف‌خواب است"
    override val facInactive = "غیرفعال — ماژول وصل نیست"
    override val facDesc =
        "با ورود به این بخش، ماشین کف‌خواب می‌شود و روی نمایشگرش کارخانه نشان داده می‌شود. " +
            "با خروج از این بخش (یا بستن برنامه) ماشین با یک افکت بامزه بیدار می‌شود و به کار عادی برمی‌گردد."
    override val facNeedConnect = "برای ارسال دستور، اول به ماژول وصل شو"
    override val facExit = "خروج از حالت کارخانه"

    override val language = "زبان"
    override val sendCustom = "ارسال فرمان دلخواه"
    override val send = "ارسال"
    override val newCommand = "فرمان جدید"
    override val defaults = "پیش‌فرض"
    override val editCommand = "ویرایش فرمان"
    override val buttonName = "نام دکمه"
    override val commandField = "فرمان ارسالی"
    override val groupField = "اسم گروه (جدید بنویس یا انتخاب کن)"
    override val save = "ذخیره"
    override val cancel = "لغو"
    override fun sentCommand(c: String) = "فرمان ارسالی: $c"
}

object EnStrings : Strings() {
    override val lang = "en"

    override val appTitle = "Peugeot 405"
    override val appSubtitle = "Pneumatic suspension control"
    override val navControl = "Control"
    override val navFactory = "Factory"
    override val navCommands = "Commands"
    override val connectedChip = "Connected"
    override val connectChip = "Connect"
    override val batteryWord = "Battery"

    override val stIdle = "Not connected"
    override val stConnecting = "Connecting..."
    override val stSendFailed = "Send failed, connection lost"
    override val stNeedConnect = "Connect to the HC-05 first"
    override val errPerm = "Bluetooth permission not granted"
    override fun stConnected(name: String) = "Connected to $name"
    override fun stFailed(reason: String) = "Connection failed: $reason"
    override fun errNoResponse(detail: String) =
        "The HC-05 did not respond. Check that it is powered and not connected to another phone ($detail)"

    override val dlgTitle = "Connect to HC-05"
    override val close = "Close"
    override val disconnect = "Disconnect"
    override val noBluetooth = "This phone has no Bluetooth."
    override val needPermission = "The app doesn't have the \u201CNearby devices\u201D (Bluetooth) permission."
    override val grantPermission = "Grant permission"
    override val openAppSettings = "Open app settings"
    override val btOff = "Bluetooth is turned off."
    override val turnOnBluetooth = "Turn on Bluetooth"
    override val noPaired =
        "No paired devices. Pair the HC-05 in your phone's Bluetooth settings first (PIN 1234 or 0000), then come back."
    override val unnamed = "Unnamed"
    override val connectHint =
        "If it fails: the HC-05 LED should blink fast and it must not be connected to another phone."

    override val cConnected = "Connected"
    override val cConnecting = "Connecting..."
    override val cIdle = "Connect to module"
    override val cSubIdle = "Power on the module and tap the button"
    override val pickDevice = "Choose device"
    override val demoMode = "Demo mode"

    override val rLock = "Lock"
    override val rUnlock = "Unlock"
    override val lockedText = "Model locked"
    override val unlockedText = "Model unlocked"
    override val readyIntro = "Module connected \u2014 remote ready"
    override val readyOverlay = "Remote ready"
    override val enterMenu = "Enter menu"
    override val noConnection = "Not connected"

    override val front = "Front"
    override val rear = "Rear"
    override val both = "Both"
    override val maybach = "Maybach"
    override val stop = "Stop"
    override val stock = "Stock"

    override val showTitle = "Showroom mode"
    override val showDesc = "Plays every eye mood, body motion and light show in sequence, like a car unveiling"
    override val showStart = "Start show"
    override val showStop = "Stop show"
    override val showPlaying = "Showing..."

    override val facTitle = "Factory mode"
    override val facActive = "Active \u2014 the car is lying flat"
    override val facInactive = "Inactive \u2014 module not connected"
    override val facDesc =
        "Entering this section makes the car lie flat and shows a factory scene on its display. " +
            "Leaving it (or closing the app) wakes the car up with a cute effect and returns it to normal."
    override val facNeedConnect = "Connect to the module first to send the command"
    override val facExit = "Exit factory mode"

    override val language = "Language"
    override val sendCustom = "Send custom command"
    override val send = "Send"
    override val newCommand = "New command"
    override val defaults = "Defaults"
    override val editCommand = "Edit command"
    override val buttonName = "Button name"
    override val commandField = "Command to send"
    override val groupField = "Group name (type a new one or pick)"
    override val save = "Save"
    override val cancel = "Cancel"
    override fun sentCommand(c: String) = "Sends: $c"
}

val LocalStrings = staticCompositionLocalOf<Strings> { FaStrings }
