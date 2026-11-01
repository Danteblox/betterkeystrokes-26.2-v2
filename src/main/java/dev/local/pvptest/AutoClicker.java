package dev.local.pvptest;

import dev.local.pvptest.mixin.MinecraftAccessor;
import dev.local.pvptest.mixin.MouseHandlerAccessor;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.ThreadLocalRandom;

public final class AutoClicker {
    private static final long MAX_LAG_NS = 150_000_000L;
    private static long nextClickNs = 0;

    /** Why the clicker did or didn't click on the last tick. Shown by /ac status. */
    public static String status = "never ran";
    public static long totalClicks = 0;

    private AutoClicker() {}

    /** Called once per client tick (max 20 clicks/s, one per tick). */
    public static void tick(Minecraft mc) {
        Config c = Config.get();
        if (!c.autoclickerEnabled) {
            status = "disabled (press R or /ac toggle)";
            nextClickNs = 0;
            return;
        }
        if (mc.player == null || mc.level == null) {
            status = "not in a world";
            nextClickNs = 0;
            return;
        }
        if (mc.gui.screen() != null) {
            status = "a screen/menu is open";
            nextClickNs = 0;
            return;
        }

        boolean right = c.button == Config.Button.RIGHT;
        KeyMapping key = right ? mc.options.keyUse : mc.options.keyAttack;

        int glfwButton = right ? GLFW.GLFW_MOUSE_BUTTON_RIGHT : GLFW.GLFW_MOUSE_BUTTON_LEFT;
        boolean keyDown = key.isDown();
        boolean physicalDown = GLFW.glfwGetMouseButton(mc.getWindow().handle(), glfwButton) == GLFW.GLFW_PRESS;
        if (c.mode == Config.Mode.HOLD && !keyDown && !physicalDown) {
            status = "HOLD mode: attack button not held (try /ac mode auto)";
            nextClickNs = 0;
            return;
        }
        if (c.pauseWhileUsing && !right && mc.player.isUsingItem()) {
            status = "using an item";
            return;
        }

        if (c.weaponOnly) {
            ItemStack held = mc.player.getMainHandItem();
            if (!held.is(ItemTags.SWORDS) && !held.is(ItemTags.AXES)) {
                status = "weaponOnly: not holding a sword/axe";
                return;
            }
        }

        if (!right) {
            HitResult hit = mc.hitResult;
            HitResult.Type type = hit == null ? HitResult.Type.MISS : hit.getType();
            if (c.onlyOnEntity && type != HitResult.Type.ENTITY) {
                status = "onlyOnEntity: not aiming at an entity (try /ac onlyonentity false)";
                return;
            }
            // Leave block mining to vanilla unless explicitly enabled
            if (!c.clickOnBlocks && type == HitResult.Type.BLOCK) {
                status = "aiming at a block (try /ac clickonblocks true)";
                return;
            }
        }

        long now = System.nanoTime();
        if (nextClickNs == 0 || now - nextClickNs > MAX_LAG_NS) {
            nextClickNs = now + nextDelayNs(c);
            status = "armed, waiting for first click";
            return;
        }
        if (now < nextClickNs) {
            status = "waiting for next click";
            return;
        }

        // accumulate from the previous deadline so the average rate matches the target CPS
        nextClickNs += nextDelayNs(c);

        if (c.input == Config.Input.REAL) {
            MouseHandlerAccessor mh = (MouseHandlerAccessor) mc.mouseHandler;
            long window = mc.getWindow().handle();
            MouseButtonInfo info = new MouseButtonInfo(glfwButton, 0);
            if (keyDown) {
                // button is held: release then press so it ends held, with one counted press
                mh.pvptest$onButton(window, info, GLFW.GLFW_RELEASE);
                mh.pvptest$onButton(window, info, GLFW.GLFW_PRESS);
            } else {
                mh.pvptest$onButton(window, info, GLFW.GLFW_PRESS);
                mh.pvptest$onButton(window, info, GLFW.GLFW_RELEASE);
            }
        } else {
            MinecraftAccessor acc = (MinecraftAccessor) mc;
            if (right) {
                acc.pvptest$startUseItem();
            } else {
                acc.pvptest$startAttack();
            }
        }
        totalClicks++;
        status = "clicking (" + totalClicks + " total)";
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
