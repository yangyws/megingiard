package com.stormpanda.megingiard.macropad

import android.net.Uri
import com.stormpanda.megingiard.AppLog
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "ButtonImagePickerManager"

/**
 * Coordinator singleton for button custom image file picker requests between UI overlays
 * (which run in non-Activity Window contexts) and [MainActivity].
 */
object ButtonImagePickerManager {
    private val _pickRequest =
        MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val pickRequest: SharedFlow<Unit> = _pickRequest.asSharedFlow()

    private val _pickedUri = MutableStateFlow<Uri?>(null)
    val pickedUri: StateFlow<Uri?> = _pickedUri.asStateFlow()

    private val _pickedUriFlow =
        MutableSharedFlow<Uri>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val pickedUriFlow: SharedFlow<Uri> = _pickedUriFlow.asSharedFlow()

    fun requestImagePicker() {
        AppLog.d(TAG, "requestImagePicker")
        _pickRequest.tryEmit(Unit)
    }

    fun setPickedUri(uri: Uri?) {
        AppLog.d(TAG, "setPickedUri: $uri")
        _pickedUri.value = uri
        if (uri != null) {
            _pickedUriFlow.tryEmit(uri)
        }
    }

    fun clearPickedUri() {
        _pickedUri.value = null
    }
}
