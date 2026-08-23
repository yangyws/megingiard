package com.stormpanda.megingiard.session

import com.stormpanda.megingiard.AppLog
import java.net.URLDecoder

private const val TAG = "DolphinDetector"

/**
 * Detector implementation for Dolphin GameCube/Wii emulators.
 */
object DolphinDetector : EmulatorDetector {
    override val supportedPackages: Set<String> =
        setOf(
            "org.dolphinemu.dolphinemu",
            "org.dolphinemu.dolphinemu.debug",
            "org.dolphinemu.mmjr",
            "org.dolphinemu.mmj",
            "org.dolphinemu.mmjr2",
        )

    override val systemId: String = "gc"

    private fun getCandidateIniPaths(packageName: String): List<String> =
        listOf(
            "/storage/emulated/0/dolphin-emu/Config/Dolphin.ini",
            "/sdcard/dolphin-emu/Config/Dolphin.ini",
            "/storage/emulated/0/dolphin-mmjr/Config/Dolphin.ini",
            "/sdcard/dolphin-mmjr/Config/Dolphin.ini",
            "/storage/emulated/0/Android/data/$packageName/files/Config/Dolphin.ini",
            "/sdcard/Android/data/$packageName/files/Config/Dolphin.ini",
        )

    override suspend fun detectActiveSession(packageName: String): ActiveGameSession? {
        if (!supportedPackages.contains(packageName)) return null

        val candidatePaths = getCandidateIniPaths(packageName)
        for (path in candidatePaths) {
            val iniContent = ProcessCmdlineProvider.readTextFile(path)
            if (!iniContent.isNullOrBlank()) {
                val session = parseSessionFromIni(packageName, iniContent)
                if (session != null) {
                    AppLog.i(TAG, "Resolved active session via '$path': ${session.gameTitle} (${session.systemId})")
                    return session
                }
            }
        }

        return ActiveGameSession(
            packageName = packageName,
            romPath = null,
            gameTitle = "Dolphin",
            systemId = "gc",
            coreOrBackend = "Dolphin",
        )
    }

    internal fun parseSessionFromIni(
        packageName: String,
        iniContent: String,
    ): ActiveGameSession? {
        var lastFilename: String? = null
        for (line in iniContent.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("LastFilename", ignoreCase = true) && trimmed.contains("=")) {
                lastFilename = trimmed.substringAfter("=").trim()
                break
            }
        }
        if (lastFilename.isNullOrBlank()) return null
        val decoded = try { URLDecoder.decode(lastFilename, "UTF-8") } catch (_: Exception) { lastFilename }
        val fileName = decoded.substringAfterLast('/').substringAfterLast('\\')
        val title = fileName.substringBeforeLast('.').trim()
        if (title.isBlank()) return null
        val sysId = if (fileName.endsWith(".wbfs", ignoreCase = true) || fileName.contains("wii", ignoreCase = true)) "wii" else "gc"
        return ActiveGameSession(
            packageName = packageName,
            romPath = decoded,
            gameTitle = title,
            systemId = sysId,
            coreOrBackend = "Dolphin",
        )
    }
}
