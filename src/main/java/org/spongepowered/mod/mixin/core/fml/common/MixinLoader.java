/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.mod.mixin.core.fml.common;

import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.discovery.ModDiscoverer;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeImpl;

import java.io.File;

@Mixin(value = Loader.class, remap = false)
public abstract class MixinLoader {
    
    @Shadow private File canonicalModsDir;

    @Inject(method = "identifyMods", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
        value = "FIELD",
        opcode = Opcodes.GETFIELD,
        target = "Lnet/minecraftforge/fml/common/Loader;mods:Ljava/util/List;",
        ordinal = 2
    ))
    private void identifyPlugins(CallbackInfoReturnable<ModDiscoverer> cir, ModDiscoverer discoverer) {
        String pluginsDirName = SpongeImpl.getGlobalConfig().getConfig().getGeneral().pluginsDir();
        pluginsDirName = pluginsDirName.replace("${CANONICAL_MODS_DIR}", this.canonicalModsDir.getAbsolutePath());
        
        File pluginsDir = new File(pluginsDirName);
        if (pluginsDir.isDirectory()) {
            FMLLog.info("Also searching %s for plugins", pluginsDir);
            discoverer.findModDirMods(pluginsDir);
        }
    }
    
}
