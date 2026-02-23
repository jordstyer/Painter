package com.painter;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BrushItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.command.CommandSource;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.ServerCommandSource;

import java.util.HashMap;
import java.util.Map;

public class PainterCommand {

    // Custom suggestion provider to auto-complete block IDs
    private static final SuggestionProvider<ServerCommandSource> SUGGEST_BLOCKS = (context, builder) -> {
        String remaining = builder.getRemaining();

        // Find the start of the current word being typed
        int lastDelim = -1;
        for (int i = remaining.length() - 1; i >= 0; i--) {
            char c = remaining.charAt(i);
            if (c == ' ' || c == ',' || c == ';' || c == '=') {
                lastDelim = i;
                break;
            }
        }

        String currentWord = remaining.substring(lastDelim + 1);

        // If typing a number, don't suggest block names
        if (currentWord.matches("\\d+.*")) {
            return builder.buildFuture();
        }

        // Extract block paths without the "minecraft:" prefix for vanilla blocks
        java.util.List<String> suggestions = new java.util.ArrayList<>();
        for (Identifier id : Registries.BLOCK.getIds()) {
            if (id.getNamespace().equals("minecraft")) {
                suggestions.add(id.getPath()); // Adds "oak_planks"
            } else {
                suggestions.add(id.toString()); // Adds "modname:custom_block"
            }
        }

        // Shift the builder to only replace the current word, then suggest the simplified names
        return CommandSource.suggestMatching(suggestions, builder.createOffset(builder.getStart() + lastDelim + 1));
    };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("paintbrush")
                    .then(CommandManager.literal("clear")
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ItemStack mainHand = player.getMainHandStack();

                                if (!(mainHand.getItem() instanceof BrushItem)) {
                                    context.getSource().sendError(Text.literal("You must hold a brush!"));
                                    return 0;
                                }
                                mainHand.remove(PainterMod.PALETTE_COMPONENT);
                                context.getSource().sendFeedback(() -> Text.literal("§aPalette cleared! Brush returned to normal."), false);
                                return 1;
                            })
                    )
                    .then(CommandManager.literal("set")
                            .then(CommandManager.argument("pattern", StringArgumentType.greedyString())
                                    .suggests(SUGGEST_BLOCKS)
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ItemStack mainHand = player.getMainHandStack();

                                        if (!(mainHand.getItem() instanceof BrushItem)) {
                                            context.getSource().sendError(Text.literal("You must hold a brush!"));
                                            return 0;
                                        }

                                        String pattern = StringArgumentType.getString(context, "pattern");
                                        Map<Block, Integer> weights = parsePattern(pattern);

                                        if (weights.isEmpty()) {
                                            context.getSource().sendError(Text.literal("§cCould not parse any valid blocks. Use format: '30 stone, 70 dirt'"));
                                        } else {
                                            mainHand.set(PainterMod.PALETTE_COMPONENT, new PaletteData(weights));
                                            context.getSource().sendFeedback(() -> Text.literal("§aPalette applied to Brush!"), false);
                                        }
                                        return 1;
                                    })
                            )
                    )
            );
        });
    }

    private static Map<Block, Integer> parsePattern(String input) {
        Map<Block, Integer> weights = new HashMap<>();
        // Split by commas or semicolons
        String[] parts = input.split("[,;]");

        for (String part : parts) {
            part = part.trim().replace("%", ""); // Remove optional % symbols
            if (part.isEmpty()) continue;

            // Split by space, colon, or equals
            String[] tokens = part.split("[ :=]+");
            if (tokens.length >= 2) {
                String t1 = tokens[0].trim();
                String t2 = tokens[1].trim();

                int weight = -1;
                String blockName = null;

                // Flexible parsing: allows "20 stone" or "stone 20"
                try {
                    weight = Integer.parseInt(t1);
                    blockName = t2;
                } catch (NumberFormatException e) {
                    try {
                        weight = Integer.parseInt(t2);
                        blockName = t1;
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                }

                if (weight > 0 && blockName != null) {
                    if (!blockName.contains(":")) {
                        blockName = "minecraft:" + blockName;
                    }
                    try {
                        Identifier id = Identifier.of(blockName);
                        if (Registries.BLOCK.containsId(id)) {
                            Block block = Registries.BLOCK.get(id);
                            if (block != Blocks.AIR) {
                                weights.put(block, weight);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return weights;
    }
}