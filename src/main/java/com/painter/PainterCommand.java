package com.painter;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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
            if (id.getNamespace().equals("minecraft")) suggestions.add(id.getPath());
            else suggestions.add(id.toString());
        }
        return CommandSource.suggestMatching(suggestions, builder.createOffset(builder.getStart() + lastDelim + 1));
    };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("paintbrush")
                    .then(CommandManager.literal("size")
                            .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 5))
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player != null) {
                                            int size = IntegerArgumentType.getInteger(context, "value");
                                            player.getMainHandStack().set(PainterMod.BRUSH_SIZE_COMPONENT, size);
                                            player.sendMessage(Text.literal("§bBrush size: " + size + "x" + size), true);
                                        }
                                        return 1;
                                    })
                            )
                    )
                    .then(CommandManager.literal("shape")
                            .then(CommandManager.literal("square").executes(context -> setShape(context.getSource().getPlayer(), PainterMod.BrushShape.SQUARE)))
                            .then(CommandManager.literal("circle").executes(context -> setShape(context.getSource().getPlayer(), PainterMod.BrushShape.CIRCLE)))
                            .then(CommandManager.literal("diamond").executes(context -> setShape(context.getSource().getPlayer(), PainterMod.BrushShape.DIAMOND)))
                    )
                    .then(CommandManager.literal("set")
                            .then(CommandManager.argument("pattern", StringArgumentType.greedyString())
                                    .suggests(SUGGEST_BLOCKS)
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player != null) {
                                            String pattern = StringArgumentType.getString(context, "pattern");
                                            Map<Block, Integer> weights = parsePattern(pattern);
                                            if (weights.isEmpty()) return 0;
                                            player.getMainHandStack().set(PainterMod.PALETTE_COMPONENT, new PaletteData(weights));
                                            player.sendMessage(Text.literal("§aBrush palette updated!"), true);
                                        }
                                        return 1;
                                    })
                            )
                    )
                    .then(CommandManager.literal("clear")
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player != null) {
                                    player.getMainHandStack().remove(PainterMod.PALETTE_COMPONENT);
                                    player.sendMessage(Text.literal("§eBrush palette cleared."), true);
                                }
                                return 1;
                            })
                    )
            );
        });
    }

    private static int setShape(ServerPlayerEntity player, PainterMod.BrushShape shape) {
        if (player != null) {
            player.getMainHandStack().set(PainterMod.BRUSH_SHAPE_COMPONENT, shape);
            player.sendMessage(Text.literal("§bBrush shape: §f" + shape.name()), true);
        }
        return 1;
    }

    private static Map<Block, Integer> parsePattern(String pattern) {
        Map<Block, Integer> weights = new HashMap<>();
        for (String part : pattern.split("[,;]")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            String[] subParts = part.split("\\s+");
            int weight = 100;
            String blockId;
            if (subParts[0].matches("\\d+")) {
                weight = Integer.parseInt(subParts[0]);
                blockId = subParts.length > 1 ? subParts[1] : "";
            } else {
                blockId = subParts[0];
            }
            if (blockId.isEmpty()) continue;
            Identifier id = blockId.contains(":") ? Identifier.of(blockId) : Identifier.of("minecraft", blockId);
            Block block = Registries.BLOCK.get(id);
            if (block != net.minecraft.block.Blocks.AIR) weights.put(block, weight);
        }
        return weights;
    }
}