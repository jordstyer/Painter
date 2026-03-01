package com.painter;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class PainterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.contains(PainterMod.PALETTE_COMPONENT)) {
                // 1. Display active profile if it exists
                if (stack.contains(PainterMod.ACTIVE_PROFILE_COMPONENT)) {
                    String profileName = stack.get(PainterMod.ACTIVE_PROFILE_COMPONENT);
                    lines.add(Text.literal("§6📁 Profile: §f" + profileName));
                }

                // 2. Display brush settings
                int size = stack.getOrDefault(PainterMod.BRUSH_SIZE_COMPONENT, 1);
                PainterMod.BrushShape shape = stack.getOrDefault(PainterMod.BRUSH_SHAPE_COMPONENT, PainterMod.BrushShape.SQUARE);
                lines.add(Text.literal("§b📐 Size: " + size + "x" + size + " §7(" + shape.name() + ")"));

                // 3. Display Palette weights
                PaletteData data = stack.get(PainterMod.PALETTE_COMPONENT);
                if (data != null && !data.weights().isEmpty()) {
                    lines.add(Text.literal("§e🎨 Active Palette:"));

                    PlayerEntity player = MinecraftClient.getInstance().player;

                    data.weights().forEach((block, weight) -> {
                        String check = "";
                        if (player != null) {
                            // Check if the player has the block in their inventory
                            boolean hasBlock = player.getInventory().count(block.asItem()) > 0;
                            check = hasBlock ? "§a✓ " : "§c✗ ";
                        }
                        lines.add(Text.literal("  " + check + "§7- " + block.getName().getString() + ": §f" + weight + "%"));
                    });
                }
            }
        });
    }
}
