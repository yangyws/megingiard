package com.stormpanda.megingiard.translation

import com.stormpanda.megingiard.AppLog
import kotlinx.serialization.Serializable

private const val TAG = "MemoryDialogueHook"

/**
 * Definition of a memory dialogue hook for extracting in-game text from known RAM offsets.
 */
@Serializable
data class MemoryDialogueHook(
    val id: String,
    val systemId: String,
    val gameTitleRegex: String? = null,
    val titleId: String? = null,
    val textAddressOffset: Long,
    val maxReadLength: Int = 256,
    val encoding: GameTextEncoding = GameTextEncoding.SHIFT_JIS,
    val normLeft: Float = 0.05f,
    val normTop: Float = 0.65f,
    val normRight: Float = 0.95f,
    val normBottom: Float = 0.95f,
)

/**
 * Registry storing pre-configured and user-defined game memory hooks.
 */
object MemoryHookRegistry {

    private val BUILTIN_HOOKS = listOf(
        // Monster Hunter Portable 3rd (PSP) - Sample Quest / Dialogue Buffer Hook
        MemoryDialogueHook(
            id = "mhp3rd_dialogue",
            systemId = "psp",
            gameTitleRegex = ".*Monster Hunter.*|.*モンスターハンター.*",
            titleId = "ULJM05800",
            textAddressOffset = 0x09F20000L,
            maxReadLength = 192,
            encoding = GameTextEncoding.SHIFT_JIS,
        ),
        // Persona 3 Portable (PSP) - Dialogue Buffer Hook
        MemoryDialogueHook(
            id = "p3p_dialogue",
            systemId = "psp",
            gameTitleRegex = ".*Persona 3.*|.*ペルソナ3.*",
            titleId = "ULJM05489",
            textAddressOffset = 0x098A4000L,
            maxReadLength = 256,
            encoding = GameTextEncoding.SHIFT_JIS,
        ),
        // Dragon Quest Monsters (GBA / RetroArch)
        MemoryDialogueHook(
            id = "dqm_gba_dialogue",
            systemId = "gba",
            gameTitleRegex = ".*Dragon Quest.*|.*ドラゴンクエスト.*",
            titleId = null,
            textAddressOffset = 0x02008000L,
            maxReadLength = 128,
            encoding = GameTextEncoding.SHIFT_JIS,
        ),
    )

    private val customHooks = mutableListOf<MemoryDialogueHook>()

    fun findHook(systemId: String, gameTitle: String?, titleId: String?): MemoryDialogueHook? {
        val allHooks = BUILTIN_HOOKS + customHooks

        // 1. Exact match by Title ID
        if (!titleId.isNullOrBlank()) {
            val byTitleId = allHooks.firstOrNull { it.systemId.equals(systemId, ignoreCase = true) && it.titleId.equals(titleId, ignoreCase = true) }
            if (byTitleId != null) {
                AppLog.d(TAG, "Found memory hook by titleId='$titleId': ${byTitleId.id}")
                return byTitleId
            }
        }

        // 2. Regex match by Game Title
        if (!gameTitle.isNullOrBlank()) {
            val byTitle = allHooks.firstOrNull { hook ->
                hook.systemId.equals(systemId, ignoreCase = true) &&
                    hook.gameTitleRegex != null &&
                    Regex(hook.gameTitleRegex, RegexOption.IGNORE_CASE).matches(gameTitle)
            }
            if (byTitle != null) {
                AppLog.d(TAG, "Found memory hook by gameTitle='$gameTitle': ${byTitle.id}")
                return byTitle
            }
        }

        return null
    }

    fun registerCustomHook(hook: MemoryDialogueHook) {
        customHooks.add(hook)
        AppLog.i(TAG, "Registered custom memory hook: ${hook.id}")
    }

    fun clearCustomHooks() {
        customHooks.clear()
    }
}
