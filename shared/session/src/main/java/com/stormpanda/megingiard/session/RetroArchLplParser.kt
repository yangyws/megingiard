package com.stormpanda.megingiard.session

import com.stormpanda.megingiard.AppLog

private const val TAG = "RetroArchLplParser"

private val EXT_SNES = setOf("sfc", "smc", "fig")
private val EXT_N64 = setOf("n64", "z64", "v64")
private val EXT_GBC = setOf("gb", "gbc")
private val EXT_NES = setOf("nes", "fds")
private val EXT_PS1 = setOf("pbp", "chd", "cue")
private val EXT_GENESIS = setOf("md", "gen", "smd")

/**
 * Pure Kotlin parser for RetroArch `content_history.lpl` and system playlist JSON files.
 */
object RetroArchLplParser {
    fun parseMostRecentSession(
        packageName: String,
        jsonContent: String,
    ): ActiveGameSession? {
        if (jsonContent.isBlank()) return null
        return try {
            val itemsIndex = jsonContent.indexOf("\"items\"")
            if (itemsIndex == -1) return null
            val firstBraceIndex = jsonContent.indexOf('{', itemsIndex)
            if (firstBraceIndex == -1) return null
            val closeBraceIndex = jsonContent.indexOf('}', firstBraceIndex)
            if (closeBraceIndex == -1 || closeBraceIndex <= firstBraceIndex) return null

            val firstItemBlock = jsonContent.substring(firstBraceIndex, closeBraceIndex + 1)

            val path = extractJsonField(firstItemBlock, "path")
            val label = extractJsonField(firstItemBlock, "label")
            val corePath = extractJsonField(firstItemBlock, "core_path")
            val coreName = extractJsonField(firstItemBlock, "core_name")

            if (path == null) return null

            val gameTitle = deriveGameTitle(label, path) ?: return null
            val systemId = resolveSystemId(path, corePath, coreName)

            ActiveGameSession(
                packageName = packageName,
                romPath = path,
                gameTitle = gameTitle,
                systemId = systemId,
                coreOrBackend = coreName ?: corePath ?: "unknown",
            )
        } catch (e: Exception) {
            AppLog.w(TAG, "parseMostRecentSession: failed to parse LPL JSON - $e")
            null
        }
    }

    private fun extractJsonField(
        json: String,
        fieldName: String,
    ): String? {
        val pattern = Regex(""""$fieldName"\s*:\s*"(.*?)(?<!\\)"""")
        val match = pattern.find(json) ?: return null
        val value = match.groupValues[1]
        return value.replace("\\\"", "\"").replace("\\\\", "\\").takeIf { it.isNotBlank() }
    }

    fun deriveGameTitle(
        label: String?,
        path: String?,
    ): String? {
        if (!label.isNullOrBlank() && label != "DETECT") {
            return label.trim()
        }
        if (!path.isNullOrBlank()) {
            val fileName = path.substringAfterLast('/').substringAfterLast('\\')
            val nameWithoutExt = fileName.substringBeforeLast('.')
            if (nameWithoutExt.isNotBlank()) {
                return nameWithoutExt.trim()
            }
        }
        return null
    }

    fun resolveSystemId(
        path: String?,
        corePath: String?,
        coreName: String?,
    ): String {
        val ext = path?.substringAfterLast('.', "")?.lowercase() ?: ""
        val core = (coreName ?: corePath ?: "").lowercase()
        val pathLower = (path ?: "").lowercase()

        return when {
            // 1. Core-based detection (most accurate)
            core.contains("swanstation") || core.contains("duckstation") ||
                core.contains("pcsx") || core.contains("beetle_psx") ||
                core.contains("mednafen_psx") || core.contains("playstation") ||
                core.contains("psx") || core.contains("ps1") -> "ps1"

            core.contains("snes9x") || core.contains("bsnes") ||
                core.contains("mesen-s") || core.contains("snes") ||
                core.contains("super nintendo") -> "snes"

            core.contains("mgba") || core.contains("vba") ||
                core.contains("gpsp") || core.contains("gameboy advance") -> "gba"

            core.contains("gambatte") || core.contains("sameboy") ||
                core.contains("gearboy") || core.contains("tgbdual") -> "gbc"

            core.contains("fceu") || core.contains("nestopia") ||
                core.contains("mesen") || core.contains("quicknes") -> "nes"

            core.contains("mupen") || core.contains("parallel_n64") ||
                core.contains("n64") -> "n64"

            core.contains("melonds") || core.contains("desmume") -> "nds"

            core.contains("ppsspp") -> "psp"

            core.contains("pcsx2") || core.contains("play!") -> "ps2"

            core.contains("genesis_plus_gx") || core.contains("picodrive") ||
                core.contains("blastem") || core.contains("genesis") ||
                core.contains("megadrive") -> "genesis"

            core.contains("flycast") || core.contains("dreamcast") -> "dreamcast"

            core.contains("yabause") || core.contains("kronos") ||
                core.contains("beetle_saturn") || core.contains("saturn") -> "saturn"

            core.contains("fbneo") || core.contains("mame") ||
                core.contains("arcade") -> "arcade"

            core.contains("mednafen_pce") || core.contains("pcfx") ||
                core.contains("beetle_pce") -> "pce"

            // 2. Folder path-based detection (handles .zip/.7z ROMs in organized folders)
            pathLower.contains("/ps1/") || pathLower.contains("/psx/") || pathLower.contains("/playstation/") -> "ps1"
            pathLower.contains("/ps2/") || pathLower.contains("/ps2_games/") -> "ps2"
            pathLower.contains("/psp/") -> "psp"
            pathLower.contains("/gba/") -> "gba"
            pathLower.contains("/gbc/") || pathLower.contains("/gb/") -> "gbc"
            pathLower.contains("/snes/") || pathLower.contains("/sfc/") -> "snes"
            pathLower.contains("/nes/") || pathLower.contains("/fc/") -> "nes"
            pathLower.contains("/n64/") -> "n64"
            pathLower.contains("/nds/") -> "nds"
            pathLower.contains("/3ds/") -> "3ds"
            pathLower.contains("/gamecube/") || pathLower.contains("/gc/") || pathLower.contains("/wii/") -> "gc"
            pathLower.contains("/dreamcast/") || pathLower.contains("/dc/") -> "dreamcast"
            pathLower.contains("/saturn/") || pathLower.contains("/ss/") -> "saturn"
            pathLower.contains("/genesis/") || pathLower.contains("/megadrive/") || pathLower.contains("/md/") -> "genesis"

            // 3. Extension-based fallback
            ext in setOf("sfc", "smc", "fig") -> "snes"
            ext in setOf("n64", "z64", "v64") -> "n64"
            ext == "gba" -> "gba"
            ext in setOf("gb", "gbc") -> "gbc"
            ext in setOf("nes", "fds", "unf") -> "nes"
            ext in setOf("pbp", "chd", "cue") -> "ps1"
            ext in setOf("md", "gen", "smd") -> "genesis"
            ext == "nds" -> "nds"
            else -> "retroarch"
        }
    }
}
