package com.sewa.ai

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** [confirmTap] is set when a UI tap needs the user's explicit yes/no before running. */
data class EngineResult(val reply: String, val confirmTap: String? = null)

object LocalCommandEngine {
    private val YES = setOf("yes", "y", "haan", "han", "ha", "haa", "confirm", "ok", "okay", "kar do", "हाँ", "हां", "जी")
    private const val NEED_SERVICE =
        "Phone control ke liye Accessibility service on karni hogi. 'enable phone control' bolo, phir list mein SEWA AI ko on karo."

    fun isYes(raw: String): Boolean = raw.trim().lowercase(Locale.ROOT).trimEnd('.', '!') in YES

    private fun String.hasAny(vararg keys: String) = keys.any { contains(it) }

    suspend fun handle(context: Context, raw: String): EngineResult? {
        val c = raw.trim().lowercase(Locale.ROOT)
        if (c.isEmpty()) return null

        // ---- Timer ----
        if (c.hasAny("timer", "टाइमर")) {
            val ask = EngineResult("Kitne der ka timer? Jaise: 'set timer 5 minutes'.")
            val m = Regex("(\\d+)\\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?|घंटे|घंटा|मिनट|सेकंड)").find(c) ?: return ask
            val n = m.groupValues[1].toIntOrNull() ?: return ask
            val unit = m.groupValues[2]
            val secs = when {
                unit.startsWith("sec") || unit == "सेकंड" -> n
                unit.startsWith("min") || unit == "मिनट" -> n * 60
                else -> n * 3600
            }.coerceIn(1, 86400)
            val intent = Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, secs)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            return EngineResult(safeStart(context, intent, "Timer set: $n ${unit}.", "Timer app nahi mila."))
        }

        // ---- Flashlight ----
        if (c.hasAny("torch", "flashlight", "टॉर्च", "फ्लैशलाइट")) {
            return EngineResult(torch(context, !c.hasAny("off", "band", "बंद")))
        }

        // ---- Volume ----
        if (c.hasAny("volume", "वॉल्यूम")) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val up = c.hasAny("up", "badha", "increase", "zyada", "बढ़ा")
            val down = c.hasAny("down", "kam", "decrease", "ghata", "कम")
            if (!up && !down) return EngineResult("Volume up ya down bolo.")
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            return EngineResult(if (up) "Volume badha diya." else "Volume kam kar diya.")
        }

        // ---- Enable phone control ----
        if (c.hasAny("enable phone control", "phone control on", "accessibility")) {
            return EngineResult(safeStart(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), "Accessibility settings khol raha hoon. SEWA AI ko on karo.", "Accessibility settings nahi khul payi."))
        }

        // ---- System-level phone control (needs Accessibility service) ----
        val globalAction: Pair<Int, String>? = when {
            c.hasAny("go home", "home screen", "home jao", "होम स्क्रीन") -> AccessibilityService.GLOBAL_ACTION_HOME to "Home screen par ja raha hoon."
            c.hasAny("go back", "back jao", "wapas jao", "पीछे जाओ") -> AccessibilityService.GLOBAL_ACTION_BACK to "Back."
            c.hasAny("recent apps", "recents", "रीसेंट") -> AccessibilityService.GLOBAL_ACTION_RECENTS to "Recent apps."
            c.hasAny("quick settings", "क्विक सेटिंग") -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS to "Quick settings khol di."
            c.hasAny("notification", "नोटिफिकेशन") -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS to "Notifications khol raha hoon."
            c.hasAny("lock screen", "lock phone", "phone lock", "screen lock", "फोन लॉक") -> AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN to "Phone lock kar raha hoon."
            c.hasAny("screenshot", "स्क्रीनशॉट") -> AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT to "Screenshot le raha hoon."
            else -> null
        }
        if (globalAction != null) {
            val service = SewaAccessibilityService.instance ?: return EngineResult(NEED_SERVICE)
            return EngineResult(if (service.global(globalAction.first)) globalAction.second else "Ye action abhi nahi ho paya.")
        }

        // ---- Scroll (acts on the app in front of SEWA, so SEWA steps aside first) ----
        if (c.hasAny("scroll down", "neeche scroll", "नीचे स्क्रोल", "scroll up", "upar scroll", "ऊपर स्क्रोल")) {
            val service = SewaAccessibilityService.instance ?: return EngineResult(NEED_SERVICE)
            val forward = c.hasAny("scroll down", "neeche", "नीचे")
            (context as? Activity)?.moveTaskToBack(true)
            delay(900)
            return EngineResult(if (service.scroll(forward)) "Scroll kar diya." else "Scroll karne layak kuch nahi mila. Pehle woh app kholo jo scroll karni hai.")
        }

        // ---- Tap on screen text (sensitive: always asks for confirmation) ----
        val tapTarget = Regex("^(?:tap|click|press)\\s+(?:on\\s+)?(.+)$").find(c)?.groupValues?.get(1)
            ?: Regex("^(.+?)\\s+(?:dabao|click karo|tap karo)$").find(c)?.groupValues?.get(1)
        if (tapTarget != null && tapTarget.isNotBlank()) {
            if (SewaAccessibilityService.instance == null) return EngineResult(NEED_SERVICE)
            return EngineResult("'$tapTarget' par tap karun? 'haan' ya 'yes' bolo confirm karne ke liye.", confirmTap = tapTarget.trim())
        }

        // ---- Settings pages ----
        if (c.hasAny("wifi", "wi-fi", "वाईफाई")) {
            return EngineResult(safeStart(context, Intent(Settings.ACTION_WIFI_SETTINGS), "Android Wi-Fi ko app se on/off karne nahi deta, isliye Wi-Fi settings khol raha hoon.", "Wi-Fi settings nahi khul payi."))
        }
        if (c.hasAny("bluetooth", "ब्लूटूथ")) {
            return EngineResult(safeStart(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Bluetooth settings khol raha hoon.", "Bluetooth settings nahi khul payi."))
        }
        if (c.hasAny("open settings", "settings kholo", "सेटिंग खोलो", "सेटिंग्स खोलो")) {
            return EngineResult(safeStart(context, Intent(Settings.ACTION_SETTINGS), "Opening phone settings.", "Settings nahi khul payi."))
        }

        // ---- Camera ----
        if (c.hasAny("open camera", "camera kholo", "कैमरा खोलो")) {
            val intent = Intent("android.media.action.IMAGE_CAPTURE")
            return EngineResult(if (intent.resolveActivity(context.packageManager) != null) safeStart(context, intent, "Opening camera.", "Camera nahi khul paya.") else "No camera app is available.")
        }

        // ---- Specific apps ----
        if (c.hasAny("open youtube", "youtube kholo", "यूट्यूब खोलो", "یوٹیوب کھولو")) return EngineResult(launchPackage(context, "com.google.android.youtube", "YouTube"))
        if (c.hasAny("open chrome", "chrome kholo", "क्रोम खोलो")) return EngineResult(launchPackage(context, "com.android.chrome", "Chrome"))

        // ---- Dial (opens dialer; the user presses the call button) ----
        Regex("(?:call|dial)\\s+([+\\d][\\d\\s-]{4,})").find(c)?.let {
            val number = it.groupValues[1].filter { ch -> ch.isDigit() || ch == '+' }
            return EngineResult(safeStart(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")), "Dialer khol raha hoon: $number. Call button aap dabayenge.", "Dialer nahi mila."))
        }

        // ---- Web search / maps ----
        Regex("^(?:search|google)\\s+(?:for\\s+)?(.+)$").find(c)?.let {
            val q = it.groupValues[1]
            return EngineResult(safeStart(context, Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q), "Searching: $q", "Search ke liye koi browser nahi mila."))
        }
        Regex("^(?:navigate to|directions to|map of)\\s+(.+)$").find(c)?.let {
            val q = it.groupValues[1]
            return EngineResult(safeStart(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(q))), "Maps mein khol raha hoon: $q", "Maps app nahi mila."))
        }

        // ---- Battery / time ----
        if (c.hasAny("battery", "बैटरी")) {
            val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            return EngineResult("Your battery level is ${manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)} percent.")
        }
        if (Regex("\\btime\\b").containsMatchIn(c) || c.hasAny("समय", "टाइम", "samay")) {
            return EngineResult("The time is ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}.")
        }

        // ---- Open any installed app by name ("open whatsapp", "whatsapp kholo") ----
        val appName = Regex("^(?:open|launch|start)\\s+(.+)$").find(c)?.groupValues?.get(1)
            ?: Regex("^(.+?)\\s+(?:kholo|chalu karo|खोलो)$").find(c)?.groupValues?.get(1)
        if (appName != null) return EngineResult(launchByLabel(context, appName.trim()))

        // ---- Greeting ----
        if (c.hasAny("hello", "namaste", "नमस्ते", "हेलो")) return EngineResult("Hello! Main SEWA hoon. Main aapki madad ke liye ready hoon.")

        return null
    }

    /** Runs a confirmed tap. SEWA steps aside first so the tap lands on the app behind it. */
    suspend fun tapText(context: Context, text: String): String {
        val service = SewaAccessibilityService.instance ?: return NEED_SERVICE
        (context as? Activity)?.moveTaskToBack(true)
        delay(900)
        return if (service.clickText(text)) "'$text' par tap kar diya." else "Screen par '$text' nahi mila. Pehle woh app/screen kholo."
    }

    private fun torch(context: Context, on: Boolean): String = try {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        if (id == null) "Is phone mein flashlight nahi mili." else { cm.setTorchMode(id, on); if (on) "Flashlight on." else "Flashlight off." }
    } catch (e: Exception) { "Flashlight control nahi ho paya." }

    private fun safeStart(context: Context, intent: Intent, ok: String, fail: String): String = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); ok
    } catch (e: ActivityNotFoundException) { fail } catch (e: SecurityException) { fail }

    private fun launchPackage(context: Context, packageName: String, appName: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) safeStart(context, intent, "Opening $appName.", "$appName cannot be opened.")
        else "$appName is not installed or cannot be opened on this device."
    }

    private fun launchByLabel(context: Context, name: String): String {
        if (name.length < 2) return "Kaunsa app kholna hai?"
        val pm = context.packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        val match = apps.firstOrNull { it.loadLabel(pm).toString().equals(name, ignoreCase = true) }
            ?: apps.firstOrNull { it.loadLabel(pm).toString().contains(name, ignoreCase = true) }
            ?: return "'$name' naam ka app nahi mila."
        val intent = pm.getLaunchIntentForPackage(match.activityInfo.packageName) ?: return "'$name' open nahi ho paya."
        val label = match.loadLabel(pm).toString()
        return safeStart(context, intent, "Opening $label.", "$label open nahi ho paya.")
    }
}
