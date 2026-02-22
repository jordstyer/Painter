package com.painter;

import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
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

        // 2. Identity Check: Prevent replacing a block with the exact same block
        if (oldState.isOf(targetBlock)) {
            return false; // Stop! Don't consume items or damage the brush.
        }

        // 3. Shape matching (Stair to Stair, Full to Full)
        if (!isCompatible(oldState.getBlock(), targetBlock)) {
            return false;
        }

        // 4. Survival resource consumption
        if (!player.isCreative()) {
            if (!consumeItem(player, targetBlock.asItem())) {
                player.sendMessage(Text.literal("§cMissing " + targetBlock.getName().getString() + " in inventory!"), true);
                return false;
            }
        }

        // 5. Transfer properties to maintain orientation
        BlockState newState = targetBlock.getDefaultState();
        for (Property<?> prop : oldState.getProperties()) {
            if (newState.contains(prop)) {
                newState = copyProperty(oldState, newState, prop);
            }
        }

        // 6. Replace block, play block-place sound, and damage brush
        world.setBlockState(pos, newState);
        world.playSound(null, pos, newState.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);

        EquipmentSlot slot = context.getHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        context.getStack().damage(1, player, slot);

        return true;
    }

    // Helper to safely cast and copy properties
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
        // Standard full blocks
        return oldBlock.getClass() == Block.class && newBlock.getClass() == Block.class;
    }

    private static boolean consumeItem(PlayerEntity player, Item itemToConsume) {
        PlayerInventory inv = player.getInventory();

        // Pass 1: Standard Inventory slots
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(itemToConsume)) {
                stack.decrement(1);
                return true;
            }
        }

        // Pass 2: Bundle extraction
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.contains(DataComponentTypes.BUNDLE_CONTENTS)) {
                BundleContentsComponent bundleContents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
                if (bundleContents != null) {
                    List<ItemStack> extractedItems = new ArrayList<>();
                    bundleContents.iterate().forEach(s -> extractedItems.add(s.copy()));

                    boolean found = false;
                    for (ItemStack bundleItem : extractedItems) {
                        if (bundleItem.isOf(itemToConsume) && bundleItem.getCount() > 0) {
                            bundleItem.decrement(1);
                            found = true;
                            break;
                        }
                    }

                    // Rebuild and save the modified bundle
                    if (found) {
                        BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(BundleContentsComponent.DEFAULT);
                        for (ItemStack s : extractedItems) {
                            if (!s.isEmpty()) builder.add(s);
                        }
                        stack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}