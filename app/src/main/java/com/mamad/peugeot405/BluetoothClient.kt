package com.mamad.peugeot405

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothClient(private val context: Context) {
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private var socket: BluetoothSocket? = null
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    val isSupported: Boolean get() = adapter != null
    val isEnabled: Boolean get() = adapter?.isEnabled == true

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    fun pairedDevices(): List<BluetoothDevice> {
        if (!hasPermission()) return emptyList()
        return runCatching { adapter?.bondedDevices?.toList().orEmpty() }.getOrDefault(emptyList())
    }

    // سه روش اتصال: معمولی، ناامن، و روش کانال ۱ (برای گوشی‌هایی که روش معمولی را قبول نمی‌کنند)
    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        disconnect()
        runCatching { adapter?.cancelDiscovery() }
        val makers = listOf<() -> BluetoothSocket>(
            { device.createRfcommSocketToServiceRecord(sppUuid) },
            { device.createInsecureRfcommSocketToServiceRecord(sppUuid) },
            {
                device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    .invoke(device, 1) as BluetoothSocket
            }
        )
        var last: Throwable? = null
        for (make in makers) {
            var s: BluetoothSocket? = null
            try {
                s = make()
                s.connect()
                socket = s
                return@withContext Result.success(Unit)
            } catch (e: Throwable) {
                last = e
                runCatching { s?.close() }
                delay(250)
            }
        }
        Result.failure<Unit>(last ?: IllegalStateException("اتصال برقرار نشد"))
    }

    suspend fun send(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val out = socket!!.outputStream
            out.write(text.toByteArray())
            out.flush()
        }
    }

    // خطوطی که ماکت می‌فرستد (مثلا "BAT:87"). وقتی اتصال بسته یا قطع شود جریان تمام می‌شود
    fun lines(): Flow<String> = flow {
        val input = socket?.inputStream
        if (input != null) {
            val sb = StringBuilder()
            val buf = ByteArray(64)
            while (true) {
                val n = try { input.read(buf) } catch (e: Exception) { -1 }
                if (n < 0) break
                for (i in 0 until n) {
                    val ch = buf[i].toInt().toChar()
                    if (ch == '\n') {
                        emit(sb.toString().trim())
                        sb.setLength(0)
                    } else if (ch != '\r' && sb.length < 64) {
                        sb.append(ch)
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun disconnect() {
        runCatching { socket?.close() }
        socket = null
    }
}
