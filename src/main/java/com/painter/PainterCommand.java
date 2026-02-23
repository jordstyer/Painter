package com.painter;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class PainterCommand {

    // Custom suggestion provider to auto-complete block IDs without "minecraft:" prefix
    private static final SuggestionProvider<ServerCommandSource> SUGGEST_BLOCKS = (context, builder) -> {
        String remaining = builder.getRemaining();
        int lastDelim = -1;
        for (int i = remaining.length() - 1; i >= 0; i--) {
            char c = remaining.charAt(i);
            if (c == ' ' || c == ',' || c == ';' || c == '=') {
                lastDelim = i;
                break;
            }
        }

        String currentWord = remaining.substring(lastDelim + 1);
        if (currentWord.matches("\\d+.*")) return builder.buildFuture();

        java.util.List<String> suggestions = new java.util.ArrayList<>();
        for (Identifier id : Registries.BLOCK.getIds()) {
            if (id.getNamespace().equals("minecraft")) {
                suggestions.add(id.getPath());
            } else {
                suggestions.add(id.toString());
            }
        }

        return CommandSource.suggestMatching(suggestions, builder.createOffset(builder.getStart() + lastDelim + 1));
    };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("paintbrush")
                    .then(CommandManager.literal("clear")
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player != null) {
                                    ItemStack stack = player.getMainHandStack();
                                    stack.remove(PainterMod.PALETTE_COMPONENT);
                                    player.sendMessage(Text.literal("§eBrush palette cleared."), true);
                                }
                                return 1;
                            })
                    )
                    .then(CommandManager.literal("set")
                            .then(CommandManager.argument("pattern", StringArgumentType.greedyString())
                                    .suggests(SUGGEST_BLOCKS)
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player != null) {
                                            String pattern = StringArgumentType.getString(context, "pattern");
                                            Map<Block, Integer> weights = parsePattern(pattern);

                                            if (weights.isEmpty()) {
                                                player.sendMessage(Text.literal("§cInvalid pattern! Use: 'oak_planks' or '50 stone, 50 dirt'"), false);
                                                return 0;
                                            }

                                            ItemStack stack = player.getMainHandStack();
                                            stack.set(PainterMod.PALETTE_COMPONENT, new PaletteData(weights));
                                            player.sendMessage(Text.literal("§aBrush palette updated!"), true);
                                        }
                                        return 1;
                                    })
                            )
                    )
            );
        });
    }

    private static Map<Block, Integer> parsePattern(String pattern) {
        Map<Block, Integer> weights = new HashMap<>();
        String[] parts = pattern.split("[,;]");

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            String[] subParts = part.split("\\s+");
            int weight = 100; // Default weight if not specified
            String blockId;

            // Check if the first subpart is a number (the weight)
            if (subParts[0].matches("\\d+")) {
                weight = Integer.parseInt(subParts[0]);
                blockId = subParts.length > 1 ? subParts[1] : "";
            } else {
                blockId = subParts[0];
            }

            if (blockId.isEmpty()) continue;

            // Handle prefixes
            Identifier id = blockId.contains(":") ? Identifier.of(blockId) : Identifier.of("minecraft", blockId);
            Block block = Registries.BLOCK.get(id);

            // Register.BLOCK.get returns AIR if not found
            if (block != net.minecraft.block.Blocks.AIR) {
                weights.put(block, weight);
            }
        }
        return weights;
    }
}