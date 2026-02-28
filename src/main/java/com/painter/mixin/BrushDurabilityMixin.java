package com.painter.mixin;

import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Item.Settings.class)
public abstract class BrushDurabilityMixin {

    /**
     * @author YourName
     * @reason Intercepts any item setting its durability to 64 (the Brush's unique value)
     * and upgrades it to 1024. This provides a professional-grade lifespan for
     * large architectural projects.
     */
    @ModifyVariable(method = "maxDamage", at = @At("HEAD"), argsOnly = true)
    private int painter$upgradeDurability(int maxDamage) {
        // 64 is the unique durability of the Brush and Flint & Steel.
        // We boost it to 1024 to accommodate mass texturing.
        if (maxDamage == 64) {
            return 1024;
        }
        return maxDamage;
    }
}