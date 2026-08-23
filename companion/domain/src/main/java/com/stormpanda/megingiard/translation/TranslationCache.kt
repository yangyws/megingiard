package com.stormpanda.megingiard.translation

import com.stormpanda.megingiard.AppLog

private const val TAG = "TranslationCache"
private const val DEFAULT_MAX_ENTRIES = 500

/**
 * In-memory thread-safe LRU Cache for translation results.
 */
class TranslationCache(private val maxEntries: Int = DEFAULT_MAX_ENTRIES) {

    private val cache = object : LinkedHashMap<String, String>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > maxEntries
        }
    }

    private val lock = Any()

    private fun buildKey(text: String, sourceLang: String, targetLang: String): String {
        return "$sourceLang->$targetLang:${text.trim()}"
    }

    fun get(text: String, sourceLang: String, targetLang: String): String? {
        val key = buildKey(text, sourceLang, targetLang)
        synchronized(lock) {
            val hit = cache[key]
            if (hit != null) {
                AppLog.d(TAG, "Cache HIT for '$key'")
            }
            return hit
        }
    }

    fun put(text: String, sourceLang: String, targetLang: String, translated: String) {
        val key = buildKey(text, sourceLang, targetLang)
        synchronized(lock) {
            cache[key] = translated
            AppLog.d(TAG, "Cache PUT for '$key' (current size=${cache.size})")
        }
    }

    fun clear() {
        synchronized(lock) {
            cache.clear()
            AppLog.d(TAG, "Cache cleared")
        }
    }

    val size: Int
        get() = synchronized(lock) { cache.size }
}
