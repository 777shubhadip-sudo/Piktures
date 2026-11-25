package com.example.ui.editor.video

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

@OptIn(UnstableApi::class)
object VideoExporter {

    sealed class ExportOutcome {
        data class Success(val file: File) : ExportOutcome()
        data class Failure(val message: String) : ExportOutcome()
    }

    suspend fun exportComposition(
        context: Context,
        state: VideoEditState,
        resolution: VideoExportResolution,
        onProgress: (Float) -> Unit
    ): ExportOutcome {
        val outputFile = File(context.cacheDir, "export_${System.currentTimeMillis()}.mp4")
        var progressJob: Job? = null

        return try {
            // Prepare all clips, GL effects, audio processors, and overlays on a background thread
            val composition = withContext(Dispatchers.Default) {
                val videoItems = state.clips.map { clip ->
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(clip.uriString))
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(clip.trimStartMs.coerceAtLeast(0L))
                                .setEndPositionMs(clip.trimEndMs.coerceAtLeast(clip.trimStartMs + 200L))
                                .build()
                        )
                        .build()

                    val videoEffects = VideoEffectsFactory.buildClipVideoEffects(
                        clip = clip,
                        aspectRatio = state.aspectRatio,
                        colorAdjustments = state.colorAdjustments,
                        includeSpeedEffect = true
                    )
                    val audioProcessors = VideoEffectsFactory.buildClipAudioProcessors(
                        speed = clip.speed,
                        originalVolume = state.originalVolume
                    )

                    EditedMediaItem.Builder(mediaItem)
                        .setRemoveAudio(state.originalVolume <= 0.01f)
                        .setEffects(Effects(audioProcessors, videoEffects))
                        .build()
                }

                if (videoItems.isEmpty()) return@withContext null

                val videoSequence = EditedMediaItemSequence.Builder(videoItems).build()
                val sequences = ArrayList<EditedMediaItemSequence>(2).apply { add(videoSequence) }

                val music = state.selectedMusic
                if (music != null && state.musicVolume > 0.01f) {
                    val musicMediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(music.uriString))
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(music.trimStartMs.coerceAtLeast(0L))
                                .setEndPositionMs(music.trimEndMs.coerceAtLeast(music.trimStartMs + 300L))
                                .build()
                        )
                        .build()

                    val musicEdited = EditedMediaItem.Builder(musicMediaItem)
                        .setEffects(
                            Effects(
                                listOf(VideoEffectsFactory.GainAudioProcessor(state.musicVolume)),
                                emptyList()
                            )
                        )
                        .build()

                    val musicSeq = EditedMediaItemSequence.Builder(listOf(musicEdited))
                        .setIsLooping(true)
                        .build()
                    sequences.add(musicSeq)
                }

                val compEffects = VideoEffectsFactory.buildCompositionEffects(
                    overlays = state.textOverlays,
                    resolution = resolution,
                    aspectRatio = state.aspectRatio
                )

                Composition.Builder(sequences)
                    .setEffects(Effects(emptyList(), compEffects))
                    .build()
            } ?: return ExportOutcome.Failure("No clips to export")

            withContext(Dispatchers.Main) {
                val progressHolder = ProgressHolder()
                val encoderFactory = DefaultEncoderFactory.Builder(context)
                    .setEnableFallback(true)
                    .build()

                suspendCancellableCoroutine<ExportOutcome> { cont: CancellableContinuation<ExportOutcome> ->
                    val transformer = Transformer.Builder(context)
                        .setLooper(Looper.getMainLooper())
                        .setEncoderFactory(encoderFactory)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                progressJob?.cancel()
                                onProgress(1f)
                                if (cont.isActive) {
                                    cont.resume(ExportOutcome.Success(outputFile))
                                }
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException
                            ) {
                                progressJob?.cancel()
                                runCatching { outputFile.delete() }
                                if (cont.isActive) {
                                    cont.resume(
                                        ExportOutcome.Failure(
                                            exportException.localizedMessage ?: "Export failed"
                                        )
                                    )
                                }
                            }
                        })
                        .build()

                    transformer.start(composition, outputFile.absolutePath)

                    progressJob = launch(Dispatchers.Main) {
                        while (isActive && cont.isActive) {
                            val stateCode = transformer.getProgress(progressHolder)
                            if (stateCode == Transformer.PROGRESS_STATE_AVAILABLE) {
                                onProgress((progressHolder.progress / 100f).coerceIn(0f, 0.99f))
                            }
                            delay(160L)
                        }
                    }

                    cont.invokeOnCancellation {
                        progressJob?.cancel()
                        runCatching { transformer.cancel() }
                        runCatching { outputFile.delete() }
                    }
                }
            }
        } catch (t: Throwable) {
            progressJob?.cancel()
            runCatching { outputFile.delete() }
            ExportOutcome.Failure(t.localizedMessage ?: "Unable to export video")
        }
    }
}
