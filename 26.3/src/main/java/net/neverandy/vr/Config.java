package net.neverandy.vr;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The client settings, in config/vr-client.toml. */
public final class Config {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue RANGE;
    public static final ModConfigSpec.DoubleValue THICKNESS;
    public static final ModConfigSpec.ConfigValue<String> HIGHLIGHT_COLOR;
    public static final ModConfigSpec.ConfigValue<String> PATH_COLOR;
    public static final ModConfigSpec.ConfigValue<String> QC_COLOR;
    public static final ModConfigSpec.BooleanValue SIGNAL_NUMBERS;
    public static final ModConfigSpec.BooleanValue POWER_PATH;
    public static final ModConfigSpec.BooleanValue COMPONENT_INFO;
    public static final ModConfigSpec.BooleanValue QC_HINTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        RANGE = builder.comment("How many blocks around the player get signal numbers, component info and hints.")
            .defineInRange("range", 8, 1, 16);
        THICKNESS = builder.comment("Width of the bright box outline. The dark border under it is twice as wide.")
            .defineInRange("thickness", 3.5, 0.5, 10.0);
        HIGHLIGHT_COLOR = builder.comment("Color of the box on the block you look at, as #RRGGBB.")
            .define("highlightColor", "#40FF40", Config::isColor);
        PATH_COLOR = builder.comment("Color of the boxes on the blocks wired to the one you look at, as #RRGGBB.")
            .define("pathColor", "#40C0FF", Config::isColor);
        QC_COLOR = builder.comment("Color of quasi-connectivity hints on pistons, dispensers and droppers, as #RRGGBB.")
            .define("qcColor", "#FFAA00", Config::isColor);
        SIGNAL_NUMBERS = builder.comment("Show the signal strength (0 to 15) over redstone dust and powered blocks.")
            .define("signalNumbers", true);
        POWER_PATH = builder.comment("Outline every powered block wired to the one you look at.")
            .define("powerPath", true);
        COMPONENT_INFO = builder.comment("Show the delay of repeaters and the mode of comparators.")
            .define("componentInfo", true);
        QC_HINTS = builder.comment("Mark pistons, dispensers and droppers that are quasi-powered but have not updated yet.")
            .define("qcHints", true);
        SPEC = builder.build();
    }

    private Config() {
    }

    /** The color as 0xRRGGBB, or the fallback when it is not a valid #RRGGBB value. */
    public static int color(ModConfigSpec.ConfigValue<String> value, int fallback) {
        String text = value.get();
        return isColor(text) ? Integer.parseInt(text.substring(1), 16) : fallback;
    }

    private static boolean isColor(Object value) {
        return value instanceof String && ((String) value).matches("#[0-9a-fA-F]{6}");
    }
}
