package com.painter;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;

public class PainterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.contains(PainterMod.PALETTE_COMPONENT)) {
                int size = stack.getOrDefault(PainterMod.BRUSH_SIZE_COMPONENT, 1);
                PainterMod.BrushShape shape = stack.getOrDefault(PainterMod.BRUSH_SHAPE_COMPONENT, PainterMod.BrushShape.SQUARE);

                lines.add(Text.literal("§b📐 Size: " + size + "x" + size + " §7(" + shape.name() + ")"));

                PaletteData data = stack.get(PainterMod.PALETTE_COMPONENT);
                if (data != null && !data.weights().isEmpty()) {
                    lines.add(Text.literal("§6🎨 Active Palette:"));
                    data.weights().forEach((block, weight) -> {
                        lines.add(Text.literal(" §e" + weight + "%  §7" + block.getName().getString()));
                    });
                }
            }
        });
    }
}