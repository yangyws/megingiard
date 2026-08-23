package com.stormpanda.megingiard.session

/**
 * Abstraction provider for querying process command lines and privileged text files.
 *
 * Allows process detectors in `:shared:session` to query running process command lines
 * without depending directly on the privileged daemon client in `:companion:domain`.
 */
object ProcessCmdlineProvider {
    @Volatile var runningProcessesProvider: (suspend () -> String?)? = null

    @Volatile var textFileReader: (suspend (String) -> String?)? = null

    suspend fun getRunningProcesses(): String? = runningProcessesProvider?.invoke()

    suspend fun readTextFile(path: String): String? {
        val custom = textFileReader?.invoke(path)
        if (!custom.isNullOrBlank()) return custom
        return try {
            val file = java.io.File(path)
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
