package com.painter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;

import java.util.Map;

// Java 21 Record holding our percentages mapping
public record PaletteData(Map<Block, Integer> weights) {

    // The Codec instructs Minecraft how to save this map to the world's NBT data
    public static final Codec<PaletteData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Registries.BLOCK.getCodec(), Codec.INT)
                            .fieldOf("weights").forGetter(PaletteData::weights)
            ).apply(instance, PaletteData::new)
    );
}