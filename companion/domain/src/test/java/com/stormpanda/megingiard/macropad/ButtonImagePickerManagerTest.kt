package com.stormpanda.megingiard.macropad

import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ButtonImagePickerManagerTest {
    @After
    fun tearDown() {
        ButtonImagePickerManager.clearPickedUri()
    }

    @Test
    fun requestImagePicker_emitsUnitOnPickRequest() =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)
            var received = false
            val job =
                backgroundScope.launch(testDispatcher) {
                    ButtonImagePickerManager.pickRequest.first()
                    received = true
                }

            ButtonImagePickerManager.requestImagePicker()

            assertEquals(true, received)
            job.cancel()
        }

    @Test
    fun setPickedUri_updatesStateFlowAndClearResetsToNull() {
        val testUri = Uri.parse("content://media/external/images/media/456")

        ButtonImagePickerManager.setPickedUri(testUri)
        assertEquals(testUri, ButtonImagePickerManager.pickedUri.value)

        ButtonImagePickerManager.clearPickedUri()
        assertNull(ButtonImagePickerManager.pickedUri.value)
    }

    @Test
    fun setPickedUri_emitsToPickedUriFlow() =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)
            val testUri = Uri.parse("content://media/external/images/media/789")
            var receivedUri: Uri? = null
            val job =
                backgroundScope.launch(testDispatcher) {
                    receivedUri = ButtonImagePickerManager.pickedUriFlow.first()
                }

            ButtonImagePickerManager.setPickedUri(testUri)

            assertEquals(testUri, receivedUri)
            job.cancel()
        }
}
