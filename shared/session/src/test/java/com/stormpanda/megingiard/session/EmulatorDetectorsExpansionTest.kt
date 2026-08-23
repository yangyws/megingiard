package com.stormpanda.megingiard.session

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmulatorDetectorsExpansionTest {
    @Test
    fun dolphinDetector_parsesLastFilenameFromIni() {
        val ini =
            """
            [General]
            LastFilename = /storage/emulated/0/roms/GameCube/Super%20Smash%20Bros.%20Melee%20(USA).iso
            ISOPaths = 1
            """.trimIndent()

        val session = DolphinDetector.parseSessionFromIni("org.dolphinemu.dolphinemu", ini)

        assertNotNull(session)
        assertEquals("org.dolphinemu.dolphinemu", session?.packageName)
        assertEquals("Super Smash Bros. Melee (USA)", session?.gameTitle)
        assertEquals("gc", session?.systemId)
        assertEquals("/storage/emulated/0/roms/GameCube/Super Smash Bros. Melee (USA).iso", session?.romPath)
    }

    @Test
    fun citraDetector_parsesLoadingLogLine() {
        val log =
            """
            [ 0.123] Frontend <Info> Loading /sdcard/roms/3ds/Pokemon%20Sun.3ds (0004000000164800)...
            """.trimIndent()

        val session = CitraDetector.parseSessionFromContent("io.github.lime3ds.android", log)

        assertNotNull(session)
        assertEquals("io.github.lime3ds.android", session?.packageName)
        assertEquals("Pokemon Sun", session?.gameTitle)
        assertEquals("3ds", session?.systemId)
    }

    @Test
    fun genericStandaloneDetector_detectsKnownPlatforms() = runBlocking {
        // NDS (DraStic)
        val drastic = GenericStandaloneEmulatorDetector.detectActiveSession("com.dsemu.drastic")
        assertNotNull(drastic)
        assertEquals("nds", drastic?.systemId)
        assertEquals("DraStic", drastic?.gameTitle)

        // N64 (Mupen64Plus FZ)
        val mupen = GenericStandaloneEmulatorDetector.detectActiveSession("org.mupen64plusae.v3.fzurita.pro")
        assertNotNull(mupen)
        assertEquals("n64", mupen?.systemId)

        // PS1 (DuckStation)
        val duck = GenericStandaloneEmulatorDetector.detectActiveSession("com.github.stenzek.duckstation")
        assertNotNull(duck)
        assertEquals("ps1", duck?.systemId)

        // Dreamcast (Redream)
        val redream = GenericStandaloneEmulatorDetector.detectActiveSession("io.recompiled.redream")
        assertNotNull(redream)
        assertEquals("dreamcast", redream?.systemId)

        // GBA (My Boy!)
        val myboy = GenericStandaloneEmulatorDetector.detectActiveSession("com.fastemulator.gba")
        assertNotNull(myboy)
        assertEquals("gba", myboy?.systemId)

        // Winlator
        val winlator = GenericStandaloneEmulatorDetector.detectActiveSession("com.winlator")
        assertNotNull(winlator)
        assertEquals("pc", winlator?.systemId)
    }

    @Test
    fun emulatorDetectionFunnel_containsAllRegisteredEmulators() {
        assertTrue(EmulatorDetectionFunnel.isRegisteredEmulator("org.ppsspp.ppsspp"))
        assertTrue(EmulatorDetectionFunnel.isRegisteredEmulator("org.dolphinemu.mmjr"))
        assertTrue(EmulatorDetectionFunnel.isRegisteredEmulator("io.github.lime3ds.android"))
        assertTrue(EmulatorDetectionFunnel.isRegisteredEmulator("com.dsemu.drastic"))
        assertTrue(EmulatorDetectionFunnel.isRegisteredEmulator("org.mupen64plusae.v3.fzurita"))
        assertTrue(EmulatorDetectionFunnel.isRegisteredEmulator("com.github.stenzek.duckstation"))
        assertTrue(EmulatorDetectionFunnel.isRegisteredEmulator("dev.eden.eden_emulator"))
        assertTrue(EmulatorDetectionFunnel.isRegisteredEmulator("com.winlator"))
    }
}
