package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SettingsRepository
import com.example.ui.editor.photo.AdjustParameter
import com.example.ui.editor.photo.MarkupTextItem
import com.example.ui.editor.photo.NormalizedPoint
import com.example.ui.editor.photo.PhotoAspectRatio
import com.example.ui.editor.photo.PhotoEditorViewModel
import com.example.ui.editor.photo.PhotoFilterPreset
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
class PhotoEditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: PhotoEditorViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = PhotoEditorViewModel(context, SettingsRepository(context))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isNeutralWithNoChangesOrUndoHistory() {
        assertFalse(viewModel.hasUnsavedChanges.value)
        assertFalse(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)
        assertEquals(PhotoFilterPreset.ORIGINAL, viewModel.state.value.filter)
    }

    @Test
    fun adjustmentsAndUndoRedo_pushSingleHistoryEntryPerCommit() {
        viewModel.previewAdjustment(AdjustParameter.BRIGHTNESS, 15f)
        viewModel.previewAdjustment(AdjustParameter.BRIGHTNESS, 40f)
        // Before commit, state reflects live preview but undo stack stays empty
        assertEquals(40f, viewModel.state.value.adjustments.brightness, 0.01f)
        assertFalse(viewModel.canUndo.value)

        viewModel.commitAdjustment()
        assertTrue(viewModel.hasUnsavedChanges.value)
        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)

        viewModel.undo()
        assertEquals(0f, viewModel.state.value.adjustments.brightness, 0.01f)
        assertFalse(viewModel.canUndo.value)
        assertTrue(viewModel.canRedo.value)

        viewModel.redo()
        assertEquals(40f, viewModel.state.value.adjustments.brightness, 0.01f)
        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)
    }

    @Test
    fun toggleAutoEnhance_andCropAspectRatio_updateStateCorrectly() {
        viewModel.toggleAutoEnhance()
        assertTrue(viewModel.state.value.tools.autoEnhanced)
        assertEquals(PhotoFilterPreset.VIVID, viewModel.state.value.filter)

        viewModel.toggleAutoEnhance()
        assertFalse(viewModel.state.value.tools.autoEnhanced)
        assertEquals(PhotoFilterPreset.ORIGINAL, viewModel.state.value.filter)

        viewModel.selectAspectRatio(PhotoAspectRatio.SQUARE, previewWidth = 1600, previewHeight = 900)
        val crop = viewModel.state.value.crop
        assertEquals(PhotoAspectRatio.SQUARE, crop.aspectRatio)
        assertTrue(crop.left > 0f && crop.right < 1f)
    }

    @Test
    fun markupAndMagicEraser_addUpdateAndClearCleanly() {
        viewModel.addMarkupStroke(listOf(NormalizedPoint(0.1f, 0.1f), NormalizedPoint(0.8f, 0.8f)))
        assertEquals(1, viewModel.state.value.markupStrokes.size)

        val text = MarkupTextItem(id = "t1", text = "Hello")
        viewModel.addOrUpdateMarkupText(text)
        assertEquals(1, viewModel.state.value.markupTexts.size)

        viewModel.deleteMarkupText("t1")
        assertTrue(viewModel.state.value.markupTexts.isEmpty())

        viewModel.addMagicEraserStroke(listOf(NormalizedPoint(0.2f, 0.3f), NormalizedPoint(0.4f, 0.5f)))
        assertEquals(1, viewModel.state.value.tools.eraserStrokes.size)

        viewModel.clearMagicEraser()
        assertTrue(viewModel.state.value.tools.eraserStrokes.isEmpty())
    }
}
