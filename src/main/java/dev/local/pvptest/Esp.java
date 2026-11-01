package dev.local.pvptest;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Player ESP using the vanilla glow outline, which renders through walls.
 * The mixins ask this class whether an entity should glow and which color to use.
 */
public final class Esp {
    private Esp() {}

    public static boolean shouldGlow(Entity entity) {
        Config c = Config.get();
        if (!c.espEnabled) return false;
        if (!(entity instanceof Player)) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity == mc.player) return false;

        if (!c.espInvisible && entity.isInvisible()) return false;

        if (c.espMaxDistance > 0) {
            double max = c.espMaxDistance;
            if (mc.player.distanceToSqr(entity) > max * max) return false;
        }
        return true;
    }

    public static int color() {
        return Config.get().espColor & 0xFFFFFF;
    }
}
