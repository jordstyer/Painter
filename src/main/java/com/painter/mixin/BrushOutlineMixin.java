package com.painter.mixin;

import com.painter.PainterMod;
import com.painter.PaletteData;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class BrushOutlineMixin {

    /**
     * Injects after vanilla draws the single block outline.
     * We draw additional outlines for all blocks in the brush area.
     *
     * The method signature uses the camera position and vertex consumer provider
     * that vanilla passes when rendering the block outline each frame.
     */
    @Inject(
        method = "drawBlockOutline",
        at = @At("RETURN")
    )
    private void drawBrushAreaOutline(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
                                       double cameraX, double cameraY, double cameraZ,
                                       BlockPos pos, BlockState state, CallbackInfo ci) {

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        World world = client.world;
        if (player == null || world == null) return;

        // Check main hand first, then offhand
        ItemStack stack = player.getMainHandStack();
        if (!isBrush(stack)) {
            stack = player.getOffHandStack();
            if (!isBrush(stack)) return;
        }

        // Must be looking at a block
        if (!(client.crosshairTarget instanceof BlockHitResult hit)) return;

        BlockPos centerPos = hit.getBlockPos();
        Direction side = hit.getSide();

        int size = stack.getOrDefault(PainterMod.BRUSH_SIZE_COMPONENT, 1);
        PainterMod.BrushShape shape = stack.getOrDefault(PainterMod.BRUSH_SHAPE_COMPONENT, PainterMod.BrushShape.SQUARE);

        VertexConsumer lines = vertexConsumers.getBuffer(RenderLayers.lines());

        int radius = (size - 1) / 2;
        int min = -radius;
        int max = (size % 2 == 0) ? radius + 1 : radius;

        for (int a = min; a <= max; a++) {
            for (int b = min; b <= max; b++) {
                if (!isInShape(a, b, size, shape)) continue;

                BlockPos targetPos = getRelativePos(centerPos, side, a, b);
                if (world.getBlockState(targetPos).isAir()) continue;

                double x = targetPos.getX() - cameraX;
                double y = targetPos.getY() - cameraY;
                double z = targetPos.getZ() - cameraZ;

                VertexRendering.drawOutline(
                        matrices,
                        lines,
                        world.getBlockState(targetPos).getOutlineShape(world, targetPos),
                        x, y, z,
                        0x66000000, // black, ~40% opacity
                        2.0f        // line width
                );
            }
        }
    }

    private static boolean isBrush(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() == Items.BRUSH
                && stack.contains(PainterMod.BRUSH_SIZE_COMPONENT);
    }

    private static boolean isInShape(int a, int b, int size, PainterMod.BrushShape shape) {
        double offset = (size % 2 == 0) ? 0.5 : 0.0;
        double x = (double) a - offset;
        double y = (double) b - offset;
        double r = (double) size / 2.0;
        return switch (shape) {
            case SQUARE  -> true;
            case CIRCLE  -> (x * x + y * y) < (r * r);
            case DIAMOND -> (Math.abs(x) + Math.abs(y)) < r;
        };
    }

    private static BlockPos getRelativePos(BlockPos pos, Direction side, int a, int b) {
        return switch (side.getAxis()) {
            case X -> pos.add(0, a, b);
            case Y -> pos.add(a, 0, b);
            case Z -> pos.add(a, b, 0);
        };
    }
}
