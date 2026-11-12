package com.sewa.ai

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF08090D)
private val Panel = Color(0xFF141722)
private val Violet = Color(0xFF9B7BFF)
private val Cyan = Color(0xFF65DDEB)
private val Muted = Color(0xFF9AA3B2)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        tts = TextToSpeech(this, this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Violet, secondary = Cyan, background = Bg, surface = Panel)) {
                SewaHome(this, SewaDatabase.get(this).chatDao(), { speak(it) })
            }
        }
    }
    override fun onInit(status: Int) { ttsReady = status == TextToSpeech.SUCCESS }
    private fun speak(text: String) { if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sewa-${System.currentTimeMillis()}") }
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); tts = null; super.onDestroy() }
}

@Composable
private fun SewaHome(context: Context, chatDao: ChatDao, speak: (String) -> Unit) {
    var status by remember { mutableStateOf("Ready when you are") }
    var heardText by remember { mutableStateOf("Tap the microphone or type a message") }
    var reply by remember { mutableStateOf("Namaste! Main SEWA hoon. Aapki kya madad kar sakta hoon?") }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var pendingTap by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val history = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) { chatDao.recent(40).reversed() }
        history.clear()
        saved.forEach { history.add(if (it.role == "user") "You: ${it.content}" else "SEWA: ${it.content}") }
    }

    suspend fun append(user: String, answer: String) {
        withContext(Dispatchers.IO) {
            chatDao.insertAll(listOf(ChatMessageEntity(role = "user", content = user), ChatMessageEntity(role = "assistant", content = answer)))
        }
        history.add("You: $user"); history.add("SEWA: $answer")
        while (history.size > 40) history.removeAt(0)
    }

    fun process(text: String) {
        if (text.isBlank() || busy) return
        input = ""; heardText = text; busy = true; status = "Understanding…"
        scope.launch {
            val pending = pendingTap
            pendingTap = null
            val result = if (pending != null) {
                if (LocalCommandEngine.isYes(text)) LocalCommandEngine.tapText(context, pending) else "Theek hai, tap cancel kar diya."
            } else {
                val r = LocalCommandEngine.handle(context, text)
                pendingTap = r?.confirmTap
                r?.reply ?: LlmClient.reply(text, history.toList())
            }
            reply = result; status = "Ready when you are"; busy = false
            append(text, result); speak(result)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) process(spoken) else status = "I couldn't hear that. Please try again."
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { if (!launchSpeech(context, speechLauncher)) status = "Speech recognition is not available on this device." } else status = "Microphone permission was denied."
    }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SEWA", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                    Text("PERSONAL AI AGENT", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp)
                }
                TextButton(onClick = { scope.launch { withContext(Dispatchers.IO) { chatDao.clearAll() }; history.clear(); reply = "Local chat history cleared."; status = "Ready when you are" } }) { Text("CLEAR HISTORY", color = Cyan, fontSize = 10.sp) }
            }
            Spacer(Modifier.height(22.dp))
            Text("UNDERSTAND EVERY LANGUAGE", color = Muted, fontSize = 10.sp, letterSpacing = 1.6.sp)
            Text("EXECUTE AUTHORIZED ACTIONS", color = Muted, fontSize = 10.sp, letterSpacing = 1.6.sp)
            Spacer(Modifier.height(12.dp))
            val transition = rememberInfiniteTransition(label = "orb")
            val pulse by transition.animateFloat(0.94f, 1.05f, infiniteRepeatable(tween(1700), RepeatMode.Reverse), label = "pulse")
            Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(160.dp).scale(pulse).background(Brush.radialGradient(listOf(Color(0x554F3B9D), Color(0x223D8FA5), Color.Transparent)), CircleShape))
                Box(Modifier.size(116.dp).background(Brush.linearGradient(listOf(Color(0xFF30245D), Color(0xFF151B2A), Color(0xFF203C49))), CircleShape), contentAlignment = Alignment.Center) {
                    Text("S", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Light)
                }
            }
            Text(status, color = Cyan, fontSize = 12.sp)
            Spacer(Modifier.height(5.dp))
            Text(heardText, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                status = "Listening…"
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    if (!launchSpeech(context, speechLauncher)) status = "Speech recognition is not available on this device."
                } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }, enabled = !busy, modifier = Modifier.size(66.dp), shape = CircleShape, contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet)) { Text("MIC", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("SEWA RESPONSE", color = Violet, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp)); Text(reply, color = Color.White, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), singleLine = true,
                    placeholder = { Text("Message SEWA…", fontSize = 12.sp) }, shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.width(8.dp))
                Button(onClick = { process(input) }, enabled = input.isNotBlank() && !busy, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("SEND") }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("RECENT ACTIVITY", color = Muted, fontSize = 10.sp, letterSpacing = 1.3.sp)
                Text("${history.size / 2} interactions", color = Muted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(history.takeLast(8).reversed()) { item ->
                    Text(item, color = if (item.startsWith("You:")) Color.White else Muted, fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(9.dp)).padding(9.dp))
                }
            }
            Text("SEWA AI • LOCAL-ONLY BUILD", color = Color(0xFF626B7B), fontSize = 9.sp, letterSpacing = 1.sp)
        }
    }
}

private fun launchSpeech(context: Context, launcher: androidx.activity.result.ActivityResultLauncher<Intent>): Boolean {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to SEWA")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }
    return try { launcher.launch(intent); true } catch (e: ActivityNotFoundException) { false }
}
