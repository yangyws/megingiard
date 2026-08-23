package com.stormpanda.megingiard.translation

import com.stormpanda.megingiard.AppLog
import java.io.File
import java.io.RandomAccessFile

private const val TAG = "MemoryReader"

/**
 * Interface for reading raw memory bytes from target process space.
 */
interface MemoryReader {
    fun findPid(packageName: String): Int?
    fun readBytes(pid: Int, address: Long, length: Int): ByteArray?
}

/**
 * Direct process memory reader via Linux /proc/[pid]/mem.
 * Operates when running with sufficient process permissions (e.g. under Privileged Mode shell UID).
 */
class ProcMemReader : MemoryReader {

    override fun findPid(packageName: String): Int? {
        val procDir = File("/proc")
        val pidDirs = procDir.listFiles { file -> file.isDirectory && file.name.all { it.isDigit() } } ?: return null

        for (dir in pidDirs) {
            val cmdlineFile = File(dir, "cmdline")
            if (cmdlineFile.exists() && cmdlineFile.canRead()) {
                try {
                    val cmdline = cmdlineFile.readText().trim().trimEnd(0.toChar())
                    if (cmdline.equals(packageName, ignoreCase = true) || cmdline.startsWith("$packageName:")) {
                        val pid = dir.name.toIntOrNull()
                        if (pid != null) {
                            AppLog.d(TAG, "Found PID=$pid for package='$packageName'")
                            return pid
                        }
                    }
                } catch (e: Exception) {
                    // Ignore inaccessible / ephemeral process entries
                }
            }
        }
        return null
    }

    override fun readBytes(pid: Int, address: Long, length: Int): ByteArray? {
        if (pid <= 0 || length <= 0) return null
        val memFile = File("/proc/$pid/mem")
        if (!memFile.exists()) {
            AppLog.w(TAG, "Process mem file not found for PID=$pid")
            return null
        }

        return try {
            RandomAccessFile(memFile, "r").use { raf ->
                raf.seek(address)
                val buffer = ByteArray(length)
                val read = raf.read(buffer)
                if (read > 0) {
                    if (read == length) buffer else buffer.copyOf(read)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed reading memory at 0x${address.toString(16)} for PID=$pid: ${e.message}")
            null
        }
    }
}
