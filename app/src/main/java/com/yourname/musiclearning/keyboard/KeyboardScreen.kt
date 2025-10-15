@file:OptIn(ExperimentalMaterial3Api::class)

package com.yourname.musiclearning.keyboard

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectTapGestures


/**
 * Locks orientation to landscape for the lifetime of this Composable, restoring when disposed.
 */
@Composable
private fun LandscapeOnly(content: @Composable () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val original = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = original
        }
    }
    content()
}

/** Simple PCM synth with one AudioTrack per MIDI note (cached). */
private class NotePlayer {

    private val tracks = mutableMapOf<Int, AudioTrack>()
    private val sampleRate = 44100
    private val seconds = 2.0 // duration of each note sample
    private val attack = 0.01 // seconds
    private val release = 0.15 // seconds

    private fun midiToFreq(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    private fun buildPcm16(midi: Int): ShortArray {
        val n = (seconds * sampleRate).roundToInt()
        val twoPiF = 2.0 * Math.PI * midiToFreq(midi)
        val attackN = (attack * sampleRate).coerceAtLeast(1.0).roundToInt()
        val releaseN = (release * sampleRate).coerceAtLeast(1.0).roundToInt()

        return ShortArray(n) { i ->
            val t = i / sampleRate.toDouble()
            // simple sine
            val sample = kotlin.math.sin(twoPiF * t)
            // very basic ADSR (A ~10ms, D/S ~flat, R ~150ms)
            val env = when {
                i < attackN -> i / attackN.toDouble()
                i > n - releaseN -> (n - i) / releaseN.toDouble()
                else -> 1.0
            }
            (sample * env * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun trackFor(midi: Int): AudioTrack {
        return tracks.getOrPut(midi) {
            val buffer = buildPcm16(midi)
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            @Suppress("DEPRECATION")
            val at = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf, buffer.size * 2),
                AudioTrack.MODE_STATIC
            )
            val byteBuf = java.nio.ByteBuffer.allocate(buffer.size * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            buffer.forEach { s -> byteBuf.putShort(s) }
            at.write(byteBuf.array(), 0, byteBuf.capacity())
            at
        }
    }

    fun noteOn(midi: Int) {
        val at = trackFor(midi)
        at.pause()
        at.flush()
        at.playbackHeadPosition = 0
        at.play()
    }

    fun noteOff(midi: Int) {
        tracks[midi]?.let { at ->
            // Quick stop (we pre-baked a short release into the sample)
            if (at.playState == AudioTrack.PLAYSTATE_PLAYING) at.stop()
            at.playbackHeadPosition = 0
        }
    }

    fun release() {
        tracks.values.forEach {
            try { it.release() } catch (_: Throwable) {}
        }
        tracks.clear()
    }
}

/** Model for a single key on the keyboard. */
private data class KeySpec(
    val midi: Int,
    val isBlack: Boolean,
    val label: String
)

/**
 * Build a 3-octave set starting at C4 (MIDI 60) → B6 (95).
 * You can change startMidi to 48 for C3 if preferred.
 */
private fun buildThreeOctaves(startMidi: Int = 60): List<KeySpec> {
    val names = arrayOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
    return (0 until 36).map { i ->
        val midi = startMidi + i
        val idx = (midi % 12 + 12) % 12
        val name = names[idx]
        KeySpec(
            midi = midi,
            isBlack = name.contains("#"),
            label = name
        )
    }
}

/**
 * Simple landscape screen: header + content area + keyboard docked to bottom.
 */
@Composable
fun KeyboardScreen() {
    LandscapeOnly {
        val keys = remember { buildThreeOctaves(startMidi = 60) } // C4..B6
        val player = remember { NotePlayer() }

        DisposableEffect(Unit) {
            onDispose { player.release() }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Piano Keyboard") }
                )
            }
        ) { inner ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
            ) {
                // Content area (leave empty or add lesson UI)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        "Try tapping or dragging across the keys at the bottom.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Keyboard docked at bottom
                PianoKeyboard(
                    keys = keys,
                    whiteKeyHeight = 220.dp,
                    onNoteOn = { player.noteOn(it) },
                    onNoteOff = { player.noteOff(it) }
                )
            }
        }
    }
}

@Composable
private fun PianoKeyboard(
    keys: List<KeySpec>,
    whiteKeyHeight: Dp,
    onNoteOn: (midi: Int) -> Unit,
    onNoteOff: (midi: Int) -> Unit
) {
    val pressedKeys = remember { mutableStateOf(setOf<Int>()) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(whiteKeyHeight + 16.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 8.dp)
    ) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // 1) TAP: press → highlight, release → clear
                .pointerInput(keys) {
                    detectTapGestures(
                        onPress = { pos ->
                            val midi = keyAtPosition(
                                pos.x, pos.y,
                                size.width.toFloat(), size.height.toFloat(),
                                keys
                            )
                            if (midi != null) {
                                pressedKeys.value = setOf(midi)
                                onNoteOn(midi)
                                try {
                                    // suspend until up/cancel
                                    tryAwaitRelease()
                                } finally {
                                    onNoteOff(midi)
                                    pressedKeys.value = emptySet()
                                }
                            }
                        }
                    )
                }
                // 2) DRAG: slide across keys to change highlight
                .pointerInput(keys) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            keyAtPosition(
                                pos.x, pos.y,
                                size.width.toFloat(), size.height.toFloat(),
                                keys
                            )?.let { midi ->
                                pressedKeys.value = setOf(midi)
                                onNoteOn(midi)
                            }
                        },
                        onDragEnd = {
                            pressedKeys.value.forEach(onNoteOff)
                            pressedKeys.value = emptySet()
                        },
                        onDragCancel = {
                            pressedKeys.value.forEach(onNoteOff)
                            pressedKeys.value = emptySet()
                        }
                    ) { change, _ ->
                        val midi = keyAtPosition(
                            change.position.x, change.position.y,
                            size.width.toFloat(), size.height.toFloat(),
                            keys
                        )
                        val current = pressedKeys.value
                        if (midi != null && midi !in current) {
                            current.forEach(onNoteOff)
                            pressedKeys.value = setOf(midi)
                            onNoteOn(midi)
                        }
                    }
                }
        ) {
            // Draw code unchanged, but force a read to trigger redraws when state changes:
            val pressed = pressedKeys.value

            val W = size.width
            val H = size.height
            val whiteKeys = keys.filter { !it.isBlack }
            val blackKeys = keys.filter { it.isBlack }
            val whiteW = W / whiteKeys.size

            // --- Draw white keys
            whiteKeys.forEachIndexed { index, key ->
                val left = index * whiteW
                val color = if (key.midi in pressed) Color(0xFFFFE5E5) else Color(0xFFFDFDFD)
                drawRect(
                    color = color,
                    topLeft = Offset(left, 0f),
                    size = androidx.compose.ui.geometry.Size(whiteW - 1f, H)
                )
                drawLine(
                    color = Color(0xFFBDBDBD),
                    start = Offset(left + whiteW - 1f, 0f),
                    end = Offset(left + whiteW - 1f, H),
                    strokeWidth = 1f
                )
            }

            // --- Draw black keys
            val whitePerOctave = 7
            val octaveCount = whiteKeys.size / whitePerOctave
            val blackRelPositions = floatArrayOf(0.65f, 1.55f, 3.05f, 3.95f, 4.85f)
            val blackKeyWidth = whiteW * 0.6f
            val blackKeyHeight = H * 0.6f

            var blackIndex = 0
            for (oct in 0 until octaveCount) {
                val octaveLeft = oct * whitePerOctave * whiteW
                for (i in 0 until 5) {
                    if (blackIndex >= blackKeys.size) break
                    val key = blackKeys[blackIndex]
                    val centerX = octaveLeft + blackRelPositions[i] * whiteW
                    val left = centerX - blackKeyWidth / 2f
                    val color = if (key.midi in pressed) Color(0xFFFF7B7B) else Color(0xFF111111)
                    drawRect(
                        color = color,
                        topLeft = Offset(left, 0f),
                        size = androidx.compose.ui.geometry.Size(blackKeyWidth, blackKeyHeight)
                    )
                    blackIndex++
                }
            }
        }


    }
}


/**
 * Hit-testing: prefer black keys where they exist (upper 60% of height), otherwise white by column.
 */
private fun keyAtPosition(x: Float, y: Float, width: Float, height: Float, allKeys: List<KeySpec>): Int? {
    val whites = allKeys.filter { !it.isBlack }
    val whiteW = width / whites.size
    val whiteIndex = (x / whiteW).toInt().coerceIn(0, whites.lastIndex)

    // Calculate if inside a black key footprint
    val H = height
    val blackHeight = H * 0.6f
    if (y <= blackHeight) {
        val inBlack = isOverBlackKey(x, width, whites.size)
        if (inBlack != null) return inBlack.midi
    }
    return whites[whiteIndex].midi
}

/** Compute which black key (if any) is under x. Mirrors the drawing math. */
private fun isOverBlackKey(x: Float, width: Float, whiteCount: Int): KeySpec? {
    val names = arrayOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
    fun build(startMidi: Int) = (0 until 36).map { i ->
        val midi = 60 + i
        val name = names[(midi % 12 + 12) % 12]
        KeySpec(midi, name.contains("#"), name)
    }
    val all = build(60)
    val blacks = all.filter { it.isBlack }

    val whiteW = width / whiteCount
    val whitePerOctave = 7
    val octaveCount = whiteCount / whitePerOctave
    val blackRelPositions = floatArrayOf(0.65f, 1.55f, 3.05f, 3.95f, 4.85f)
    val blackKeyWidth = whiteW * 0.6f

    var idx = 0
    for (oct in 0 until octaveCount) {
        val octaveLeft = oct * whitePerOctave * whiteW
        for (i in 0 until 5) {
            if (idx >= blacks.size) break
            val centerX = octaveLeft + blackRelPositions[i] * whiteW
            val left = centerX - blackKeyWidth / 2f
            val right = centerX + blackKeyWidth / 2f
            if (x in left..right) return blacks[idx]
            idx++
        }
    }
    return null
}