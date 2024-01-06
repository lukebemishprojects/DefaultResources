/*
 * Copyright (C) 2023-2024 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.fabriquilt;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.server.packs.PackType;

@SuppressWarnings("deprecation")
public class DefaultResourcesFabriQuiltClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DefaultResourcesFabriQuilt.addPackResources(PackType.CLIENT_RESOURCES);
    }
}
