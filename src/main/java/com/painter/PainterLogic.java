package com.painter;

import net.minecraft.block.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import java.util.HashMap;
import java.util.Map;

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
        int changedCount = 0;
        BlockState lastState = null;

        int radius = (size - 1) / 2;
        int min = -radius;
        int max = (size % 2 == 0) ? radius + 1 : radius;

        for (int a = min; a <= max; a++) {
            for (int b = min; b <= max; b++) {
                if (!isInShape(a, b, size, shape)) continue;

                BlockPos targetPos = getRelativePos(centerPos, side, a, b);
                Item item = paintSingle(world, targetPos, player, palette);

                if (item != null) {
                    changedCount++;
                    lastState = world.getBlockState(targetPos);
                    if (item != net.minecraft.item.Items.AIR) {
                        returnedItems.put(item, returnedItems.getOrDefault(item, 0) + 1);
                    }
                }
            }
        }

        if (changedCount > 0 && lastState != null) {
            world.playSound(null, centerPos, lastState.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, lastState),
                        centerPos.getX() + 0.5, centerPos.getY() + 0.5, centerPos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.1);
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

        // FIXED: Added SQUARE case to ensure switch is exhaustive
        return switch (shape) {
            case SQUARE -> true;
            case CIRCLE -> (x * x + y * y) < (r * r);
            case DIAMOND -> (Math.abs(x) + Math.abs(y)) < r;
        };
    }

    private static Item paintSingle(World world, BlockPos pos, PlayerEntity player, PaletteData palette) {
        BlockState oldState = world.getBlockState(pos);
        Block target = pickRandom(palette.weights(), world.random);
        if (target == null || oldState.isOf(target) || !isCompatible(oldState.getBlock(), target)) return null;
        if (!player.isCreative() && !consumeItem(player, target.asItem())) return null;

        BlockState newState = target.getDefaultState();
        for (Property<?> prop : oldState.getProperties()) {
            if (newState.contains(prop)) newState = copyProp(oldState, newState, prop);
        }
        world.setBlockState(pos, newState, 2);
        return oldState.getBlock().asItem();
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

    private static boolean isCompatible(Block b1, Block b2) {
        return b1.getClass().equals(b2.getClass()) ||
                (b1 instanceof StairsBlock && b2 instanceof StairsBlock) ||
                (b1 instanceof SlabBlock && b2 instanceof SlabBlock);
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