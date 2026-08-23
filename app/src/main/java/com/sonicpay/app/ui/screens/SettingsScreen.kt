package com.sonicpay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sonicpay.app.audio.TonePlayer
import com.sonicpay.app.data.CrashReporter
import com.sonicpay.app.data.SessionPrefs
import com.sonicpay.app.sonic.FskModulator
import com.sonicpay.app.sonic.SonicProtocol
import com.sonicpay.app.ui.components.GlassCard
import com.sonicpay.app.ui.components.GlassChip
import com.sonicpay.app.ui.components.TopBar
import com.sonicpay.app.ui.theme.AccentMint
import com.sonicpay.app.ui.theme.TextMuted
import com.sonicpay.app.ui.theme.TextPrimary
import com.sonicpay.app.ui.theme.TextSecondary
import com.sonicpay.app.ui.theme.WarnAmber
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun SettingsScreen(onBack: () -> Unit, onRoleSwitched: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val player = remember { TonePlayer() }
    var name by remember { mutableStateOf(SessionPrefs.merchantName) }
    var vpa by remember { mutableStateOf(SessionPrefs.merchantVpa) }
    val currentRole = SessionPrefs.savedRole

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        TopBar(title = "Settings", onBack = onBack)
        Spacer(Modifier.height(20.dp))

        SectionLabel("Your role")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoleOption(
                    icon = Icons.Filled.Storefront,
                    label = "Merchant",
                    selected = currentRole is SessionPrefs.Role.Merchant,
                    modifier = Modifier.weight(1f)
                ) {
                    SessionPrefs.chooseRole(SessionPrefs.Role.Merchant)
                    onRoleSwitched()
                }
                Spacer(Modifier.width(12.dp))
                RoleOption(
                    icon = Icons.Filled.Hearing,
                    label = "Customer",
                    selected = currentRole is SessionPrefs.Role.Customer,
                    modifier = Modifier.weight(1f)
                ) {
                    SessionPrefs.chooseRole(SessionPrefs.Role.Customer)
                    onRoleSwitched()
                }
            }
        }
        Text(
            "The app opens straight into your role.",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        if (currentRole is SessionPrefs.Role.Merchant) {
            Spacer(Modifier.height(24.dp))
            SectionLabel("Merchant profile")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            SessionPrefs.merchantName = it.trim()
                        },
                        label = { Text("Shop name") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentMint,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                            cursorColor = AccentMint
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = vpa,
                        onValueChange = {
                            vpa = it
                            SessionPrefs.merchantVpa = it.trim()
                        },
                        label = { Text("VPA (UPI id)") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentMint,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                            cursorColor = AccentMint
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Sound check")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    Column {
                        Text("Test tones", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            "Play: a real ₹1 request · Sweep: each tone in the band",
                            style = MaterialTheme.typography.bodyMedium, color = TextSecondary
                        )
                    }
                }
                GlassChip(label = "Sweep") {
                    scope.launch {
                        try {
                            player.play(sweepSamples())
                        } catch (_: Throwable) {
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                GlassChip(label = "Play") {
                    scope.launch {
                        try {
                            val frame = SonicProtocol.encodePayload("test@sonic", 100L)
                            player.play(FskModulator.frameToSamples(frame))
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "SonicPay prototype · tones travel 14.2–16.6 kHz",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
        Spacer(Modifier.height(16.dp))

        val lastCrash = remember { CrashReporter.lastCrash(context) }
        var crashVisible by remember { mutableStateOf(true) }
        if (lastCrash != null && crashVisible) {
            SectionLabel("Diagnostics")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Last crash",
                        style = MaterialTheme.typography.titleMedium,
                        color = WarnAmber
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        lastCrash,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.heightIn(max = 200.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        GlassChip(label = "Copy") {
                            clipboard.setText(AnnotatedString(lastCrash))
                        }
                        Spacer(Modifier.width(8.dp))
                        GlassChip(label = "Dismiss") {
                            CrashReporter.clear(context)
                            crashVisible = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

private fun sweepSamples(): FloatArray {
    val freqs = listOf(SonicProtocol.SYNC_FREQ_HZ) +
        (0 until SonicProtocol.TONES_PER_SYMBOL).map { SonicProtocol.dataFreq(it) }
    val segLen = (SonicProtocol.SAMPLE_RATE * 0.12).toInt()
    val fade = SonicProtocol.SAMPLE_RATE / 200
    val out = FloatArray(freqs.size * segLen)
    freqs.forEachIndexed { f, freq ->
        val base = f * segLen
        for (i in 0 until segLen) {
            val phase = 2.0 * Math.PI * freq * i / SonicProtocol.SAMPLE_RATE
            var gain = 1f
            if (i < fade) gain = i.toFloat() / fade
            else if (i > segLen - fade) gain = (segLen - i).toFloat() / fade
            out[base + i] = (sin(phase) * gain).toFloat()
        }
    }
    return out
}

@Composable
private fun RoleOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .background(
                if (selected) AccentMint.copy(alpha = 0.14f)
                else androidx.compose.ui.graphics.Color.Transparent,
                shape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) AccentMint else TextMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) AccentMint else TextSecondary
            )
        }
    }
}
