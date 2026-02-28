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

    private static final SuggestionProvider<ServerCommandSource> SUGGEST_PROFILES = (context, builder) ->
            CommandSource.suggestMatching(ProfileManager.getProfileNames(), builder);

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
        ProfileManager.loadFromDisk();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("paintbrush")
                    // --- HELP COMMAND ---
                    .executes(context -> {
                        sendHelpMessage(context.getSource());
                        return 1;
                    })
                    .then(CommandManager.literal("help").executes(context -> {
                        sendHelpMessage(context.getSource());
                        return 1;
                    }))
                    // --- PROFILE COMMANDS ---
                    .then(CommandManager.literal("save")
                            .then(CommandManager.argument("name", StringArgumentType.word())
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ItemStack stack = player.getMainHandStack();

                                        if (!stack.contains(PainterMod.PALETTE_COMPONENT)) {
                                            player.sendMessage(Text.literal("§cYour brush has no palette to save!"), false);
                                            return 0;
                                        }

                                        String name = StringArgumentType.getString(context, "name");
                                        PaletteData palette = stack.get(PainterMod.PALETTE_COMPONENT);
                                        int size = stack.getOrDefault(PainterMod.BRUSH_SIZE_COMPONENT, 1);
                                        PainterMod.BrushShape shape = stack.getOrDefault(PainterMod.BRUSH_SHAPE_COMPONENT, PainterMod.BrushShape.SQUARE);

                                        ProfileManager.saveProfile(name, palette, size, shape);
                                        stack.set(PainterMod.ACTIVE_PROFILE_COMPONENT, name);

                                        player.sendMessage(Text.literal("§aProfile '§f" + name + "§a' saved successfully!"), false);
                                        return 1;
                                    })
                            )
                    )
                    .then(CommandManager.literal("load")
                            .then(CommandManager.argument("name", StringArgumentType.word())
                                    .suggests(SUGGEST_PROFILES)
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;

                                        String name = StringArgumentType.getString(context, "name");
                                        PaletteProfile profile = ProfileManager.getProfile(name);

                                        if (profile == null) {
                                            player.sendMessage(Text.literal("§cProfile '§f" + name + "§c' not found."), false);
                                            return 0;
                                        }

                                        ItemStack stack = player.getMainHandStack();
                                        Map<Block, Integer> weights = new HashMap<>();
                                        profile.weights().forEach((idStr, weight) -> {
                                            Block block = Registries.BLOCK.get(Identifier.of(idStr));
                                            if (block != net.minecraft.block.Blocks.AIR) weights.put(block, weight);
                                        });

                                        stack.set(PainterMod.PALETTE_COMPONENT, new PaletteData(weights));
                                        stack.set(PainterMod.BRUSH_SIZE_COMPONENT, profile.size());
                                        stack.set(PainterMod.BRUSH_SHAPE_COMPONENT, PainterMod.BrushShape.valueOf(profile.shape()));
                                        stack.set(PainterMod.ACTIVE_PROFILE_COMPONENT, name);

                                        player.sendMessage(Text.literal("§bProfile '§f" + name + "§b' loaded onto brush."), true);
                                        return 1;
                                    })
                            )
                    )
                    // --- SETTINGS COMMANDS ---
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

                                            ItemStack stack = player.getMainHandStack();
                                            stack.set(PainterMod.PALETTE_COMPONENT, new PaletteData(weights));
                                            stack.remove(PainterMod.ACTIVE_PROFILE_COMPONENT);

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
                                    ItemStack stack = player.getMainHandStack();
                                    stack.remove(PainterMod.PALETTE_COMPONENT);
                                    stack.remove(PainterMod.ACTIVE_PROFILE_COMPONENT);
                                    player.sendMessage(Text.literal("§eBrush palette cleared."), true);
                                }
                                return 1;
                            })
                    )
            );
        });
    }

    private static void sendHelpMessage(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§6§l=== Painter Mod Help ==="), false);
        source.sendFeedback(() -> Text.literal("§e/paintbrush set <pattern> §7- Define blocks (e.g. 50 stone, 50 grass)"), false);
        source.sendFeedback(() -> Text.literal("§e/paintbrush size <1-5> §7- Adjust brush radius"), false);
        source.sendFeedback(() -> Text.literal("§e/paintbrush shape <type> §7- Square, Circle, or Diamond"), false);
        source.sendFeedback(() -> Text.literal("§e/paintbrush save <name> §7- Save current settings to a profile"), false);
        source.sendFeedback(() -> Text.literal("§e/paintbrush load <name> §7- Load a saved profile"), false);
        source.sendFeedback(() -> Text.literal("§e/paintbrush clear §7- Wipe current brush settings"), false);
        source.sendFeedback(() -> Text.literal("§6§l========================"), false);
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