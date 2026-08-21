package hero.bane.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import hero.bane.HerosGotMotion;
import hero.bane.config.HerosGotMotionConfig;
import hero.bane.config.HerosGotMotionConfig.Data;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HerosGotMotionCommand {
    public enum Mode {
        BAR,
        TEXT,
        BOTH
    }

    private static final Minecraft client = Minecraft.getInstance();

    private static Mode currentMode = Mode.BAR;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        SuggestionProvider<FabricClientCommandSource> suggestX = (ctx, b) -> {
            b.suggest(Double.toString(HerosGotMotion.speedOffsetX), Component.literal("current X"));
            return b.buildFuture();
        };
        SuggestionProvider<FabricClientCommandSource> suggestY = (ctx, b) -> {
            b.suggest(Double.toString(HerosGotMotion.speedOffsetY), Component.literal("current Y"));
            return b.buildFuture();
        };

        dispatcher.register(
                literal("h-motion")
                        .executes(ctx -> {
                            String s =
                                    "Enabled: " + HerosGotMotion.enabled +
                                        "\n" +
                                    "Mode: " +
                                    currentMode.name().charAt(0) +
                                    currentMode.name().substring(1).toLowerCase() +
                                        "\n" +
                                    "Smooth Bar Animation: " + HerosGotMotion.smoothBar +
                                        "\n" +
                                    "Text Opacity: " + HerosGotMotion.textOpacity +
                                        "\n" +
                                    "Speed Text Offset: " +
                                    HerosGotMotion.speedOffsetX + ", " +
                                    HerosGotMotion.speedOffsetY +
                                        "\n" +
                                    "XP Bar Hide Length: " + HerosGotMotion.hideTicksAfterXp + " ticks";
                            say("Mod Values:");
                            say(false, s, 0xFFFF00);
                            return 1;
                        })
                        .then(ClientCommandManager.literal("textOpacity")
                                .then(ClientCommandManager.argument("value", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            builder.suggest("reset");
                                            builder.suggest("100%");
                                            return builder.buildFuture();
                                        })
                                        .executes(HerosGotMotionCommand::setOpacity)))
                        .then(literal("smoothBar")
                                .executes(ctx -> {
                                    HerosGotMotion.smoothBar = !HerosGotMotion.smoothBar;
                                    say("smoothBar set to " + HerosGotMotion.smoothBar);
                                    if (HerosGotMotion.smoothBar) {
                                        say(
                                                "Having smoothbar will delay the bar by a tick as it transitions smoothly rather than instantly",
                                                0xFFFF00
                                        );
                                    }
                                    saveConfig();
                                    return 1;
                                })
                                .then(ClientCommandManager.argument("value", StringArgumentType.word())
                                        .suggests((c, b) -> {
                                            b.suggest("true");
                                            b.suggest("false");
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String v = StringArgumentType.getString(ctx, "value");
                                            if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false")) {
                                                HerosGotMotion.smoothBar = Boolean.parseBoolean(v);
                                                say("smoothBar set to " + HerosGotMotion.smoothBar);
                                                saveConfig();
                                            } else {
                                                say("Invalid value: " + v, 0xFF5555);
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(literal("mode")
                                .executes(ctx -> {
                                    cycleMode();
                                    say("Mode set to " + currentMode.name().charAt(0) + currentMode.name().substring(1).toLowerCase());
                                    saveConfig();
                                    return 1;
                                })
                                .then(ClientCommandManager.argument("value", StringArgumentType.word())
                                        .suggests((c, b) -> {
                                            b.suggest("bar");
                                            b.suggest("text");
                                            b.suggest("both");
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String arg = StringArgumentType.getString(ctx, "value").toUpperCase();
                                            try {
                                                setMode(Mode.valueOf(arg));
                                                say("Mode set to " + currentMode.name().charAt(0) + currentMode.name().substring(1).toLowerCase());
                                                saveConfig();
                                            } catch (IllegalArgumentException e) {
                                                say("Invalid mode: " + arg, 0xFF5555);
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(literal("textOffset")
                                .executes(ctx -> {
                                    HerosGotMotion.speedOffsetX = HerosGotMotion.DEFAULT_SPEED_OFFSET_X;
                                    HerosGotMotion.speedOffsetY = HerosGotMotion.DEFAULT_SPEED_OFFSET_Y;
                                    say("Speed text offset reset to X=" + HerosGotMotion.speedOffsetX +
                                            ", Y=" + HerosGotMotion.speedOffsetY);
                                    saveConfig();
                                    return 1;
                                })
                                .then(ClientCommandManager.argument("x", DoubleArgumentType.doubleArg())
                                        .suggests(suggestX)
                                        .then(ClientCommandManager.argument("y", DoubleArgumentType.doubleArg())
                                                .suggests(suggestY)
                                                .executes(ctx -> {
                                                    double x = DoubleArgumentType.getDouble(ctx, "x");
                                                    double y = DoubleArgumentType.getDouble(ctx, "y");
                                                    HerosGotMotion.speedOffsetX = x;
                                                    HerosGotMotion.speedOffsetY = y;
                                                    say("Speed text offset set to X=" + x + ", Y=" + y);
                                                    saveConfig();
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(literal("xpHideTicks")
                                .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            int t = IntegerArgumentType.getInteger(ctx, "ticks");
                                            HerosGotMotion.hideTicksAfterXp = t;
                                            if (t == 0) {
                                                say("xpHideTicks set to 0, the bar will no longer hide on XP gain");
                                            } else {
                                                say("xpHideTicks set to " + t);
                                            }
                                            saveConfig();
                                            return 1;
                                        })
                                )
                        )
        );
    }

    public static int setOpacity(CommandContext<FabricClientCommandSource> ctx) {
        String input = StringArgumentType.getString(ctx, "value");

        if (input.equalsIgnoreCase("reset")) {
            HerosGotMotion.textOpacity = 255;
            saveConfig();
            say("Opacity reset to 255");
            return 0;
        }

        try {
            int value;
            if (input.endsWith("%")) {
                String num = input.substring(0, input.length() - 1);
                float percent = Float.parseFloat(num);
                value = (int)((percent / 100f) * 255f);
            } else {
                value = Integer.parseInt(input);
            }

            if (value < 26) {
                say("Opacities < 26 are invisible so setting to 26");
            }
            HerosGotMotion.textOpacity = Math.clamp(value, 26, 255);

            saveConfig();
            say("Opacity set to " + HerosGotMotion.textOpacity);
            return 0;
        } catch (NumberFormatException e) {
            say("Invalid Input", 0xFF5555);
            return 1;
        }
    }

    public static void setModeFromConfig(String name) {
        try {
            setMode(Mode.valueOf(name));
        } catch (Exception ignored) {
            setMode(Mode.BAR);
        }
    }

    public static Mode getMode() {
        return currentMode;
    }

    public static void setMode(Mode mode) {
        currentMode = mode;
        switch (mode) {
            case BAR -> {
                HerosGotMotion.showBar.set(true);
                HerosGotMotion.showTexts.set(false);
            }
            case TEXT -> {
                HerosGotMotion.showBar.set(false);
                HerosGotMotion.showTexts.set(true);
            }
            case BOTH -> {
                HerosGotMotion.showBar.set(true);
                HerosGotMotion.showTexts.set(true);
            }
        }
    }

    private static void cycleMode() {
        switch (currentMode) {
            case BAR -> setMode(Mode.BOTH);
            case BOTH -> setMode(Mode.TEXT);
            case TEXT -> setMode(Mode.BAR);
        }
    }

    public static void saveConfig() {
        Data d = snapshot();
        HerosGotMotionConfig.save(d);
    }

    public static Data snapshot() {
        Data d = new Data();
        d.mode = currentMode.name();
        d.speedOffsetX = HerosGotMotion.speedOffsetX;
        d.speedOffsetY = HerosGotMotion.speedOffsetY;
        d.smoothBar = HerosGotMotion.smoothBar;
        d.textOpacity = HerosGotMotion.textOpacity;
        d.hideTicksAfterXp = HerosGotMotion.hideTicksAfterXp;
        return d;
    }

    public static void say(boolean sayModId, String message, int color) {
        if(sayModId) {
            message = "[HerosGotMotion] "+message;
        }
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message).withStyle(style -> style.withColor(color)), false);
        }
    }

    public static void say(String message) {
        say(true, message, 0xFFFFFF);
    }

    public static void say(String message, int color) {
        say(true, message, color);
    }
}
