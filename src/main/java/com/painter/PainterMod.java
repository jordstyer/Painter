package com.painter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PainterMod implements ModInitializer {
    public static final String MOD_ID = "painter";

    // Registering the custom Data Component that will live on the Brush
    public static final ComponentType<PaletteData> PALETTE_COMPONENT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(MOD_ID, "palette"),
            ComponentType.<PaletteData>builder().codec(PaletteData.CODEC).build()
    );

    @Override
    public void onInitialize() {
        // Register the chat command
        PainterCommand.register();

        // Register the Item Tooltip (Hover text)
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.contains(PALETTE_COMPONENT)) {
                PaletteData data = stack.get(PALETTE_COMPONENT);
                if (data != null && !data.weights().isEmpty()) {
                    lines.add(Text.literal("")); // Blank line for spacing
                    lines.add(Text.literal("§6🎨 Active Palette:"));

                    // Loop through the saved blocks and display their weights
                    data.weights().forEach((block, weight) -> {
                        String blockName = block.getName().getString();
                        // ADDED: The % symbol right after the weight number
                        lines.add(Text.literal(" §e" + weight + "%  §7" + blockName));
                    });
                }
            }
        });
    }
}