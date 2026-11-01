package dev.local.pvptest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** JSON config stored at config/pvptest.json. Edit the file or use the /ac and /esp commands. */
public class Config {
    public enum Mode { HOLD, AUTO }
    public enum Button { LEFT, RIGHT }

    // ---- Autoclicker ----
    public boolean autoclickerEnabled = false;
    /** HOLD: clicks only while you hold the mouse button. AUTO: clicks continuously while enabled. */
    public Mode mode = Mode.HOLD;
    public Button button = Button.LEFT;
    public double minCps = 8.0;
    public double maxCps = 12.0;
    /** Extra random variation applied to each delay, as a fraction of the delay (0.0 - 0.5). */
    public double jitter = 0.15;
    /** Only click while the crosshair is on an entity (left click). */
    public boolean onlyOnEntity = true;
    /** Allow clicking while the crosshair is on a block (left click normally mines instead). */
    public boolean clickOnBlocks = false;
    /** Only click while holding a sword or axe. */
    public boolean weaponOnly = false;
    /** Don't click while the player is using an item (eating, blocking, drawing a bow). */
    public boolean pauseWhileUsing = true;

    // ---- ESP ----
    public boolean espEnabled = false;
    /** Outline color as 0xRRGGBB. */
    public int espColor = 0xFF3030;
    /** Max distance in blocks, 0 = unlimited. */
    public double espMaxDistance = 0;
    public boolean espInvisible = true;

    // ---- persistence ----
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance = new Config();

    public static Config get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("pvptest.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                Config loaded = GSON.fromJson(r, Config.class);
                if (loaded != null) instance = loaded;
            } catch (Exception e) {
                System.err.println("[pvptest] Failed to read config, using defaults: " + e);
            }
        }
        instance.sanitize();
        save();
    }

    public static void save() {
        instance.sanitize();
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            System.err.println("[pvptest] Failed to save config: " + e);
        }
    }

    private void sanitize() {
        if (mode == null) mode = Mode.HOLD;
        if (button == null) button = Button.LEFT;
        minCps = Math.max(1.0, Math.min(minCps, 20.0));
        maxCps = Math.max(minCps, Math.min(maxCps, 20.0));
        jitter = Math.max(0.0, Math.min(jitter, 0.5));
        espColor &= 0xFFFFFF;
        espMaxDistance = Math.max(0, espMaxDistance);
    }
}
