package com.painter;

import net.minecraft.block.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.Map;

public class PainterLogic {

    public static boolean tryPaint(ItemUsageContext context, PaletteData palette) {
        World world = context.getWorld();
        if (world.isClient()) return true;

        PlayerEntity player = context.getPlayer();
        if (player == null) return false;

        BlockPos pos = context.getBlockPos();
        BlockState oldState = world.getBlockState(pos);

        // 1. Roll the weighted dice
        Block targetBlock = pickRandomBlock(palette.weights(), world.random);
        if (targetBlock == null) return false;

        // 2. Identity Check: Stop if block is already the target
        if (oldState.isOf(targetBlock)) {
            return false;
        }

        // 3. Shape matching: Stop if shapes aren't compatible
        if (!isCompatible(oldState.getBlock(), targetBlock)) {
            return false;
        }

        // 4. Survival resource check and consumption
        if (!player.isCreative()) {
            if (!consumeItem(player, targetBlock.asItem())) {
                player.sendMessage(Text.literal("§cMissing " + targetBlock.getName().getString() + " in inventory!"), true);
                return false;
            }

            // Return the replaced block to the player
            Item oldItem = oldState.getBlock().asItem();
            if (oldItem != net.minecraft.item.Items.AIR) {
                ItemStack returnStack = new ItemStack(oldItem, 1);
                if (!player.getInventory().insertStack(returnStack)) {
                    player.dropItem(returnStack, false);
                }
            }
        }

        // 5. Transfer properties to maintain orientation
        BlockState newState = targetBlock.getDefaultState();
        for (Property<?> prop : oldState.getProperties()) {
            if (newState.contains(prop)) {
                newState = copyProperty(oldState, newState, prop);
            }
        }

        // 6. SUCCESS: Update world, play sound, and damage the brush
        world.setBlockState(pos, newState);
        world.playSound(null, pos, newState.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);

        if (player instanceof ServerPlayerEntity serverPlayer) {
            EquipmentSlot slot = context.getHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            context.getStack().damage(1, serverPlayer, slot);
        }

        return true;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState oldState, BlockState newState, Property<T> prop) {
        return newState.with(prop, oldState.get(prop));
    }

    private static Block pickRandomBlock(Map<Block, Integer> weights, Random random) {
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) return null;
        int roll = random.nextInt(totalWeight);
        for (Map.Entry<Block, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) return entry.getKey();
        }
        return null;
    }

    private static boolean isCompatible(Block oldBlock, Block newBlock) {
        if (oldBlock.getClass().equals(newBlock.getClass())) return true;
        if (oldBlock instanceof StairsBlock && newBlock instanceof StairsBlock) return true;
        if (oldBlock instanceof SlabBlock && newBlock instanceof SlabBlock) return true;
        if (oldBlock instanceof WallBlock && newBlock instanceof WallBlock) return true;
        if (oldBlock instanceof FenceBlock && newBlock instanceof FenceBlock) return true;
        if (oldBlock instanceof PillarBlock && newBlock instanceof PillarBlock) return true;
        return oldBlock.getClass() == Block.class && newBlock.getClass() == Block.class;
    }

    private static boolean consumeItem(PlayerEntity player, Item itemToConsume) {
        PlayerInventory inv = player.getInventory();
        // Strictly check standard inventory slots only
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(itemToConsume)) {
                stack.decrement(1);
                return true;
            }
        }
        return false;
    }
}