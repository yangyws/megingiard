package com.stormpanda.megingiard.translation

import kotlinx.serialization.Serializable

/**
 * Known text encodings used in video game ROMs and emulator RAM.
 */
@Serializable
enum class GameTextEncoding {
    SHIFT_JIS,
    UTF_8,
    UTF_16_LE,
    ASCII,
}

/**
 * Memory layout specification for an emulator platform.
 */
@Serializable
data class EmulatorMemorySpec(
    val systemId: String,
    val packageNames: List<String>,
    val ramVirtualBase: Long,
    val ramSize: Long,
    val defaultEncoding: GameTextEncoding = GameTextEncoding.SHIFT_JIS,
)

/**
 * Predefined emulator RAM mapping definitions for Tier 0 direct memory text extraction.
 */
object EmulatorMemoryMap {

    val PPSSPP_SPEC = EmulatorMemorySpec(
        systemId = "psp",
        packageNames = listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold"),
        ramVirtualBase = 0x08800000L,
        ramSize = 24L * 1024L * 1024L, // 24MB user space
        defaultEncoding = GameTextEncoding.SHIFT_JIS,
    )

    val CITRA_SPEC = EmulatorMemorySpec(
        systemId = "3ds",
        packageNames = listOf("io.github.lime3ds", "org.citra.citra_emu"),
        ramVirtualBase = 0x20000000L,
        ramSize = 128L * 1024L * 1024L, // 128MB FCRAM
        defaultEncoding = GameTextEncoding.UTF_16_LE,
    )

    val RETROARCH_GBA_SPEC = EmulatorMemorySpec(
        systemId = "gba",
        packageNames = listOf("com.retroarch", "com.retroarch.aarch64", "com.retroarch.ra32"),
        ramVirtualBase = 0x02000000L,
        ramSize = 256L * 1024L, // 256KB EWRAM
        defaultEncoding = GameTextEncoding.SHIFT_JIS,
    )

    val PCSX2_ANDROID_SPEC = EmulatorMemorySpec(
        systemId = "ps2",
        packageNames = listOf("xyz.aethersx2.android", "xyz.nethersx2.android"),
        ramVirtualBase = 0x00000000L,
        ramSize = 32L * 1024L * 1024L, // 32MB Main EE RAM
        defaultEncoding = GameTextEncoding.SHIFT_JIS,
    )

    val DOLPHIN_SPEC = EmulatorMemorySpec(
        systemId = "gc",
        packageNames = listOf("org.dolphinemu.dolphinemu"),
        ramVirtualBase = 0x80000000L,
        ramSize = 24L * 1024L * 1024L, // 24MB MEM1
        defaultEncoding = GameTextEncoding.SHIFT_JIS,
    )

    val ALL_SPECS = listOf(
        PPSSPP_SPEC,
        CITRA_SPEC,
        RETROARCH_GBA_SPEC,
        PCSX2_ANDROID_SPEC,
        DOLPHIN_SPEC,
    )

    fun findSpecByPackage(packageName: String): EmulatorMemorySpec? {
        return ALL_SPECS.firstOrNull { spec ->
            spec.packageNames.any { it.equals(packageName, ignoreCase = true) }
        }
    }

    fun findSpecBySystem(systemId: String): EmulatorMemorySpec? {
        return ALL_SPECS.firstOrNull { it.systemId.equals(systemId, ignoreCase = true) }
    }
}
