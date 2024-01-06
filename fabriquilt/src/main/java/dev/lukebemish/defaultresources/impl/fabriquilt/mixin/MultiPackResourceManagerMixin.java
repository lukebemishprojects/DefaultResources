/*
 * Copyright (C) 2023-2024 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.fabriquilt.mixin;

import dev.lukebemish.defaultresources.impl.fabriquilt.ResourceLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {
    @Mutable
    @Shadow
    @Final
    private List<PackResources> packs;

    @Unique
    private PackType defaultresources$type;

    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void defaultresources$captureType(PackType type, List<PackResources> packs, CallbackInfo ci) {
        this.defaultresources$type = type;
        this.defaultresources$addPacksToTop();
    }

    private void defaultresources$addPacksToTop() {
        if (!(this.packs instanceof ArrayList<PackResources>)) {
            this.packs = new ArrayList<>(this.packs);
        }
        ResourceLoader.get(defaultresources$type).appendTopPacks(this.packs::add);
    }
}
