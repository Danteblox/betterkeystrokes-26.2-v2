package dev.local.pvptest.mixin;

import dev.local.pvptest.Esp;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftEspMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void pvptest$forceGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Esp.shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }
}
