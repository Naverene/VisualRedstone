package net.neverandy.vr;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** The client settings, in config/vr-client.properties. The file is written with the defaults the first time. */
public final class Config {

    /** How many blocks around the player get signal numbers, component info and hints (1 to 16). */
    public static int range = 8;
    /** Width of the bright box outline (0.5 to 10). The dark border under it is twice as wide. */
    public static float thickness = 3.5F;
    public static int highlightColor = 0x40FF40;
    public static int pathColor = 0x40C0FF;
    public static int qcColor = 0xFFAA00;
    public static boolean signalNumbers = true;
    public static boolean powerPath = true;
    public static boolean componentInfo = true;
    public static boolean qcHints = true;

    private static final String HEADER = String.join("\n",
        "Visual Redstone client settings",
        "range: how many blocks around the player get signal numbers, component info and hints (1 to 16)",
        "thickness: width of the bright box outline (0.5 to 10); the dark border under it is twice as wide",
        "highlightColor: the box on the block you look at, as #RRGGBB",
        "pathColor: the boxes on the blocks wired to the one you look at, as #RRGGBB",
        "qcColor: quasi-connectivity hints on pistons, dispensers and droppers, as #RRGGBB",
        "signalNumbers: show the signal strength (0 to 15) over redstone dust and powered blocks",
        "powerPath: outline every powered block wired to the one you look at",
        "componentInfo: show the delay of repeaters and the mode of comparators",
        "qcHints: mark pistons, dispensers and droppers that are quasi-powered but have not updated yet");

    private Config() {
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("vr-client.properties");
        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                properties.load(reader);
            } catch (IOException | IllegalArgumentException e) {
                VisualRedstone.LOGGER.warn("Could not read {}, using the defaults", file, e);
            }
        }
        range = Math.max(1, Math.min(16, integer(properties, "range", range)));
        thickness = Math.max(0.5F, Math.min(10.0F, decimal(properties, "thickness", thickness)));
        highlightColor = color(properties, "highlightColor", highlightColor);
        pathColor = color(properties, "pathColor", pathColor);
        qcColor = color(properties, "qcColor", qcColor);
        signalNumbers = bool(properties, "signalNumbers", signalNumbers);
        powerPath = bool(properties, "powerPath", powerPath);
        componentInfo = bool(properties, "componentInfo", componentInfo);
        qcHints = bool(properties, "qcHints", qcHints);

        // Write every setting back, so a new or partial file lists them all.
        Properties out = new Properties();
        out.setProperty("range", Integer.toString(range));
        out.setProperty("thickness", Float.toString(thickness));
        out.setProperty("highlightColor", String.format("#%06X", highlightColor));
        out.setProperty("pathColor", String.format("#%06X", pathColor));
        out.setProperty("qcColor", String.format("#%06X", qcColor));
        out.setProperty("signalNumbers", Boolean.toString(signalNumbers));
        out.setProperty("powerPath", Boolean.toString(powerPath));
        out.setProperty("componentInfo", Boolean.toString(componentInfo));
        out.setProperty("qcHints", Boolean.toString(qcHints));
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                out.store(writer, HEADER);
            }
        } catch (IOException e) {
            VisualRedstone.LOGGER.warn("Could not write {}", file, e);
        }
    }

    private static int integer(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, "").trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float decimal(Properties properties, String key, float fallback) {
        try {
            return Float.parseFloat(properties.getProperty(key, "").trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int color(Properties properties, String key, int fallback) {
        String text = properties.getProperty(key, "").trim();
        return text.matches("#[0-9a-fA-F]{6}") ? Integer.parseInt(text.substring(1), 16) : fallback;
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        String text = properties.getProperty(key, "").trim();
        return text.isEmpty() ? fallback : Boolean.parseBoolean(text);
    }
}
