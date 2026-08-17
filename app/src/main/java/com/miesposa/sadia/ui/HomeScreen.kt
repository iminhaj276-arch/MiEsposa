package com.miesposa.sadia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miesposa.sadia.ui.theme.*

data class ChatMessage(val text: String, val fromUser: Boolean)

data class QuickAction(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val utterance: String)

private val quickActions = listOf(
    QuickAction("ব্যাটারি", Icons.Default.BatteryFull, "আমার battery কত?"),
    QuickAction("ফ্ল্যাশলাইট", Icons.Default.FlashOn, "flashlight চালু করো"),
    QuickAction("ক্যামেরা", Icons.Default.CameraAlt, "camera খুলে দাও"),
    QuickAction("YouTube", Icons.Default.PlayArrow, "youtube খুলে দাও"),
    QuickAction("সার্চ", Icons.Default.Search, "search "),
    QuickAction("সেটিংস", Icons.Default.Settings, "settings খুলে দাও"),
    QuickAction("ফোন লক", Icons.Default.Lock, "ফোন লক করো"),
    QuickAction("কল লিস্ট", Icons.Default.Call, "recent call")
)

@Composable
fun HomeScreen(
    messages: List<ChatMessage>,
    isListening: Boolean,
    onMicTapped: () -> Unit,
    onSendText: (String) -> Unit,
    onQuickAction: (String) -> Unit,
    errorMessage: String?,
    onErrorShown: () -> Unit,
    isWakeWordOn: Boolean = false,
    onWakeWordToggle: () -> Unit = {},
    onOpenPermissionCenter: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }

    Scaffold(
        containerColor = SadiaBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { SadiaHeader(isWakeWordOn = isWakeWordOn, onWakeWordToggle = onWakeWordToggle) },
        bottomBar = {
            InputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        onSendText(inputText)
                        inputText = ""
                    }
                },
                isListening = isListening,
                onMicTapped = onMicTapped
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            QuickActionsRow(onQuickAction = onQuickAction, onOpenPermissionCenter = onOpenPermissionCenter)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { message -> ChatBubble(message) }
            }
        }
    }
}

@Composable
private fun SadiaHeader(isWakeWordOn: Boolean, onWakeWordToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(SadiaSurface, SadiaBackground))
            )
            .padding(top = 20.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("🥰 Mi Esposa 🧕", color = SadiaTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Sadia • সবসময় তোমার পাশে", color = SadiaTextSecondary, fontSize = 13.sp)
            }
            WakeWordSwitch(isOn = isWakeWordOn, onToggle = onWakeWordToggle)
        }
        if (isWakeWordOn) {
            Text(
                "🎙️ 'Sadia' বললেই সাড়া দেবে",
                color = SadiaGlow,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun WakeWordSwitch(isOn: Boolean, onToggle: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Switch(
            checked = isOn,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = SadiaGlow,
                checkedTrackColor = SadiaPurple,
                uncheckedThumbColor = SadiaTextSecondary
            )
        )
        Text("Wake word", color = SadiaTextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun QuickActionsRow(onQuickAction: (String) -> Unit, onOpenPermissionCenter: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScrollCompat(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        quickActions.forEach { action ->
            QuickActionChip(action) { onQuickAction(action.utterance) }
        }
        Surface(
            onClick = onOpenPermissionCenter,
            shape = RoundedCornerShape(16.dp),
            color = SadiaCard,
            modifier = Modifier.height(76.dp).width(76.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Security, contentDescription = "Permission Center", tint = SadiaGlow)
                Spacer(Modifier.height(4.dp))
                Text("পারমিশন", color = SadiaTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun QuickActionChip(action: QuickAction, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = SadiaCard,
        modifier = Modifier.height(76.dp).width(76.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(action.icon, contentDescription = action.label, tint = SadiaGlow)
            Spacer(Modifier.height(4.dp))
            Text(action.label, color = SadiaTextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val bubbleColor = if (message.fromUser) SadiaPurple.copy(alpha = 0.35f) else SadiaCard
    val alignment = if (message.fromUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (message.fromUser) 16.dp else 2.dp,
                bottomEnd = if (message.fromUser) 2.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                message.text,
                color = SadiaTextPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun InputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    onMicTapped: () -> Unit
) {
    Surface(color = SadiaSurface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Sadia-কে লিখো...", color = SadiaTextSecondary) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SadiaTextPrimary,
                    unfocusedTextColor = SadiaTextPrimary,
                    focusedBorderColor = SadiaPurple,
                    unfocusedBorderColor = SadiaTextSecondary.copy(alpha = 0.3f)
                ),
                singleLine = true
            )

            IconButton(onClick = onSend) {
                Icon(Icons.Default.Send, contentDescription = "পাঠাও", tint = SadiaGlow)
            }

            MicButton(isListening = isListening, onClick = onMicTapped)
        }
    }
}

@Composable
private fun MicButton(isListening: Boolean, onClick: () -> Unit) {
    val bg = if (isListening) SadiaGlow else SadiaDeepRed
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(bg, SadiaPurple))),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(
                if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                contentDescription = "কথা বলো",
                tint = Color.White
            )
        }
    }
}

// Minimal cross-version horizontal scroll helper to avoid extra imports at call sites.
@Composable
private fun Modifier.horizontalScrollCompat(): Modifier {
    val scrollState = rememberScrollState()
    return this.then(Modifier.horizontalScroll(scrollState))
}
