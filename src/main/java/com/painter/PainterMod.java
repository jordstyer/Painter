package com.painter;

import net.fabricmc.api.ModInitializer;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.mojang.serialization.Codec;

public class PainterMod implements ModInitializer {
    public static final String MOD_ID = "painter";

    // Supported Brush Shapes
    public enum BrushShape {
        SQUARE, CIRCLE, DIAMOND;
        public static final Codec<BrushShape> CODEC = Codec.STRING.xmap(
                s -> BrushShape.valueOf(s.toUpperCase()),
                BrushShape::name
        );
    }

    // Palette storage (Blocks + Weights)
    public static final ComponentType<PaletteData> PALETTE_COMPONENT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MOD_ID, "palette"),
            ComponentType.<PaletteData>builder().codec(PaletteData.CODEC).build()
    );

    // Brush size storage (1-5)
    public static final ComponentType<Integer> BRUSH_SIZE_COMPONENT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MOD_ID, "brush_size"),
            ComponentType.<Integer>builder().codec(Codec.INT).build()
    );

    // Brush shape storage
    public static final ComponentType<BrushShape> BRUSH_SHAPE_COMPONENT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MOD_ID, "brush_shape"),
            ComponentType.<BrushShape>builder().codec(BrushShape.CODEC).build()
    );

    @Override
    public void onInitialize() {
        PainterCommand.register();
    }
}