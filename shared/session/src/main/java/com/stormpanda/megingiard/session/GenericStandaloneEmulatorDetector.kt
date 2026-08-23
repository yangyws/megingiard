package com.stormpanda.megingiard.session

import com.stormpanda.megingiard.AppLog

private const val TAG = "GenericStandaloneEmulatorDetector"

/**
 * Universal detector covering all standard standalone retro & handheld emulators on Android.
 * Automatically maps known emulator packages to their corresponding [systemId] and friendly platform title.
 */
object GenericStandaloneEmulatorDetector : EmulatorDetector {
    private val packageToSystemAndTitle =
        mapOf(
            // NDS
            "com.dsemu.drastic" to ("nds" to "DraStic"),
            "me.magnum.melonds" to ("nds" to "melonDS"),
            "me.magnum.melondualds" to ("nds" to "melonDS"),

            // N64
            "org.mupen64plusae.v3.fzurita" to ("n64" to "Mupen64Plus FZ"),
            "org.mupen64plusae.v3.fzurita.pro" to ("n64" to "Mupen64Plus FZ Pro"),
            "org.mupen64plusae.v3.alpha" to ("n64" to "Mupen64Plus FZ"),

            // PS1
            "com.github.stenzek.duckstation" to ("ps1" to "DuckStation"),
            "com.github.stenzek.duckstation.debug" to ("ps1" to "DuckStation"),
            "com.epsxe.ePSXe" to ("ps1" to "ePSXe"),
            "com.emulator.fpse" to ("ps1" to "FPse"),
            "com.emulator.fpse64" to ("ps1" to "FPse64"),

            // PS Vita
            "org.vita3k.emulator" to ("vita" to "Vita3K"),

            // Dreamcast
            "io.recompiled.redream" to ("dreamcast" to "Redream"),
            "org.redream.redream" to ("dreamcast" to "Redream"),
            "com.flycast.emulator" to ("dreamcast" to "Flycast"),
            "com.reicast.emulator" to ("dreamcast" to "Reicast"),

            // Sega Saturn
            "org.devmiyax.yabasanshioro2" to ("saturn" to "Yaba Sanshiro 2"),
            "org.devmiyax.yabasanshioro2.pro" to ("saturn" to "Yaba Sanshiro 2 Pro"),
            "org.yabause.yabausesatoriya" to ("saturn" to "Yaba Sanshiro"),

            // GBA
            "com.explusalpha.GbaEmu" to ("gba" to "GBA.emu"),
            "com.fastemulator.gba" to ("gba" to "My Boy!"),
            "it.dbtecno.pizzaboygba" to ("gba" to "Pizza Boy GBA"),
            "it.dbtecno.pizzaboygbapro" to ("gba" to "Pizza Boy GBA Pro"),
            "com.johnemulators.johngbac" to ("gba" to "John GBA"),
            "com.johnemulators.johngba" to ("gba" to "John GBA"),
            "com.nostalgiaemulators.nostalgiagba" to ("gba" to "Nostalgia.GBA"),

            // GBC / GB
            "com.explusalpha.GbcEmu" to ("gbc" to "GBC.emu"),
            "com.fastemulator.gbc" to ("gbc" to "My OldBoy!"),
            "it.dbtecno.pizzaboy" to ("gbc" to "Pizza Boy GBC"),
            "it.dbtecno.pizzaboypro" to ("gbc" to "Pizza Boy GBC Pro"),
            "com.johnemulators.johngbcc" to ("gbc" to "John GBC"),
            "com.johnemulators.johngbc" to ("gbc" to "John GBC"),

            // SNES
            "com.explusalpha.Snes9xEx" to ("snes" to "Snes9x EX+"),
            "com.johnemulators.johnsnesc" to ("snes" to "John SNES"),
            "com.johnemulators.johnsnes" to ("snes" to "John SNES"),
            "com.nostalgiaemulators.nostalgiasnes" to ("snes" to "Nostalgia.SNES"),

            // NES
            "com.explusalpha.NesEmu" to ("nes" to "NES.emu"),
            "com.johnemulators.johnnesc" to ("nes" to "John NES"),
            "com.johnemulators.johnnes" to ("nes" to "John NES"),
            "com.nostalgiaemulators.nostalgianes" to ("nes" to "Nostalgia.NES"),

            // Genesis / Mega Drive
            "com.explusalpha.MdEmu" to ("genesis" to "MD.emu"),

            // PCE / TurboGrafx-16
            "com.explusalpha.PceEmu" to ("pce" to "PCE.emu"),

            // NeoGeo
            "com.explusalpha.NeoS" to ("neogeo" to "NEO.emu"),

            // MSX
            "com.explusalpha.MsxEmu" to ("msx" to "MSX.emu"),

            // Arcade / MAME
            "com.seleuco.mame4droid" to ("arcade" to "MAME4droid"),
            "com.fba4droid" to ("arcade" to "FBA4droid"),

            // PC / Windows Emulation
            "com.winlator" to ("pc" to "Winlator"),
            "com.winlator.cmod" to ("pc" to "Winlator CMOD"),
            "com.winlator.glibc" to ("pc" to "Winlator Glibc"),
            "com.winlator.frost" to ("pc" to "Winlator Frost"),
            "com.winlator.ajay" to ("pc" to "Winlator Ajay"),
            "com.mobox" to ("pc" to "Mobox"),
            "com.mobox.emulator" to ("pc" to "Mobox"),
            "com.horizon.emu" to ("pc" to "Horizon Emulator"),
        )

    override val supportedPackages: Set<String> = packageToSystemAndTitle.keys

    override val systemId: String = "generic"

    override suspend fun detectActiveSession(packageName: String): ActiveGameSession? {
        val match = packageToSystemAndTitle[packageName] ?: return null
        val (sysId, title) = match
        AppLog.d(TAG, "Resolved generic standalone session for $packageName: $title ($sysId)")
        return ActiveGameSession(
            packageName = packageName,
            romPath = null,
            gameTitle = title,
            systemId = sysId,
            coreOrBackend = title,
        )
    }
}
