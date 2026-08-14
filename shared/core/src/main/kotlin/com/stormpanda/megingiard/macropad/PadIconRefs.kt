package com.stormpanda.megingiard.macropad

/**
 * Every button image id still referred to by [profiles].
 *
 * Pure function in :shared:core.
 */
fun referencedImageAssetIds(profiles: List<PadProfile>): Set<String> =
    profiles
        .asSequence()
        .flatMap { it.layouts.asSequence() }
        .flatMap { it.buttons.asSequence() }
        .mapNotNull { it.imageAssetId }
        .filter { it.isNotBlank() }
        .toSet()
