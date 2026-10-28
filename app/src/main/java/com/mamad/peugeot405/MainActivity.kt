package com.mamad.peugeot405

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Grey = Color(0xFF8B93A5)

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshDevices() }
    private val enableLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { vm.refreshDevices() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBtPermission()
        setContent {
            // زبان فارسی = راست‌به‌چپ ، انگلیسی = چپ‌به‌راست
            val dir = if (vm.lang == "en") LayoutDirection.Ltr else LayoutDirection.Rtl
            CompositionLocalProvider(LocalLayoutDirection provides dir, LocalStrings provides vm.strings) {
                MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Bg, surface = Panel)) {
                    AppRoot(vm, this@MainActivity::requestBtPermission, this@MainActivity::enableBluetooth, this@MainActivity::openAppSettings)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshDevices()
    }

    // وقتی برنامه از جلوی چشم رفت (یا بسته شد) ماکت از حالت کارخانه بیرون می‌آید؛ برگشتی دوباره وارد می‌شود
    override fun onStart() {
        super.onStart()
        vm.setForeground(true)
    }

    override fun onStop() {
        super.onStop()
        vm.setForeground(false)
    }

    private fun requestBtPermission() {
        if (Build.VERSION.SDK_INT >= 31) permLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    private fun enableBluetooth() {
        enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
    }
}


// مراحل ورود: -1 = صفحه‌ی شروع (آرم) ، 0 = دکمه‌ی اتصال ، 1 = ریموت پژو ، 2 = منوی اصلی
@Composable
fun AppRoot(
    vm: MainViewModel,
    onRequestPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var phase by rememberSaveable { mutableStateOf(if (vm.connected) 2 else -1) }
    var showConnect by remember { mutableStateOf(false) }
    var showRemote by remember { mutableStateOf(false) }
    var transition by remember { mutableStateOf(false) }

    // بعد از اتصال موفق: دکمه محو می‌شود و بعدش ریموت ظاهر می‌شود
    LaunchedEffect(vm.connected) {
        if (vm.connected && phase == 0) {
            delay(1050)
            if (phase == 0) phase = 1
        }
    }

    Backdrop(Modifier.fillMaxSize()) {
        if (phase == 2 || transition) {
            MenuLayer(
                vm,
                { vm.refreshDevices(); showConnect = true },
                { showRemote = true }
            )
        }
        if (phase == -1) {
            SplashScreen(onFinished = { phase = 0 })
        }
        if (phase == 0) {
            ConnectScreen(vm, onPick = { vm.refreshDevices(); showConnect = true }, onSkip = { phase = 2 })
        }
        if (phase == 1) {
            RemoteIntro(vm, onLeaving = { transition = true }, onDone = { phase = 2; transition = false })
        }
        RemoteOverlay(vm, showRemote) { showRemote = false }
    }
    if (showConnect) {
        ConnectDialog(vm, { showConnect = false }, onRequestPermission, onEnableBluetooth, onOpenSettings)
    }
}

@Composable
fun MenuLayer(
    vm: MainViewModel,
    onConnect: () -> Unit,
    onRemote: () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(700), label = "menuIn")
    Scaffold(
        modifier = Modifier.graphicsLayer { alpha = a },
        containerColor = Color.Transparent,
        contentColor = Color.White,
        bottomBar = { FloatingNav(vm.tab, vm::selectTab) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Header(vm, onConnect, onRemote)
            when (vm.tab) {
                0 -> ControlScreen(vm)
                1 -> FactoryScreen(vm, onExit = { vm.selectTab(0) })
                else -> SettingsScreen(vm)
            }
        }
    }
}

// ---------- نوار پایین شناور و مدرن ----------

@Composable
fun FloatingNav(tab: Int, onTab: (Int) -> Unit) {
    val S = LocalStrings.current
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(shape).background(Panel.copy(alpha = 0.94f))
            .border(1.dp, Color(0x22FFFFFF), shape).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NavItem(tab == 0, S.navControl, Cyan, Modifier.weight(1f), { onTab(0) }) { tint ->
            Icon(Icons.Default.Home, null, tint = tint)
        }
        NavItem(tab == 1, S.navFactory, Gold, Modifier.weight(1f), { onTab(1) }) { tint ->
            Canvas(Modifier.size(24.dp)) { drawGearIcon(tint) }
        }
        NavItem(tab == 2, S.navCommands, Violet, Modifier.weight(1f), { onTab(2) }) { tint ->
            Icon(Icons.Default.Settings, null, tint = tint)
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean, label: String, accent: Color, modifier: Modifier,
    onClick: () -> Unit, icon: @Composable (Color) -> Unit
) {
    val a by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "nav")
    val tint = lerp(Grey, accent, a)
    Column(
        modifier.clip(RoundedCornerShape(22.dp))
            .background(accent.copy(alpha = 0.15f * a))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon(tint)
        Text(label, color = tint, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

// آیکن چرخ‌دنده (برای تب کارخانه)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGearIcon(color: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.30f
    val sw = size.minDimension * 0.10f
    drawCircle(color, r, c, style = Stroke(sw))
    drawCircle(color, r * 0.35f, c, style = Stroke(sw))
    for (k in 0 until 8) {
        val a = k * 0.7853982f
        val dx = kotlin.math.cos(a)
        val dy = kotlin.math.sin(a)
        drawLine(
            color, Offset(c.x + dx * r * 1.1f, c.y + dy * r * 1.1f),
            Offset(c.x + dx * r * 1.6f, c.y + dy * r * 1.6f), sw * 1.3f, StrokeCap.Round
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ConnectDialog(
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val S = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(S.close) } },
        dismissButton = {
            if (vm.connected) TextButton(onClick = { vm.disconnect(); onDismiss() }) { Text(S.disconnect) }
        },
        title = { Text(S.dlgTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    !vm.btSupported -> Text(S.noBluetooth)
                    !vm.btPermission -> {
                        Text(S.needPermission)
                        Button(onClick = onRequestPermission) { Text(S.grantPermission) }
                        TextButton(onClick = onOpenSettings) { Text(S.openAppSettings) }
                    }
                    !vm.btEnabled -> {
                        Text(S.btOff)
                        Button(onClick = onEnableBluetooth) { Text(S.turnOnBluetooth) }
                    }
                    vm.devices.isEmpty() -> Text(S.noPaired)
                    else -> {
                        LazyColumn(Modifier.heightIn(max = 260.dp)) {
                            items(vm.devices) { d ->
                                TextButton(onClick = { vm.connect(d); onDismiss() }) {
                                    Text("${d.name ?: S.unnamed}  (${d.address})")
                                }
                            }
                        }
                        Text(S.connectHint, fontSize = 11.sp, color = Grey)
                    }
                }
                Text(S.status(vm.statusType, vm.statusArg), fontSize = 11.sp, color = Grey)
            }
        }
    )
}

@Composable
fun CardBox(accent: Color, active: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Panel)
            .border(1.dp, accent.copy(alpha = if (active) 0.9f else 0.25f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun CardTitle(title: String, tag: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Spacer(Modifier.weight(1f))
        Text(tag, fontSize = 11.sp, color = accent.copy(alpha = 0.7f))
    }
}

@Composable
fun PillButton(
    label: String,
    accent: Color,
    filled: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    onDown: () -> Unit = {},
    onUp: () -> Unit = {},
    onClick: () -> Unit = {},
    leading: @Composable () -> Unit = {}
) {
    val shape = RoundedCornerShape(18.dp)
    var pressed by remember { mutableStateOf(false) }
    val fg = if (filled) Color(0xFF0B0D12) else accent
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    val click by rememberUpdatedState(onClick)
    val bg = if (filled) accent.copy(alpha = if (pressed) 0.72f else 1f)
             else accent.copy(alpha = if (pressed) 0.38f else 0.14f)
    Row(
        modifier.height(height).clip(shape).background(bg)
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .pointerInput(enabled) {
                detectTapGestures(onPress = {
                    if (enabled) {
                        pressed = true
                        down()
                        var released = false
                        try {
                            released = tryAwaitRelease()
                        } finally {
                            pressed = false
                            up()
                        }
                        if (released) click()
                    }
                })
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        leading()
        Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
fun ExtraButtons(extras: List<Cmd>, accent: Color, onPress: (Cmd) -> Unit) {
    extras.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { c ->
                PillButton(
                    label = c.label, accent = accent, filled = false, enabled = true,
                    modifier = Modifier.weight(1f), height = 46.dp, onClick = { onPress(c) }
                )
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

// هر گروه دلخواه یک کارت جدا با اسم خودش
@Composable
fun GroupCard(name: String, list: List<Cmd>, onPress: (Cmd) -> Unit) {
    val accent = Color(0xFF7DD3C0)
    CardBox(accent) {
        CardTitle(name, "CUSTOM", accent)
        ExtraButtons(list, accent, onPress)
    }
}

@Composable
private fun LangChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.clip(shape)
            .background(if (selected) Cyan.copy(alpha = 0.22f) else Color(0xFF1A2230))
            .border(1.dp, if (selected) Cyan else Color(0x22FFFFFF), shape)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Cyan else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val S = LocalStrings.current
    var editing by remember { mutableStateOf<Cmd?>(null) }
    var raw by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            CardBox(Cyan) {
                CardTitle(S.language, "LANGUAGE", Cyan)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LangChip("فارسی", vm.lang == "fa") { vm.setLang("fa") }
                    LangChip("English", vm.lang == "en") { vm.setLang("en") }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(raw, { raw = it }, label = { Text(S.sendCustom) },
                    modifier = Modifier.weight(1f), singleLine = true)
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (raw.isNotEmpty()) vm.send(raw) }) { Text(S.send) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { editing = Cmd(label = "", command = "", group = "سایر") }) {
                    Icon(Icons.Default.Add, null); Text(S.newCommand)
                }
                OutlinedButton(onClick = { vm.resetDefaults() }) { Text(S.defaults) }
            }
        }
        items(vm.commands, key = { it.id }) { c ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${S.group(c.group)} \u2022 ${S.cmdLabel(c)}")
                        Text(S.sentCommand(c.command), fontSize = 11.sp, color = Grey)
                    }
                    IconButton(onClick = { editing = c }) { Icon(Icons.Default.Edit, null) }
                    if (c.action == "NONE") {
                        IconButton(onClick = { vm.delete(c.id) }) { Icon(Icons.Default.Delete, null) }
                    }
                }
            }
        }
    }
    editing?.let { cmd ->
        var label by remember(cmd.id) { mutableStateOf(S.cmdLabel(cmd)) }
        var command by remember(cmd.id) { mutableStateOf(cmd.command) }
        var group by remember(cmd.id) { mutableStateOf(cmd.group) }
        val groups = (listOf("جلو", "عقب", "میباخ", "سایر") + vm.commands.map { it.group })
            .filter { it != "سیستمی" }.distinct()
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(S.editCommand) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(label, { label = it }, label = { Text(S.buttonName) }, singleLine = true)
                    OutlinedTextField(command, { command = it }, label = { Text(S.commandField) }, singleLine = true)
                    if (cmd.action == "NONE") {
                        OutlinedTextField(group, { group = it }, label = { Text(S.groupField) }, singleLine = true)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            groups.forEach { g ->
                                Text(
                                    S.group(g), fontSize = 12.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(if (group.trim() == g) Cyan.copy(alpha = 0.3f) else Color(0xFF222B3A))
                                        .clickable { group = g }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (label.isNotBlank() && command.isNotEmpty()) {
                        // اگر اسم دکمه را دست نزده‌ای، اسم اصلی (پیش‌فرض) می‌ماند تا با تغییر زبان ترجمه شود
                        val newLabel = if (label.trim() == S.cmdLabel(cmd)) cmd.label else label.trim()
                        vm.upsert(cmd.copy(label = newLabel, command = command, group = group.ifBlank { "سایر" }.trim()))
                        editing = null
                    }
                }) { Text(S.save) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(S.cancel) } }
        )
    }
}
