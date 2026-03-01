package com.painter;

import net.minecraft.block.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PainterLogic {

    public static boolean tryPaint(ItemUsageContext context, PaletteData palette) {
        World world = context.getWorld();
        if (world.isClient()) return true;

        PlayerEntity player = context.getPlayer();
        if (player == null) return false;

        ItemStack brush = context.getStack();
        int size = brush.getOrDefault(PainterMod.BRUSH_SIZE_COMPONENT, 1);
        PainterMod.BrushShape shape = brush.getOrDefault(PainterMod.BRUSH_SHAPE_COMPONENT, PainterMod.BrushShape.SQUARE);

        BlockPos centerPos = context.getBlockPos();
        Direction side = context.getSide();

        Map<Item, Integer> returnedItems = new HashMap<>();
        Set<Block> missingBlocks = new HashSet<>();
        int changedCount = 0;
        BlockState lastState = null;

        int radius = (size - 1) / 2;
        int min = -radius;
        int max = (size % 2 == 0) ? radius + 1 : radius;

        for (int a = min; a <= max; a++) {
            for (int b = min; b <= max; b++) {
                if (!isInShape(a, b, size, shape)) continue;

                BlockPos targetPos = getRelativePos(centerPos, side, a, b);
                Item item = paintSingle(world, targetPos, player, palette, brush, missingBlocks);

                if (item != null) {
                    changedCount++;
                    lastState = world.getBlockState(targetPos);
                    // If the item is AIR, it was destroyed by the anti-cheat and isn't returned
                    if (item != Items.AIR) {
                        returnedItems.put(item, returnedItems.getOrDefault(item, 0) + 1);
                    }
                }
            }
        }

        if (!missingBlocks.isEmpty() && player instanceof ServerPlayerEntity) {
            String missingBlockNames = missingBlocks.stream()
                    .map(block -> block.getName().getString())
                    .collect(Collectors.joining(", "));
            player.sendMessage(Text.literal("§cOut of stock: §f" + missingBlockNames), true);
        }

        if (changedCount > 0 && lastState != null) {
            world.playSound(null, centerPos, lastState.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, lastState),
                        centerPos.getX() + 0.5, centerPos.getY() + 0.5, centerPos.getZ() + 0.5, 10 + (size * 2), 0.5, 0.5, 0.5, 0.1);
            }

            if (!player.isCreative()) {
                returnedItems.forEach((item, count) -> {
                    ItemStack stack = new ItemStack(item, count);
                    if (!player.getInventory().insertStack(stack)) player.dropItem(stack, false);
                });
                if (player instanceof ServerPlayerEntity sp) {
                    brush.damage(changedCount, sp, context.getHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                }
            }
            return true;
        }
        return false;
    }

    private static boolean isInShape(int a, int b, int size, PainterMod.BrushShape shape) {
        double offset = (size % 2 == 0) ? 0.5 : 0.0;
        double x = (double)a - offset;
        double y = (double)b - offset;
        double r = (double)size / 2.0;

        return switch (shape) {
            case SQUARE -> true;
            case CIRCLE -> (x * x + y * y) < (r * r);
            case DIAMOND -> (Math.abs(x) + Math.abs(y)) < r;
        };
    }

    private static Item paintSingle(World world, BlockPos pos, PlayerEntity player, PaletteData palette, ItemStack brush, Set<Block> missingBlocks) {
        BlockState oldState = world.getBlockState(pos);

        // 1. AIR GUARD: Prevent painting in mid-air.
        if (oldState.isAir()) return null;

        // 2. UNBREAKABLE GUARD: Prevent painting Bedrock, End Portals, etc.
        if (oldState.getHardness(world, pos) < 0.0F) return null;

        Block target = pickRandom(palette.weights(), world.random);
        if (target == null || oldState.isOf(target) || !isCompatible(oldState, target)) return null;

        if (!player.isCreative() && !consumeItem(player, target.asItem())) {
            missingBlocks.add(target);
            return null;
        }

        BlockState newState = target.getDefaultState();
        for (Property<?> prop : oldState.getProperties()) {
            if (newState.contains(prop)) newState = copyProp(oldState, newState, prop);
        }
        world.setBlockState(pos, newState, 2);

        // Return the item evaluated by our Silk Touch/Anti-Cheat logic
        return getReturnedItem(oldState, brush);
    }

    private static Item getReturnedItem(BlockState state, ItemStack brush) {
        Block block = state.getBlock();
        String blockId = Registries.BLOCK.getId(block).getPath();
        TagKey<Block> cOres = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores"));

        // Check 1.21 component data safely for the Silk Touch enchantment ID
        boolean hasSilkTouch = false;
        for (var entry : brush.getEnchantments().getEnchantmentEntries()) {
            if (entry.getKey().getKey().isPresent()) {
                String enchId = entry.getKey().getKey().get().getValue().getPath();
                if (enchId.equals("silk_touch")) {
                    hasSilkTouch = true;
                    break;
                }
            }
        }

        // Anti-Cheat: Downgrade or destroy restricted natural blocks if Silk Touch is missing
        if (!hasSilkTouch) {

            // 1. ORES: Destroy completely. Drops nothing (Items.AIR). Prevents free diamonds!
            if (state.isIn(cOres) || blockId.endsWith("_ore") || blockId.equals("ancient_debris")) {
                return Items.AIR;
            }
            // 2. STONE: Downgrades to Cobblestone
            if (block == Blocks.STONE) {
                return Items.COBBLESTONE;
            }
            if (block == Blocks.DEEPSLATE) {
                return Items.COBBLED_DEEPSLATE;
            }
            // 3. GRASS & DIRT: Downgrades to plain Dirt
            if (block == Blocks.GRASS_BLOCK || block == Blocks.MYCELIUM || block == Blocks.PODZOL || block == Blocks.DIRT_PATH) {
                return Items.DIRT;
            }
            if (block == Blocks.CRIMSON_NYLIUM || block == Blocks.WARPED_NYLIUM) {
                return Items.NETHERRACK;
            }
        }

        // Safe blocks (Glass, Planks, Bricks) or Silk-Touched blocks return exact item
        return block.asItem();
    }

    private static BlockPos getRelativePos(BlockPos pos, Direction side, int a, int b) {
        return switch (side.getAxis()) {
            case X -> pos.add(0, a, b);
            case Y -> pos.add(a, 0, b);
            case Z -> pos.add(a, b, 0);
        };
    }

    private static <T extends Comparable<T>> BlockState copyProp(BlockState s1, BlockState s2, Property<T> p) {
        return s2.with(p, s1.get(p));
    }

    private static Block pickRandom(Map<Block, Integer> weights, Random random) {
        int total = weights.values().stream().mapToInt(i -> i).sum();
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (var entry : weights.entrySet()) {
            if ((roll -= entry.getValue()) < 0) return entry.getKey();
        }
        return null;
    }

    private static boolean isCompatible(BlockState oldState, Block target) {
        Block b1 = oldState.getBlock();

        if (b1.getClass().equals(target.getClass())) return true;
        if (b1 instanceof PillarBlock && target instanceof PillarBlock) return true;

        // Ores are NO LONGER blocked here! You can freely paint over them.

        boolean b1IsFragile = b1 instanceof PlantBlock || b1 instanceof FluidBlock ||
                b1 instanceof BlockWithEntity || b1 instanceof DoorBlock ||
                b1 instanceof TrapdoorBlock || b1 instanceof BedBlock ||
                b1 instanceof CarpetBlock;

        if (b1IsFragile) return false;

        boolean b1IsStructural = b1 instanceof StairsBlock || b1 instanceof SlabBlock || b1 instanceof WallBlock || b1 instanceof FenceBlock || b1 instanceof PaneBlock;
        boolean targetIsStructural = target instanceof StairsBlock || target instanceof SlabBlock || target instanceof WallBlock || target instanceof FenceBlock || target instanceof PaneBlock;

        return !b1IsStructural && !targetIsStructural;
    }

    private static boolean consumeItem(PlayerEntity player, Item item) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(item)) {
                inv.getStack(i).decrement(1);
                return true;
            }
        }
        return false;
    }
}
