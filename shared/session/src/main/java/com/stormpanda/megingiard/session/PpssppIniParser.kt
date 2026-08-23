package com.stormpanda.megingiard.session

import com.stormpanda.megingiard.AppLog
import java.net.URLDecoder

private const val TAG = "PpssppIniParser"

/**
 * Pure Kotlin parser for PPSSPP `ppsspp.ini` configuration files.
 * Extracts the most recent game from the `[Recent]` section.
 */
object PpssppIniParser {
    /**
     * Parses `ppsspp.ini` content and extracts an [ActiveGameSession]
     * corresponding to the most recently played game (`FileName0`).
     */
    fun parseMostRecentSession(
        packageName: String,
        iniContent: String,
    ): ActiveGameSession? {
        if (iniContent.isBlank()) return null
        return try {
            var inRecentSection = false
            var fileName0: String? = null

            for (line in iniContent.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    inRecentSection = trimmed.equals("[Recent]", ignoreCase = true)
                    continue
                }
                if (inRecentSection) {
                    if (trimmed.startsWith("FileName0", ignoreCase = true) && trimmed.contains("=")) {
                        fileName0 = trimmed.substringAfter("=").trim()
                        break
                    }
                }
            }

            if (fileName0.isNullOrBlank()) {
                return ActiveGameSession(
                    packageName = packageName,
                    romPath = null,
                    gameTitle = "PPSSPP",
                    systemId = "psp",
                    coreOrBackend = "PPSSPP",
                )
            }

            val romPath = resolveFilePath(fileName0)
            val gameTitle = deriveTitleFromUri(fileName0) ?: "PPSSPP"

            ActiveGameSession(
                packageName = packageName,
                romPath = romPath,
                gameTitle = gameTitle,
                systemId = "psp",
                coreOrBackend = "PPSSPP",
            )
        } catch (e: Exception) {
            AppLog.w(TAG, "parseMostRecentSession: failed to parse PPSSPP ini - $e")
            null
        }
    }

    private fun resolveFilePath(uriStr: String?): String? {
        if (uriStr.isNullOrBlank()) return null
        if (uriStr.startsWith("/")) return uriStr

        return try {
            val decoded = URLDecoder.decode(uriStr, "UTF-8")
            val rawPath =
                when {
                    decoded.contains("/document/") -> decoded.substringAfter("/document/")
                    decoded.contains("/tree/") -> decoded.substringAfter("/tree/")
                    else -> decoded
                }

            when {
                rawPath.startsWith("/") -> rawPath
                rawPath.startsWith("primary:") -> "/storage/emulated/0/${rawPath.substringAfter("primary:")}"
                rawPath.contains(":") -> "/storage/${rawPath.replaceFirst(":", "/")}"
                else -> rawPath
            }
        } catch (e: Exception) {
            uriStr
        }
    }

    private fun deriveTitleFromUri(uriStr: String?): String? {
        if (uriStr.isNullOrBlank()) return null
        val decoded =
            try {
                URLDecoder.decode(uriStr, "UTF-8")
            } catch (e: Exception) {
                uriStr
            }
        val fileName = decoded.substringAfterLast('/').substringAfterLast('\\')
        val nameWithoutExt = fileName.substringBeforeLast('.')
        return nameWithoutExt.trim().takeIf { it.isNotBlank() }
    }
}
