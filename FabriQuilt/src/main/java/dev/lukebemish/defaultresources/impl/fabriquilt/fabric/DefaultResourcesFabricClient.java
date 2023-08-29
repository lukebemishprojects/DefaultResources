/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.fabriquilt.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.server.packs.PackType;

@SuppressWarnings("deprecation")
public class DefaultResourcesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DefaultResourcesFabric.addPackResources(PackType.CLIENT_RESOURCES);
    }
}
