package one_in_10000_embryo_of_philosophy;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue EMBRYO_OF_PHILOSOPHY;
    public static final ForgeConfigSpec.BooleanValue IRONTOMB;
    public static final ForgeConfigSpec.BooleanValue DEBUG;

    static {
        BUILDER.push("Spawning Settings");
        
        EMBRYO_OF_PHILOSOPHY = BUILDER
            .comment("Enable 1 in 10000 chance to spawn Embryo of Philosophy every 20 ticks (1 second)")
            .define("embryo_of_philosophy", true);

        IRONTOMB = BUILDER
            .comment("[WIP / PLACEHOLDER]")
            .define("Irontomb", false);

        DEBUG = BUILDER
            .comment("Enable debug mode WARNING anyone can use this mod's commands without admin permissions if enabled")
            .define("debug", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
