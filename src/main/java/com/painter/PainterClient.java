package com.painter;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;

public class PainterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Tooltip logic moved here to prevent Server crashes
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.contains(PainterMod.PALETTE_COMPONENT)) {
                PaletteData data = stack.get(PainterMod.PALETTE_COMPONENT);
                if (data != null && !data.weights().isEmpty()) {
                    lines.add(Text.literal(""));
                    lines.add(Text.literal("§6🎨 Active Palette:"));

                    data.weights().forEach((block, weight) -> {
                        String blockName = block.getName().getString();
                        lines.add(Text.literal(" §e" + weight + "%  §7" + blockName));
                    });
                }
            }
        });
    }
}