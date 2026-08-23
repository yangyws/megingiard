package com.stormpanda.megingiard.session

import com.stormpanda.megingiard.AppLog

private const val TAG = "PpssppDetector"

/**
 * Detector implementation for PPSSPP (PSP Emulator).
 * Reads `ppsspp.ini` over privileged socket (or fallback paths) to parse active game.
 */
object PpssppDetector : EmulatorDetector {
    override val supportedPackages: Set<String> =
        setOf(
            "org.ppsspp.ppsspp",
            "org.ppsspp.ppssppgold",
        )

    override val systemId: String = "psp"

    private fun getCandidatePaths(packageName: String): List<String> =
        listOf(
            "/storage/emulated/0/PSP/SYSTEM/ppsspp.ini",
            "/sdcard/PSP/SYSTEM/ppsspp.ini",
            "/storage/emulated/0/Android/data/$packageName/files/PSP/SYSTEM/ppsspp.ini",
            "/sdcard/Android/data/$packageName/files/PSP/SYSTEM/ppsspp.ini",
            "/storage/6914-318F/PSP/SYSTEM/ppsspp.ini",
        )

    override suspend fun detectActiveSession(packageName: String): ActiveGameSession? {
        if (!supportedPackages.contains(packageName)) return null

        val candidatePaths = getCandidatePaths(packageName)
        for (path in candidatePaths) {
            val iniContent = ProcessCmdlineProvider.readTextFile(path)
            if (!iniContent.isNullOrBlank()) {
                val session = PpssppIniParser.parseMostRecentSession(packageName, iniContent)
                if (session != null) {
                    AppLog.i(TAG, "Resolved active session via '$path': ${session.gameTitle} (${session.systemId})")
                    return session
                }
            }
        }

        AppLog.d(TAG, "No ppsspp.ini history could be parsed for $packageName, returning default session")
        return ActiveGameSession(
            packageName = packageName,
            romPath = null,
            gameTitle = "PPSSPP",
            systemId = "psp",
            coreOrBackend = "PPSSPP",
        )
    }
}
