package com.example.ui.editor.video

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object VideoAudioSynthesizer {

    private data class BuiltInSpec(
        val id: String,
        val title: String,
        val baseHz: Double,
        val bpm: Double,
        val brightness: Double
    )

    private val specs = listOf(
        BuiltInSpec("builtin_horizon", "Ambient Horizon", 220.0, 76.0, 0.45),
        BuiltInSpec("builtin_lofi", "Lo-Fi Pulse", 196.0, 84.0, 0.30),
        BuiltInSpec("builtin_acoustic", "Golden Acoustic", 261.63, 96.0, 0.60),
        BuiltInSpec("builtin_drive", "Upbeat Drive", 293.66, 122.0, 0.78)
    )

    suspend fun ensureBuiltInTracks(context: Context): List<VideoAudioTrack> = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "lumina_music").apply { mkdirs() }
        val durationSec = 12
        val sampleRate = 22050

        specs.map { spec ->
            val file = File(dir, "${spec.id}.wav")
            if (!file.exists() || file.length() < 1024L) {
                writeSynthesizedWav(file, spec, durationSec, sampleRate)
            }
            VideoAudioTrack(
                id = spec.id,
                title = spec.title,
                uriString = Uri.fromFile(file).toString(),
                isBuiltIn = true,
                durationMs = durationSec * 1000L,
                trimStartMs = 0L,
                trimEndMs = durationSec * 1000L
            )
        }
    }

    private fun writeSynthesizedWav(
        target: File,
        spec: BuiltInSpec,
        durationSec: Int,
        sampleRate: Int
    ) {
        val totalSamples = durationSec * sampleRate
        val dataSize = totalSamples * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1) // PCM
            putShort(1) // Mono
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(dataSize)
        }

        val beatPeriodSec = 60.0 / spec.bpm
        val chordRatios = arrayOf(
            doubleArrayOf(1.0, 1.25, 1.5),
            doubleArrayOf(0.8909, 1.1225, 1.3348),
            doubleArrayOf(1.1225, 1.4983, 1.7818),
            doubleArrayOf(0.75, 1.0, 1.1892)
        )

        val pcmBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate.toDouble()
            val barIdx = ((t / (beatPeriodSec * 2.0)).toInt()) % chordRatios.size
            val chord = chordRatios[barIdx]
            val beatPhase = (t % beatPeriodSec) / beatPeriodSec
            val pulseEnv = 0.42 + 0.58 * exp(-beatPhase * 4.5)

            var sample = 0.0
            for (r in chord) {
                val f = spec.baseHz * r
                sample += sin(2.0 * PI * f * t) * 0.28
                sample += sin(2.0 * PI * f * 2.0 * t) * (0.12 * spec.brightness)
            }
            // Gentle fade-in and fade-out at loop boundaries to avoid clicks
            val loopFade = when {
                t < 0.15 -> t / 0.15
                t > durationSec - 0.15 -> (durationSec - t) / 0.15
                else -> 1.0
            }.coerceIn(0.0, 1.0)

            val value = (sample * pulseEnv * loopFade * 15000.0).toInt().coerceIn(-32767, 32767)
            pcmBuffer.putShort(value.toShort())
        }

        FileOutputStream(target).use { out ->
            out.write(header.array())
            out.write(pcmBuffer.array())
        }
    }
}
