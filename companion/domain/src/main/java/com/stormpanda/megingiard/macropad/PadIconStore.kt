package com.stormpanda.megingiard.macropad

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.stormpanda.megingiard.AppLog
import com.stormpanda.megingiard.security.HmacUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

private const val TAG = "PadIconStore"

/**
 * Stores and retrieves button thumbnail images.
 */
object PadIconStore {
    const val ICONS_DIR = "padicons"
    const val THUMBNAIL_MAX_PX = 512

    private const val WEBP_QUALITY = 90
    private const val EXTENSION = ".webp"
    private const val CACHE_ENTRIES = 64
    private const val ASSET_ID_LENGTH = 16

    private val cache = LruCache<String, Bitmap>(CACHE_ENTRIES)

    fun directory(context: Context): File = File(context.filesDir, ICONS_DIR)

    fun relativePath(assetId: String): String = "$ICONS_DIR/$assetId$EXTENSION"

    fun fileFor(
        context: Context,
        assetId: String,
    ): File = File(directory(context), assetId + EXTENSION)

    suspend fun put(
        context: Context,
        bitmap: Bitmap,
    ): String? =
        withContext(Dispatchers.IO) {
            try {
                val thumbnail = downscale(bitmap)
                val bytes =
                    ByteArrayOutputStream().use { out ->
                        if (!thumbnail.compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_QUALITY, out)) {
                            AppLog.e(TAG, "WebP encode failed")
                            return@withContext null
                        }
                        out.toByteArray()
                    }
                if (thumbnail !== bitmap) thumbnail.recycle()

                val assetId = HmacUtil.sha256Hex(bytes).lowercase().take(ASSET_ID_LENGTH)
                val dir = directory(context)
                if (!dir.exists() && !dir.mkdirs()) {
                    AppLog.e(TAG, "Could not create $ICONS_DIR")
                    return@withContext null
                }
                val file = File(dir, assetId + EXTENSION)
                if (!file.exists()) file.writeBytes(bytes)
                AppLog.i(TAG, "Stored pad icon $assetId (${bytes.size} bytes)")
                assetId
            } catch (e: Exception) {
                AppLog.e(TAG, "put failed", e)
                null
            }
        }

    suspend fun load(
        context: Context,
        assetId: String,
    ): Bitmap? {
        cache.get(assetId)?.let { if (!it.isRecycled) return it }
        return withContext(Dispatchers.IO) {
            try {
                val file = fileFor(context, assetId)
                if (!file.exists()) {
                    AppLog.w(TAG, "Missing pad icon $assetId")
                    return@withContext null
                }
                BitmapFactory.decodeFile(file.absolutePath)?.also { cache.put(assetId, it) }
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to decode pad icon $assetId", e)
                null
            }
        }
    }

    suspend fun bytesOf(
        context: Context,
        assetId: String,
    ): ByteArray? =
        withContext(Dispatchers.IO) {
            runCatching { fileFor(context, assetId).takeIf { it.exists() }?.readBytes() }
                .onFailure { AppLog.e(TAG, "Failed to read pad icon $assetId", it) }
                .getOrNull()
        }

    suspend fun putRaw(
        context: Context,
        assetId: String,
        bytes: ByteArray,
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (!isValidAssetId(assetId)) {
                AppLog.w(TAG, "Rejected pad icon id: $assetId")
                return@withContext false
            }
            try {
                val dir = directory(context)
                if (!dir.exists() && !dir.mkdirs()) return@withContext false
                val file = File(dir, assetId + EXTENSION)
                if (!file.exists()) file.writeBytes(bytes)
                true
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to import pad icon $assetId", e)
                false
            }
        }

    suspend fun sweep(
        context: Context,
        referenced: Set<String>,
    ): Int =
        withContext(Dispatchers.IO) {
            try {
                val files = directory(context).listFiles() ?: return@withContext 0
                var removed = 0
                for (file in files) {
                    val id = file.name.removeSuffix(EXTENSION)
                    if (id !in referenced && file.delete()) {
                        cache.remove(id)
                        removed++
                    }
                }
                if (removed > 0) AppLog.i(TAG, "Swept $removed unreferenced pad icon(s)")
                removed
            } catch (e: Exception) {
                AppLog.e(TAG, "Pad icon sweep failed", e)
                0
            }
        }

    fun clearCache() = cache.evictAll()

    private fun downscale(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= THUMBNAIL_MAX_PX || longest == 0) return bitmap
        val scale = THUMBNAIL_MAX_PX.toFloat() / longest
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun isValidAssetId(assetId: String): Boolean =
        assetId.length == ASSET_ID_LENGTH && assetId.all { it in '0'..'9' || it in 'a'..'f' }
}
