package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SettingsRepository
import com.example.ui.editor.video.VideoEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VideoEditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: VideoEditorViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = VideoEditorViewModel(context, SettingsRepository(context))
        viewModel.initialize(
            uri = Uri.parse("content://media/external/video/media/42"),
            displayName = "clip.mp4",
            initialDurationMs = 10_000L
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun trimPreviewAndCommit_updatesTrimDragFlagAndUndoHistory() {
        assertFalse(viewModel.isTrimDragging.value)

        viewModel.previewTrimSelectedClip(startMs = 1_000L, endMs = 8_000L)
        assertTrue(viewModel.isTrimDragging.value)
        assertEquals(7_000L, viewModel.state.value.selectedClip?.effectiveDurationMs)

        viewModel.commitSliderChange()
        assertFalse(viewModel.isTrimDragging.value)
        assertTrue(viewModel.hasUnsavedChanges.value)
        assertTrue(viewModel.canUndo.value)

        viewModel.undo()
        assertEquals(10_000L, viewModel.state.value.selectedClip?.effectiveDurationMs)
    }

    @Test
    fun splitMoveAndDeleteClips_maintainsValidTimelineConstraints() {
        viewModel.updatePlayhead(4_000L)
        viewModel.splitAtPlayhead()
        assertEquals(2, viewModel.state.value.clips.size)

        val firstIdBeforeMove = viewModel.state.value.clips[0].id
        val secondIdBeforeMove = viewModel.state.value.clips[1].id
        viewModel.moveClip(0, 1)
        assertEquals(secondIdBeforeMove, viewModel.state.value.clips[0].id)
        assertEquals(firstIdBeforeMove, viewModel.state.value.clips[1].id)

        // Delete one clip -> 1 clip remains
        viewModel.selectClip(firstIdBeforeMove)
        viewModel.deleteSelectedClip()
        assertEquals(1, viewModel.state.value.clips.size)

        // Cannot delete last remaining clip
        viewModel.deleteSelectedClip()
        assertEquals(1, viewModel.state.value.clips.size)
    }

    @Test
    fun speedAndSegmentSpeed_splitAndScaleEffectiveDuration() {
        viewModel.setSelectedClipSpeed(2f)
        assertEquals(5_000L, viewModel.state.value.selectedClip?.effectiveDurationMs)

        viewModel.setSelectedClipSpeed(1f)
        viewModel.applySpeedToSegment(
            segStartOffsetMs = 2_000L,
            segEndOffsetMs = 6_000L,
            speed = 2f
        )
        assertEquals(3, viewModel.state.value.clips.size)
        // 2000ms @ 1x + 4000ms @ 2x (2000ms) + 4000ms @ 1x = 8000ms total
        assertEquals(8_000L, viewModel.state.value.totalDurationMs)
    }
}
