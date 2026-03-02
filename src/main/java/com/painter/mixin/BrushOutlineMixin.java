package com.painter.mixin;

import com.painter.PainterMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.state.OutlineRenderState;
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

    @Inject(method = "drawBlockOutline", at = @At("RETURN"))
    private void drawBrushAreaOutline(MatrixStack matrices, VertexConsumer vertexConsumer,
                                      double cameraX, double cameraY, double cameraZ,
                                      OutlineRenderState outlineRenderState, int color, float lineWidth,
                                      CallbackInfo ci) {

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        World world = client.world;
        if (player == null || world == null) return;

        // Only activate when holding a configured Painter brush
        ItemStack stack = player.getMainHandStack();
        if (!isBrush(stack)) {
            stack = player.getOffHandStack();
            if (!isBrush(stack)) return;
        }

        if (!(client.crosshairTarget instanceof BlockHitResult hit)) return;

        BlockPos centerPos = hit.getBlockPos();
        Direction side = hit.getSide();

        int size = stack.getOrDefault(PainterMod.BRUSH_SIZE_COMPONENT, 1);
        PainterMod.BrushShape shape = stack.getOrDefault(PainterMod.BRUSH_SHAPE_COMPONENT, PainterMod.BrushShape.SQUARE);

        // Re-use the same VertexConsumer vanilla already set up for the block outline
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
                        vertexConsumer,
                        world.getBlockState(targetPos).getOutlineShape(world, targetPos),
                        x, y, z,
                        0x66000000, // black ~40% opacity
                        2.0f
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