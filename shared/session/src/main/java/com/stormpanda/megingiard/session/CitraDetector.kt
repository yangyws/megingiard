package com.stormpanda.megingiard.session

import com.stormpanda.megingiard.AppLog
import java.net.URLDecoder

private const val TAG = "CitraDetector"

/**
 * Detector implementation for Citra / Lime3DS Nintendo 3DS emulators.
 */
object CitraDetector : EmulatorDetector {
    override val supportedPackages: Set<String> =
        setOf(
            "org.citra.emu",
            "org.citra.citra_emu",
            "org.citra.citra_emu.canary",
            "org.citra.citra_emu.ant",
            "io.github.lime3ds.android",
            "org.mandarine.mandarine",
            "org.cytrus.cytrus",
            "org.azahar.emu",
        )

    override val systemId: String = "3ds"

    private fun getCandidatePaths(packageName: String): List<String> {
        val logName = if (packageName.contains("lime3ds")) "lime3ds_log.txt" else "citra_log.txt"
        return listOf(
            "/storage/emulated/0/citra-emu/config/config.ini",
            "/sdcard/citra-emu/config/config.ini",
            "/storage/emulated/0/lime3ds-emu/config/config.ini",
            "/sdcard/lime3ds-emu/config/config.ini",
            "/storage/emulated/0/Android/data/$packageName/files/log/$logName",
            "/sdcard/Android/data/$packageName/files/log/$logName",
        )
    }

    override suspend fun detectActiveSession(packageName: String): ActiveGameSession? {
        if (!supportedPackages.contains(packageName)) return null

        val candidatePaths = getCandidatePaths(packageName)
        for (path in candidatePaths) {
            val content = ProcessCmdlineProvider.readTextFile(path)
            if (!content.isNullOrBlank()) {
                val session = parseSessionFromContent(packageName, content)
                if (session != null) {
                    AppLog.i(TAG, "Resolved active session via '$path': ${session.gameTitle} (${session.systemId})")
                    return session
                }
            }
        }

        return ActiveGameSession(
            packageName = packageName,
            romPath = null,
            gameTitle = "Citra",
            systemId = "3ds",
            coreOrBackend = "Citra",
        )
    }

    internal fun parseSessionFromContent(
        packageName: String,
        content: String,
    ): ActiveGameSession? {
        for (line in content.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.contains("Loading ", ignoreCase = true) || trimmed.contains("Opening file: ", ignoreCase = true)) {
                val rawPath = if (trimmed.contains("Loading ", ignoreCase = true)) trimmed.substringAfter("Loading ") else trimmed.substringAfter("Opening file: ")
                val cleanPath = rawPath.substringBefore(" (").trim()
                if (cleanPath.isNotBlank() && (cleanPath.endsWith(".3ds", ignoreCase = true) || cleanPath.endsWith(".cia", ignoreCase = true) || cleanPath.endsWith(".cxi", ignoreCase = true))) {
                    val decoded = try { URLDecoder.decode(cleanPath, "UTF-8") } catch (_: Exception) { cleanPath }
                    val fileName = decoded.substringAfterLast('/').substringAfterLast('\\')
                    val title = fileName.substringBeforeLast('.').trim()
                    if (title.isNotBlank()) {
                        return ActiveGameSession(
                            packageName = packageName,
                            romPath = decoded,
                            gameTitle = title,
                            systemId = "3ds",
                            coreOrBackend = "Citra",
                        )
                    }
                }
            }
        }
        return null
    }
}
