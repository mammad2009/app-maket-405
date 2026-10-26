package com.peugeot405.remote

import android.Manifest
import android.app.*
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import java.io.OutputStream
import java.util.UUID

class MainActivity : Activity() {
    private val bg = Color.rgb(5, 13, 19)
    private val panel = Color.rgb(13, 30, 39)
    private val cyan = Color.rgb(0, 224, 239)
    private val red = Color.rgb(255, 35, 55)
    private var english = false
    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null
    private var status: TextView? = null
    private var root: LinearLayout? = null
    private val spp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private fun tr(fa:String,en:String)=if(english) en else fa

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); window.statusBarColor=bg; window.navigationBarColor=bg; showSplash() }

    private fun shell(): LinearLayout {
        val sc=ScrollView(this); val col=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20,18,20,28); setBackgroundColor(bg) }
        sc.addView(col); setContentView(sc); root=col; return col
    }
    private fun text(s:String,size:Float=16f,color:Int=Color.WHITE,bold:Boolean=false)=TextView(this).apply {
        text=s; textSize=size; setTextColor(color); if(bold) typeface=Typeface.DEFAULT_BOLD
        gravity=Gravity.CENTER_VERTICAL; setPadding(4,7,4,7)
    }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; setPadding(14,12,14,12)
        background=GradientDrawable().apply { setColor(panel); cornerRadius=24f; setStroke(1,Color.rgb(30,75,88)) }
    }
    private fun button(label:String, action:()->Unit, color:Int=panel): Button = Button(this).apply {
        text=label; textSize=14f; setTextColor(if(color==cyan) Color.BLACK else Color.WHITE)
        background=GradientDrawable().apply { setColor(color); cornerRadius=22f }
        isAllCaps=false; setOnClickListener { action() }
    }
    private fun addGap(c:LinearLayout,h:Int=10){ c.addView(View(this),LinearLayout.LayoutParams(1,h)) }
    private fun showSplash(){
        val c=shell(); c.gravity=Gravity.CENTER
        val logo=ImageView(this).apply { setImageResource(R.drawable.peugeot405_logo); adjustViewBounds=true; scaleType=ImageView.ScaleType.FIT_CENTER }
        c.addView(logo,LinearLayout.LayoutParams(-1,330)); addGap(c,18)
        c.addView(text("PEUGEOT 405",30f,Color.WHITE,true).apply{gravity=Gravity.CENTER})
        c.addView(text("LEGEND NEVER DIES  •  PRIVATE GARAGE",11f,cyan,true).apply{gravity=Gravity.CENTER})
        addGap(c,30); c.addView(button(tr("ورود به کابین","ENTER THE GARAGE"),{showHome()},cyan),LinearLayout.LayoutParams(-1,58))
        c.addView(text(tr("رابط اختصاصی ماکت پژو ۴۰۵","A dedicated interface for your Peugeot 405 model"),13f,Color.LTGRAY).apply{gravity=Gravity.CENTER})
    }
    private fun header(c:LinearLayout,title:String){
        c.addView(text("PEUGEOT 405  /  ${title.uppercase()}",22f,cyan,true))
        c.addView(text("PRIVATE CONTROL SYSTEM  •  V1.0",10f,Color.GRAY,true))
        addGap(c,8)
    }
    private fun showHome(){
        val c=shell(); header(c,tr("کابین اصلی","Main garage"))
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL}
        status=text(if(socket==null) tr("● قطع اتصال","● DISCONNECTED") else tr("● متصل","● CONNECTED"),13f,red,true)
        top.addView(status,LinearLayout.LayoutParams(0,-2,1f))
        top.addView(button(if(english)"FA" else "EN",{english=!english;showHome()},cyan))
        c.addView(top); addGap(c,12)
        val hero=card(); hero.addView(text(tr("سیستم کنترل ۴۰۵","405 CONTROL SYSTEM"),18f,Color.WHITE,true))
        hero.addView(text(tr("کنترل تعلیق، چراغ‌ها و حالت‌های نمایشی ماکت از یک کابین لوکس.","Manage suspension, lights and show modes from one premium cockpit."),13f,Color.LTGRAY))
        hero.addView(button(tr("اتصال بلوتوث HC-05","CONNECT BLUETOOTH HC-05"),{connectDialog()},cyan),LinearLayout.LayoutParams(-1,52))
        c.addView(hero); addGap(c)
        val menu=card(); menu.addView(text(tr("دسترسی سریع","QUICK ACCESS"),16f,cyan,true))
        menu.addView(button(tr("ریموت و تنظیم ارتفاع","SUSPENSION REMOTE"),{showRemote()}))
        menu.addView(button(tr("چراغ‌ها و راهنماها","LIGHTS & INDICATORS"),{showLights()}))
        menu.addView(button(tr("حالت نمایشی MAYBACH","MAYBACH SHOW MODE"),{send('M');toast(tr("فرمان MAYBACH ارسال شد","MAYBACH command sent"))}))
        menu.addView(button(tr("قفل / بازکردن ماکت","LOCK / UNLOCK MODEL"),{send('K');toast(tr("فرمان قفل ارسال شد","Lock command sent"))}))
        menu.addView(button(tr("کنسول ارسال فرمان","CUSTOM COMMAND CONSOLE"),{showConsole()}))
        menu.addView(button(tr("تنظیمات و راهنما","SETTINGS & GUIDE"),{showSettings()}))
        c.addView(menu); addGap(c)
        c.addView(text(tr("نکته: اتصال فعلی برای بلوتوث کلاسیک HC-05 طراحی شده است. ابتدا از تنظیمات گوشی با ماژول Pair شوید.","Note: this build targets classic Bluetooth HC-05. Pair with the module in Android settings first."),11f,Color.GRAY))
    }
    private fun connectDialog(){
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT),77); return
        }
        try {
            val adapter=BluetoothAdapter.getDefaultAdapter()
            if(adapter==null){toast(tr("این گوشی بلوتوث ندارد","Bluetooth unavailable"));return}
            if(!adapter.isEnabled){startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));return}
            val paired=adapter.bondedDevices.toList()
            if(paired.isEmpty()){toast(tr("ابتدا HC-05 را در تنظیمات بلوتوث Pair کن","Pair HC-05 in Android Bluetooth settings first"));return}
            val names=paired.map{"${it.name ?: "Bluetooth device"}  (${it.address})"}.toTypedArray()
            AlertDialog.Builder(this).setTitle(tr("انتخاب ماژول ماشین","Select car module")).setItems(names){_,which->
                Thread {
                    try {
                        val device=paired[which]; adapter.cancelDiscovery()
                        val s=device.createRfcommSocketToServiceRecord(spp); s.connect()
                        socket=s; output=s.outputStream
                        runOnUiThread { toast(tr("اتصال برقرار شد","Connected")); showHome() }
                    } catch(e:Exception){runOnUiThread{toast(tr("اتصال ناموفق؛ HC-05 را بررسی کن","Connection failed; check HC-05"))}}
                }.start()
            }.setNegativeButton(tr("لغو","Cancel"),null).show()
        } catch(e:SecurityException){toast("Bluetooth permission required")}
    }
    private fun send(ch:Char){
        try { val o=output; if(o==null){toast(tr("اول به ماشین وصل شو","Connect to the car first"));return}; Thread{try{o.write(ch.code);o.flush()}catch(_:Exception){}}.start() }
        catch(_:Exception){toast(tr("ارسال فرمان ناموفق بود","Command failed"))}
    }
    private fun showRemote(){
        val c=shell(); header(c,tr("ریموت تعلیق","Suspension remote"))
        c.addView(text(tr("حرکت را نگه دار؛ برای توقف، انگشتت را بردار.","Hold a control to move; release to stop."),12f,Color.LTGRAY))
        val box=card(); box.addView(text(tr("محور جلو","FRONT AXLE"),16f,cyan,true))
        val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        row.addView(holdButton("▲  "+tr("بالا","UP"), 'F'),LinearLayout.LayoutParams(0,76,1f))
        row.addView(holdButton(tr("▼ پایین","▼ DOWN"), 'B'),LinearLayout.LayoutParams(0,76,1f))
        c.addView(box); box.addView(row)
        addGap(c); val back=card(); back.addView(text(tr("محور عقب","REAR AXLE"),16f,cyan,true))
        val r2=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        r2.addView(holdButton("▲  "+tr("بالا","UP"), 'R'),LinearLayout.LayoutParams(0,76,1f))
        r2.addView(holdButton(tr("▼ پایین","▼ DOWN"), 'L'),LinearLayout.LayoutParams(0,76,1f)); back.addView(r2); c.addView(back)
        addGap(c); val both=card(); both.addView(text(tr("کنترل هم‌زمان","BOTH AXLES"),16f,cyan,true))
        both.addView(button(tr("هر دو بالا","BOTH UP"),{send('W')},cyan)); both.addView(button(tr("هر دو پایین","BOTH DOWN"),{send('D')}))
        both.addView(button(tr("جلو حداقل","FRONT MIN"),{send('1')})); both.addView(button(tr("جلو حداکثر","FRONT MAX"),{send('2')}))
        both.addView(button(tr("عقب حداقل","REAR MIN"),{send('X')})); both.addView(button(tr("عقب حداکثر","REAR MAX"),{send('Z')}))
        c.addView(both); c.addView(button(tr("بازگشت","BACK"),{showHome()}))
    }
    private fun holdButton(label:String, down:Char):Button = button(label,{send(down)},cyan).apply {
        setOnTouchListener { v,e ->
            when(e.action){ MotionEvent.ACTION_DOWN -> {send(down);true}
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {send('Q');true}
                else -> true }
        }
    }
    private fun showLights(){
        val c=shell(); header(c,tr("چراغ‌ها","Lights"))
        val b=card()
        fun item(fa:String,en:String,on:Char,off:Char){ b.addView(text(tr(fa,en),14f,Color.WHITE,true)); val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            row.addView(button(tr("روشن","ON"),{send(on)},cyan),LinearLayout.LayoutParams(0,52,1f))
            row.addView(button(tr("خاموش","OFF"),{send(off)}),LinearLayout.LayoutParams(0,52,1f)); b.addView(row); addGap(b,5) }
        item("چراغ کوچک","Position lights",'3','4'); item("نور بالا","High beam",'5','6'); item("چراغ عقب","Rear lights",'9','a')
        item("راهنمای راست","Right indicator",'e','f'); item("راهنمای چپ","Left indicator",'g','h')
        item("فلاشر","Hazards",'7','8'); item("راهنمای نوبتی","Alternating indicators",'i','j')
        b.addView(button(tr("افکت چراغ عربی: تغییر افکت","Arabic light effect: next effect"),{send('o')},cyan))
        b.addView(button(tr("توقف افکت عربی","Stop Arabic effect"),{send('p')}))
        c.addView(b); c.addView(button(tr("بازگشت","BACK"),{showHome()}))
    }
    private fun showConsole(){
        val c=shell(); header(c,tr("کنسول فرمان","Command console"))
        c.addView(text(tr("یک کاراکتر بفرست؛ فرمان باید با کد آردوینو سازگار باشد.","Send one character; it must match the Arduino firmware."),12f,Color.LTGRAY))
        val input=EditText(this).apply{setSingleLine();textSize=22f;setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);hint="F";setPadding(18,12,18,12);background=GradientDrawable().apply{setColor(panel);cornerRadius=18f;setStroke(1,cyan)}}
        c.addView(input); c.addView(button(tr("ارسال فرمان","SEND COMMAND"),{val s=input.text.toString();if(s.length==1)send(s[0]) else toast(tr("فقط یک کاراکتر وارد کن","Enter exactly one character"))},cyan))
        c.addView(button(tr("حالت نمایشی همه صحنه‌ها (T)","Show all eye scenes (T)"),{send('T')}))
        c.addView(button(tr("پایان حالت MAYBACH (N)","Stop MAYBACH (N)"),{send('N')}))
        c.addView(button(tr("بازگشت","BACK"),{showHome()}))
    }
    private fun showSettings(){
        val c=shell(); header(c,tr("تنظیمات","Settings"))
        val b=card()
        b.addView(text(tr("پروتکل ارتباطی","Connection protocol"),16f,cyan,true))
        b.addView(text("Bluetooth Classic / SPP • HC-05",14f))
        b.addView(text(tr("برد هدف: ESP8266 NodeMCU طبق فایل car_emo_eyes.ino","Target: ESP8266 NodeMCU based on the supplied firmware."),13f,Color.LTGRAY))
        b.addView(text(tr("فرمان‌های تعلیق: F/B جلو، R/L عقب، W/D هر دو؛ 1/2 و X/Z رفتن به دو انتهای تعریف‌شده.","Suspension: F/B front, R/L rear, W/D both; 1/2 and X/Z move to preset endpoints."),12f,Color.LTGRAY))
        b.addView(text(tr("احتیاط: اگر سازوکار گیر کرد، فوراً فرمان حرکت را رها کن. محدوده سروو را فقط با آزمون ایمن تغییر بده.","Safety: release controls if the linkage binds. Adjust servo limits only with safe testing."),12f,red))
        b.addView(button(tr("تغییر زبان","TOGGLE LANGUAGE"),{english=!english;showSettings()},cyan))
        c.addView(b); c.addView(button(tr("بازگشت","BACK"),{showHome()}))
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<out String>, grantResults:IntArray){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==77 && grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED) connectDialog()
    }
    override fun onDestroy(){try{socket?.close()}catch(_:Exception){};super.onDestroy()}
}
