package com.painter;

import net.fabricmc.api.ModInitializer;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class PainterMod implements ModInitializer {
    public static final String MOD_ID = "painter";

    // Registering the custom Data Component (Safe for both Client and Server)
    public static final ComponentType<PaletteData> PALETTE_COMPONENT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MOD_ID, "palette"),
            ComponentType.<PaletteData>builder().codec(PaletteData.CODEC).build()
    );

    @Override
    public void onInitialize() {
        // Register commands on the common entrypoint (Server-side logic)
        PainterCommand.register();
    }
}