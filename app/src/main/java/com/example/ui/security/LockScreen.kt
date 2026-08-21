package com.example.ui.security

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.security.AppLockManager
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Full-screen real lock overlay. Rendered on top of everything while
 * AppLockManager.isLocked is true. Supports PIN, pattern and biometrics.
 */
@Composable
fun LockScreen() {
    val context = LocalContext.current
    val method = remember { AppLockManager.lockMethod(context) }
    val biometricAvailable = remember {
        AppLockManager.isBiometricsEnabled(context) && AppLockManager.canUseBiometrics(context)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var biometricTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(biometricTrigger) {
        if (biometricTrigger > 0 || (biometricAvailable && biometricTrigger == 0)) {
            if (biometricAvailable) showBiometricPrompt(context)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B141A)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFF25D366).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(34.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (method == AppLockManager.LockMethod.PATTERN) "Dibuja tu patrón" else "Ingresa tu PIN",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text("PanaLink está protegido", color = Color(0xFF90A4AE), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(24.dp))

            when (method) {
                AppLockManager.LockMethod.PATTERN -> PatternLockSection(onError = { errorMessage = it })
                else -> PinLockSection(onError = { errorMessage = it })
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = Color(0xFFE53935), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            if (biometricAvailable) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { biometricTrigger++ }
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Desbloquear con biometría", tint = Color(0xFF25D366), modifier = Modifier.size(40.dp))
                    Text("Usar biometría", color = Color(0xFF90A4AE), fontSize = 12.sp)
                }
            }
        }
    }
}

private fun showBiometricPrompt(context: android.content.Context) {
    val activity = context as? FragmentActivity ?: return
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            AppLockManager.unlock()
        }
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Desbloquear PanaLink")
        .setSubtitle("Verifica tu identidad")
        .setNegativeButtonText("Usar código")
        .build()
    try {
        prompt.authenticate(info)
    } catch (_: Exception) { }
}

@Composable
private fun PinLockSection(onError: (String?) -> Unit) {
    val context = LocalContext.current
    var entered by remember { mutableStateOf("") }

    fun submitAttempt(candidate: String) {
        if (AppLockManager.verifyPin(context, candidate)) {
            onError(null)
            AppLockManager.unlock()
        } else if (candidate.length >= 8) {
            onError("PIN incorrecto")
            entered = ""
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(8) { index ->
            val filled = index < entered.length
            Box(
                modifier = Modifier
                    .size(if (filled) 14.dp else 12.dp)
                    .background(
                        if (filled) Color(0xFF25D366) else Color.White.copy(alpha = 0.2f),
                        CircleShape
                    )
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))

    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    when {
                        key.isEmpty() -> Spacer(modifier = Modifier.size(64.dp))
                        key == "DEL" -> Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .clickable(enabled = entered.isNotEmpty()) {
                                    entered = entered.dropLast(1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "Borrar", tint = Color.White)
                        }
                        else -> Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                .clickable(enabled = entered.length < 8) {
                                    entered += key
                                    // Auto-verify from 4 digits up: matches any valid PIN length.
                                    if (entered.length >= 4) submitAttempt(entered)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(key, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternLockSection(onError: (String?) -> Unit) {
    val context = LocalContext.current
    PatternPad(
        onPatternComplete = { pattern ->
            if (pattern.size < 4) {
                onError("Conecta al menos 4 puntos")
            } else if (AppLockManager.verifyPattern(context, pattern)) {
                onError(null)
                AppLockManager.unlock()
            } else {
                onError("Patrón incorrecto")
            }
        }
    )
}

/**
 * Reusable 3x3 pattern pad. Emits the ordered list of selected node indices
 * (0..8, row-major) when the user lifts their finger.
 */
@Composable
fun PatternPad(
    onPatternComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0xFF90A4AE),
    activeColor: Color = Color(0xFF25D366)
) {
    var selected by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentDrag by remember { mutableStateOf<Offset?>(null) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    fun nodeCenter(index: Int): Offset {
        val col = index % 3
        val row = index / 3
        val cellW = boxSize.width / 3f
        val cellH = boxSize.height / 3f
        return Offset(cellW * col + cellW / 2f, cellH * row + cellH / 2f)
    }

    fun nodeAt(position: Offset): Int? {
        if (boxSize.width == 0) return null
        val cellW = boxSize.width / 3f
        val cellH = boxSize.height / 3f
        val radius = minOf(cellW, cellH) * 0.35f
        for (i in 0 until 9) {
            val c = nodeCenter(i)
            val dist = sqrt((position.x - c.x).pow(2) + (position.y - c.y).pow(2))
            if (dist <= radius) return i
        }
        return null
    }

    Box(
        modifier = modifier
            .size(260.dp)
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        selected = emptyList()
                        nodeAt(start)?.let { selected = listOf(it) }
                        currentDrag = start
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentDrag = change.position
                        nodeAt(change.position)?.let { node ->
                            if (node !in selected) selected = selected + node
                        }
                    },
                    onDragEnd = {
                        val result = selected
                        selected = emptyList()
                        currentDrag = null
                        if (result.isNotEmpty()) onPatternComplete(result)
                    },
                    onDragCancel = {
                        selected = emptyList()
                        currentDrag = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (selected.size >= 2) {
                for (i in 0 until selected.size - 1) {
                    drawLine(
                        color = activeColor,
                        start = nodeCenter(selected[i]),
                        end = nodeCenter(selected[i + 1]),
                        strokeWidth = 6.dp.toPx()
                    )
                }
            }
            val dragPos = currentDrag
            if (selected.isNotEmpty() && dragPos != null) {
                drawLine(
                    color = activeColor.copy(alpha = 0.6f),
                    start = nodeCenter(selected.last()),
                    end = dragPos,
                    strokeWidth = 6.dp.toPx()
                )
            }
            for (i in 0 until 9) {
                val center = nodeCenter(i)
                val isActive = i in selected
                drawCircle(
                    color = if (isActive) activeColor else dotColor.copy(alpha = 0.35f),
                    radius = if (isActive) 14.dp.toPx() else 9.dp.toPx(),
                    center = center
                )
                if (isActive) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.25f),
                        radius = 26.dp.toPx(),
                        center = center
                    )
                }
            }
        }
    }
}
