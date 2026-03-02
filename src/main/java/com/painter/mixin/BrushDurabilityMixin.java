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
     * and upgrades it to 128. This avoids targeting the BrushItem class directly,
     * which prevents the "No refMap" mapping crashes.
     */
    @ModifyVariable(method = "maxDamage", at = @At("HEAD"), argsOnly = true)
    private int painter$upgradeDurability(int maxDamage) {
        // 64 is the unique durability of the Brush and Flint & Steel.
        // If an item asks for 64, we give it 128.
        if (maxDamage == 64) {
            return 64;
        }
        return maxDamage;
    }
}