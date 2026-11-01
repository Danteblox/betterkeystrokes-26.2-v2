package dev.local.pvptest;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class PvpTestMod implements ClientModInitializer {
    private static KeyMapping toggleClicker;
    private static KeyMapping toggleEsp;
    private static Object lastPlayer;

    @Override
    public void onInitializeClient() {
        Config.load();
        System.out.println("[pvptest] loaded (client init)");

        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("pvptest", "main"));
        toggleClicker = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pvptest.autoclicker", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, category));
        toggleEsp = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pvptest.esp", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player != null && mc.player != lastPlayer) {
                lastPlayer = mc.player;
                mc.player.sendSystemMessage(Component.literal(
                        "[PvPTest] loaded. R = autoclicker, H = ESP. Try /ac status"));
            }
            while (toggleClicker.consumeClick()) {
                Config c = Config.get();
                c.autoclickerEnabled = !c.autoclickerEnabled;
                Config.save();
                status(mc, "Autoclicker " + (c.autoclickerEnabled ? "ON" : "OFF"));
            }
            while (toggleEsp.consumeClick()) {
                Config c = Config.get();
                c.espEnabled = !c.espEnabled;
                Config.save();
                status(mc, "ESP " + (c.espEnabled ? "ON" : "OFF"));
            }
            AutoClicker.tick(mc);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerAutoclickerCommands(dispatcher);
            registerEspCommands(dispatcher);
        });
    }

    private static void status(Minecraft mc, String msg) {
        if (mc.player != null) {
            mc.player.sendOverlayMessage(Component.literal("[PvPTest] " + msg));
        }
    }

    private static void reply(FabricClientCommandSource src, String msg) {
        src.sendFeedback(Component.literal("[PvPTest] " + msg));
    }

    private static void registerAutoclickerCommands(
            com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> d) {
        d.register(ClientCommands.literal("ac")
                .executes(ctx -> {
                    Config c = Config.get();
                    reply(ctx.getSource(), String.format(Locale.ROOT,
                            "enabled=%b mode=%s button=%s cps=%.1f-%.1f jitter=%.2f onlyOnEntity=%b "
                                    + "clickOnBlocks=%b weaponOnly=%b pauseWhileUsing=%b",
                            c.autoclickerEnabled, c.mode, c.button, c.minCps, c.maxCps, c.jitter,
                            c.onlyOnEntity, c.clickOnBlocks, c.weaponOnly, c.pauseWhileUsing));
                    return 1;
                })
                .then(ClientCommands.literal("status").executes(ctx -> {
                    reply(ctx.getSource(), "clicker: " + AutoClicker.status);
                    return 1;
                }))
                .then(ClientCommands.literal("toggle").executes(ctx -> {
                    Config c = Config.get();
                    c.autoclickerEnabled = !c.autoclickerEnabled;
                    Config.save();
                    reply(ctx.getSource(), "Autoclicker " + (c.autoclickerEnabled ? "ON" : "OFF"));
                    return 1;
                }))
                .then(ClientCommands.literal("cps")
                        .then(ClientCommands.argument("min", DoubleArgumentType.doubleArg(1, 20))
                                .then(ClientCommands.argument("max", DoubleArgumentType.doubleArg(1, 20))
                                        .executes(ctx -> {
                                            Config c = Config.get();
                                            c.minCps = DoubleArgumentType.getDouble(ctx, "min");
                                            c.maxCps = DoubleArgumentType.getDouble(ctx, "max");
                                            Config.save();
                                            reply(ctx.getSource(), String.format(Locale.ROOT,
                                                    "CPS %.1f-%.1f", c.minCps, c.maxCps));
                                            return 1;
                                        }))))
                .then(ClientCommands.literal("jitter")
                        .then(ClientCommands.argument("value", DoubleArgumentType.doubleArg(0, 0.5))
                                .executes(ctx -> {
                                    Config.get().jitter = DoubleArgumentType.getDouble(ctx, "value");
                                    Config.save();
                                    reply(ctx.getSource(), "Jitter set");
                                    return 1;
                                })))
                .then(ClientCommands.literal("mode")
                        .then(ClientCommands.argument("mode", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    b.suggest("hold");
                                    b.suggest("auto");
                                    return b.buildFuture();
                                })
                                .executes(ctx -> {
                                    String m = StringArgumentType.getString(ctx, "mode").toLowerCase(Locale.ROOT);
                                    Config.get().mode = m.equals("auto") ? Config.Mode.AUTO : Config.Mode.HOLD;
                                    Config.save();
                                    reply(ctx.getSource(), "Mode " + Config.get().mode);
                                    return 1;
                                })))
                .then(ClientCommands.literal("input")
                        .then(ClientCommands.argument("input", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    b.suggest("real");
                                    b.suggest("direct");
                                    return b.buildFuture();
                                })
                                .executes(ctx -> {
                                    String m = StringArgumentType.getString(ctx, "input").toLowerCase(Locale.ROOT);
                                    Config.get().input = m.equals("direct") ? Config.Input.DIRECT : Config.Input.REAL;
                                    Config.save();
                                    reply(ctx.getSource(), "Input " + Config.get().input);
                                    return 1;
                                })))
                .then(ClientCommands.literal("button")
                        .then(ClientCommands.argument("button", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    b.suggest("left");
                                    b.suggest("right");
                                    return b.buildFuture();
                                })
                                .executes(ctx -> {
                                    String m = StringArgumentType.getString(ctx, "button").toLowerCase(Locale.ROOT);
                                    Config.get().button = m.equals("right") ? Config.Button.RIGHT : Config.Button.LEFT;
                                    Config.save();
                                    reply(ctx.getSource(), "Button " + Config.get().button);
                                    return 1;
                                })))
                .then(boolOption("onlyonentity", "onlyOnEntity", v -> Config.get().onlyOnEntity = v))
                .then(boolOption("clickonblocks", "clickOnBlocks", v -> Config.get().clickOnBlocks = v))
                .then(boolOption("weapononly", "weaponOnly", v -> Config.get().weaponOnly = v))
                .then(boolOption("pausewhileusing", "pauseWhileUsing", v -> Config.get().pauseWhileUsing = v)));
    }

    private static void registerEspCommands(
            com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> d) {
        d.register(ClientCommands.literal("esp")
                .executes(ctx -> {
                    Config c = Config.get();
                    reply(ctx.getSource(), String.format(Locale.ROOT,
                            "enabled=%b color=%06X distance=%.0f invisible=%b",
                            c.espEnabled, c.espColor, c.espMaxDistance, c.espInvisible));
                    return 1;
                })
                .then(ClientCommands.literal("toggle").executes(ctx -> {
                    Config c = Config.get();
                    c.espEnabled = !c.espEnabled;
                    Config.save();
                    reply(ctx.getSource(), "ESP " + (c.espEnabled ? "ON" : "OFF"));
                    return 1;
                }))
                .then(ClientCommands.literal("color")
                        .then(ClientCommands.argument("hex", StringArgumentType.word())
                                .executes(ctx -> {
                                    String s = StringArgumentType.getString(ctx, "hex");
                                    if (s.startsWith("#")) s = s.substring(1);
                                    try {
                                        Config.get().espColor = Integer.parseInt(s, 16) & 0xFFFFFF;
                                        Config.save();
                                        reply(ctx.getSource(), "Color #" + s.toUpperCase(Locale.ROOT));
                                        return 1;
                                    } catch (NumberFormatException e) {
                                        reply(ctx.getSource(), "Use a hex color like ff0000");
                                        return 0;
                                    }
                                })))
                .then(ClientCommands.literal("distance")
                        .then(ClientCommands.argument("blocks", DoubleArgumentType.doubleArg(0, 512))
                                .executes(ctx -> {
                                    Config.get().espMaxDistance = DoubleArgumentType.getDouble(ctx, "blocks");
                                    Config.save();
                                    reply(ctx.getSource(), "Distance set (0 = unlimited)");
                                    return 1;
                                })))
                .then(boolOption("invisible", "invisible", v -> Config.get().espInvisible = v)));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> boolOption(
            String literal, String label, java.util.function.Consumer<Boolean> setter) {
        return ClientCommands.literal(literal)
                .then(ClientCommands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean v = BoolArgumentType.getBool(ctx, "value");
                            setter.accept(v);
                            Config.save();
                            reply(ctx.getSource(), label + " = " + v);
                            return 1;
                        }));
    }
}
