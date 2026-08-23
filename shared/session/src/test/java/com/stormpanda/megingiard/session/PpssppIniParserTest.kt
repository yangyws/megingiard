package com.stormpanda.megingiard.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PpssppIniParserTest {
    @Test
    fun parseMostRecentSession_validIniContent_returnsParsedActiveGameSession() {
        val ini =
            """
            VRForce = False
            VRImmersiveMode = True
            [Recent]
            MaxRecent = 60
            FileName0 = content://com.android.externalstorage.documents/tree/primary%3Aroms%2FPSP/document/primary%3Aroms%2FPSP%2FDragonball%20Z%20-%20Shin%20Budokai%202%20(Bandai%20Namco%20Games)(JP).iso
            FileName1 = /storage/emulated/0/roms/PSP/Crisis Core - Final Fantasy VII (USA).iso
            [Log]
            SYSTEMEnabled = True
            """.trimIndent()

        val session = PpssppIniParser.parseMostRecentSession("org.ppsspp.ppsspp", ini)

        assertNotNull(session)
        assertEquals("org.ppsspp.ppsspp", session?.packageName)
        assertEquals("Dragonball Z - Shin Budokai 2 (Bandai Namco Games)(JP)", session?.gameTitle)
        assertEquals("psp", session?.systemId)
        assertEquals("PPSSPP", session?.coreOrBackend)
        assertEquals("/storage/emulated/0/roms/PSP/Dragonball Z - Shin Budokai 2 (Bandai Namco Games)(JP).iso", session?.romPath)
    }

    @Test
    fun parseMostRecentSession_directPathInRecent_returnsParsedSession() {
        val ini =
            """
            [Recent]
            FileName0 = /storage/emulated/0/PSP/GAME/Tekken 6.iso
            """.trimIndent()

        val session = PpssppIniParser.parseMostRecentSession("org.ppsspp.ppssppgold", ini)

        assertNotNull(session)
        assertEquals("org.ppsspp.ppssppgold", session?.packageName)
        assertEquals("Tekken 6", session?.gameTitle)
        assertEquals("psp", session?.systemId)
        assertEquals("/storage/emulated/0/PSP/GAME/Tekken 6.iso", session?.romPath)
    }

    @Test
    fun parseMostRecentSession_emptyOrNoRecent_returnsFallbackSession() {
        val iniNoRecent =
            """
            [General]
            FirstRun = false
            """.trimIndent()

        val session = PpssppIniParser.parseMostRecentSession("org.ppsspp.ppsspp", iniNoRecent)

        assertNotNull(session)
        assertEquals("org.ppsspp.ppsspp", session?.packageName)
        assertEquals("PPSSPP", session?.gameTitle)
        assertEquals("psp", session?.systemId)
        assertNull(session?.romPath)
    }

    @Test
    fun parseMostRecentSession_blankString_returnsNull() {
        assertNull(PpssppIniParser.parseMostRecentSession("org.ppsspp.ppsspp", ""))
    }
}
