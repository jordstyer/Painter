package com.painter.mixin;

import com.painter.PainterLogic;
import com.painter.PainterMod;
import com.painter.PaletteData;
import net.minecraft.item.BrushItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrushItem.class)
public class BrushItemMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void interceptBrushing(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = context.getStack();

        // Only override vanilla behavior if the brush has our custom palette assigned
        if (stack.contains(PainterMod.PALETTE_COMPONENT)) {
            PaletteData data = stack.get(PainterMod.PALETTE_COMPONENT);

            if (data != null && !data.weights().isEmpty()) {
                boolean success = PainterLogic.tryPaint(context, data);

                // Success swings the arm, Pass prevents the block from acting like standard Sus Sand
                if (success) {
                    cir.setReturnValue(ActionResult.SUCCESS);
                } else {
                    cir.setReturnValue(ActionResult.PASS);
                }
            }
        }
    }
}