package com.stormpanda.megingiard.macropad

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stormpanda.megingiard.AppLog
import com.stormpanda.megingiard.R
import com.stormpanda.megingiard.ui.GamepadActionCard
import com.stormpanda.megingiard.ui.GamepadInfoBox
import com.stormpanda.megingiard.ui.LocalAppColors
import com.stormpanda.megingiard.ui.firstDeckItem
import com.stormpanda.megingiard.ui.primaryOverlayFocusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "LocalImagePickerSubPage"

private const val LIP_GRID_COLUMNS = 4
private val LIP_CELL_CORNER = 8.dp
private val LIP_CELL_SHAPE = RoundedCornerShape(LIP_CELL_CORNER)
private val LIP_GRID_SPACING = 6.dp
private val LIP_GRID_VERTICAL_PADDING = 4.dp
private val LIP_GRID_HEIGHT = 280.dp
private const val LIP_THUMB_SIZE_PX = 256
private const val LIP_MAX_IMAGES_QUERY = 120

internal data class LocalImageEntry(
    val uri: Uri,
    val displayName: String,
    val dateModified: Long,
)

@Composable
internal fun LocalImagePickerSubPageContent(
    onSelectImage: (Uri) -> Unit,
    onOpenSteamGridDb: () -> Unit,
    onOpenSystemPicker: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    var images by remember { mutableStateOf<List<LocalImageEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        images = withContext(Dispatchers.IO) {
            queryLocalImages(context)
        }
        isLoading = false
    }

    // ── Quick action shortcuts ─────────────────────────────────────────────
    GamepadActionCard(
        title = stringResource(R.string.button_image_picker_open_steamgriddb),
        description = stringResource(R.string.button_image_picker_open_steamgriddb_desc),
        icon = Icons.Rounded.TravelExplore,
        onClick = onOpenSteamGridDb,
        modifier = Modifier.firstDeckItem(),
    )

    GamepadActionCard(
        title = stringResource(R.string.button_image_picker_open_system),
        description = stringResource(R.string.button_image_picker_open_system_desc),
        icon = Icons.Rounded.FolderOpen,
        onClick = onOpenSystemPicker,
    )

    // ── Local images grid ──────────────────────────────────────────────────
    if (images.isEmpty() && !isLoading) {
        GamepadInfoBox(
            text = stringResource(R.string.button_image_picker_empty),
            description = stringResource(R.string.button_image_picker_empty_desc),
            icon = Icons.Rounded.Image,
        )
    } else {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(LIP_GRID_HEIGHT),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(LIP_GRID_COLUMNS),
                contentPadding = PaddingValues(vertical = LIP_GRID_VERTICAL_PADDING),
                horizontalArrangement = Arrangement.spacedBy(LIP_GRID_SPACING),
                verticalArrangement = Arrangement.spacedBy(LIP_GRID_SPACING),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(images, key = { it.uri.toString() }) { entry ->
                    LocalImageThumbnailTile(
                        entry = entry,
                        onClick = { onSelectImage(entry.uri) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalImageThumbnailTile(
    entry: LocalImageEntry,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    var bitmap by remember(entry.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(entry.uri) {
        bitmap = withContext(Dispatchers.IO) {
            loadThumbnailBitmap(context, entry.uri)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(LIP_CELL_SHAPE)
                .background(colors.surface)
                .border(
                    width = 1.dp,
                    color = colors.subduedBorder,
                    shape = LIP_CELL_SHAPE,
                ).primaryOverlayFocusable(
                    onClick = onClick,
                    shape = LIP_CELL_SHAPE,
                ),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = entry.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Image(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = entry.displayName,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

private fun queryLocalImages(context: Context): List<LocalImageEntry> {
    val list = mutableListOf<LocalImageEntry>()
    val seenPaths = mutableSetOf<String>()

    // 1. Query MediaStore Images
    runCatching {
        val projection =
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_MODIFIED,
            )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        val cursor =
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder,
            )
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

            var count = 0
            while (it.moveToNext() && count < LIP_MAX_IMAGES_QUERY) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn) ?: "Image_$id"
                val date = it.getLong(dateColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                list.add(LocalImageEntry(contentUri, name, date))
                seenPaths.add(contentUri.toString())
                count++
            }
        }
    }.onFailure { err ->
        AppLog.w(TAG, "queryLocalImages MediaStore failed: ${err.message}")
    }

    // 2. Scan standard directories directly (Screenshots, Pictures, Downloads) as fallback
    runCatching {
        val scanDirs =
            listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots"),
            )

        val imageExtensions = setOf("png", "jpg", "jpeg", "webp", "bmp", "gif")
        for (dir in scanDirs) {
            if (!dir.exists() || !dir.isDirectory) continue
            val files =
                dir.listFiles { file ->
                    file.isFile && file.extension.lowercase() in imageExtensions
                } ?: continue

            for (file in files) {
                val uri = Uri.fromFile(file)
                if (seenPaths.add(uri.toString())) {
                    list.add(
                        LocalImageEntry(
                            uri = uri,
                            displayName = file.name,
                            dateModified = file.lastModified() / 1000L,
                        ),
                    )
                }
            }
        }
    }.onFailure { err ->
        AppLog.w(TAG, "queryLocalImages file scan failed: ${err.message}")
    }

    return list.sortedByDescending { it.dateModified }
}

private fun loadThumbnailBitmap(
    context: Context,
    uri: Uri,
): Bitmap? =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == "content") {
            context.contentResolver.loadThumbnail(uri, Size(LIP_THUMB_SIZE_PX, LIP_THUMB_SIZE_PX), null)
        } else {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            stream.use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
            }
        }
    }.getOrNull()
