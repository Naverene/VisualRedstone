package net.neverandy.vr;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** The client settings, in config/vr-client.cfg. */
public final class ClientConfig {

    /** How many blocks around the player get signal numbers, component info and hints. */
    public static int range = 8;
    /** Width of the bright box outline. The dark border under it is twice as wide. */
    public static float thickness = 3.5F;
    public static int highlightColor = 0x40FF40;
    public static int pathColor = 0x40C0FF;
    public static int qcColor = 0xFFAA00;
    public static boolean signalNumbers = true;
    public static boolean powerPath = true;
    public static boolean componentInfo = true;
    public static boolean qcHints = true;

    private ClientConfig() {}

    public static void load(File file) {
        Configuration config = new Configuration(file);
        String category = Configuration.CATEGORY_GENERAL;
        range = config
            .getInt("range", category, range, 1, 16, "How many blocks around the player get signal numbers, component info and hints.");
        thickness = config.getFloat(
            "thickness",
            category,
            thickness,
            0.5F,
            10.0F,
            "Width of the bright box outline. The dark border under it is twice as wide.");
        highlightColor = color(
            config.getString("highlightColor", category, "#40FF40", "Color of the box on the block you look at, as #RRGGBB."),
            highlightColor);
        pathColor = color(
            config.getString(
                "pathColor",
                category,
                "#40C0FF",
                "Color of the boxes on the blocks wired to the one you look at, as #RRGGBB."),
            pathColor);
        qcColor = color(
            config.getString(
                "qcColor",
                category,
                "#FFAA00",
                "Color of quasi-connectivity hints on pistons, dispensers and droppers, as #RRGGBB."),
            qcColor);
        signalNumbers = config.getBoolean(
            "signalNumbers",
            category,
            signalNumbers,
            "Show the signal strength (0 to 15) over redstone dust and powered blocks.");
        powerPath = config
            .getBoolean("powerPath", category, powerPath, "Outline every powered block wired to the one you look at.");
        componentInfo = config.getBoolean(
            "componentInfo",
            category,
            componentInfo,
            "Show the delay of repeaters and the mode of comparators.");
        qcHints = config.getBoolean(
            "qcHints",
            category,
            qcHints,
            "Mark pistons, dispensers and droppers that are quasi-powered but have not updated yet.");
        if (config.hasChanged()) {
            config.save();
        }
    }

    private static int color(String text, int fallback) {
        return text != null && text.matches("#[0-9a-fA-F]{6}") ? Integer.parseInt(text.substring(1), 16) : fallback;
    }
}
