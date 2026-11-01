package dev.local.pvptest;

import dev.local.pvptest.mixin.MinecraftAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.ThreadLocalRandom;

public final class AutoClicker {
    private static final long MAX_LAG_NS = 150_000_000L;
    private static long nextClickNs = 0;

    private AutoClicker() {}

    /** Called once per client tick (max 20 clicks/s, one per tick). */
    public static void tick(Minecraft mc) {
        Config c = Config.get();
        if (!c.autoclickerEnabled) {
            nextClickNs = 0;
            return;
        }
        if (mc.player == null || mc.level == null || mc.screen != null || !mc.mouseHandler.isMouseGrabbed()) {
            nextClickNs = 0;
            return;
        }

        boolean right = c.button == Config.Button.RIGHT;
        KeyMapping key = right ? mc.options.keyUse : mc.options.keyAttack;

        if (c.mode == Config.Mode.HOLD && !key.isDown()) {
            nextClickNs = 0;
            return;
        }
        if (c.pauseWhileUsing && !right && mc.player.isUsingItem()) return;

        if (c.weaponOnly) {
            ItemStack held = mc.player.getMainHandItem();
            if (!held.is(ItemTags.SWORDS) && !held.is(ItemTags.AXES)) return;
        }

        if (!right) {
            HitResult hit = mc.hitResult;
            HitResult.Type type = hit == null ? HitResult.Type.MISS : hit.getType();
            if (c.onlyOnEntity && type != HitResult.Type.ENTITY) return;
            // Leave block mining to vanilla unless explicitly enabled
            if (!c.clickOnBlocks && type == HitResult.Type.BLOCK) return;
        }

        long now = System.nanoTime();
        if (nextClickNs == 0 || now - nextClickNs > MAX_LAG_NS) {
            nextClickNs = now + nextDelayNs(c);
            return;
        }
        if (now < nextClickNs) return;

        // accumulate from the previous deadline so the average rate matches the target CPS
        nextClickNs += nextDelayNs(c);

        MinecraftAccessor acc = (MinecraftAccessor) mc;
        if (right) {
            acc.pvptest$startUseItem();
        } else {
            acc.pvptest$startAttack();
        }
    }

    private static long nextDelayNs(Config c) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double cps = c.minCps + rnd.nextDouble() * (c.maxCps - c.minCps);
        double delayMs = 1000.0 / cps;
        double noise = rnd.nextGaussian() * c.jitter * delayMs * 0.5;
        delayMs = Math.max(30.0, delayMs + noise);
        return (long) (delayMs * 1_000_000.0);
    }
}
