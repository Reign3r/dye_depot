package com.ninni.dye_depot.data.client;

import com.google.gson.JsonObject;
import com.ninni.dye_depot.data.DDJsonProvider;
import com.ninni.dye_depot.data.ModCompat;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.world.item.DyeColor;

/**
 * Optional-mod models are emitted by identifier so datagen does not require
 * unavailable 26.2 Supplementaries binaries.
 */
public final class DDCompatModels extends DDJsonProvider {

    private static final String[] FACES = {"ceiling", "floor", "wall"};
    private static final String[] FACINGS = {"east", "north", "south", "west"};
    private static final int[] Y_ROTATIONS = {90, 0, 180, 270};

    public DDCompatModels(FabricPackOutput output) {
        super(output);
    }

    @Override
    protected void generate(Output output) {
        ModCompat.colors().forEach(color -> {
            generateFlag(output, color);
            generatePresent(output, color, false);
            generatePresent(output, color, true);
            generateCandleHolder(output, color, ModCompat.SUPPLEMENTARIES, "candle_holder");
            generateCandleHolder(output, color, ModCompat.SUPPLEMENTARIES_SQUARED, "gold_candle_holder");
            generatePreservedQuirks(output, color);
        });
    }

    private void generateFlag(Output output, DyeColor color) {
        String name = "flag_" + color.getSerializedName();
        output.accept(asset(ModCompat.SUPPLEMENTARIES, "blockstates", name),
                simpleBlockState(ModCompat.SUPPLEMENTARIES + ":block/flag"));
        output.accept(asset(ModCompat.SUPPLEMENTARIES, "models/item", name),
                parentModel(ModCompat.SUPPLEMENTARIES + ":item/flag_black"));
        output.accept(asset(ModCompat.SUPPLEMENTARIES, "items", name),
                clientItem(ModCompat.SUPPLEMENTARIES + ":item/" + name));
    }

    private void generatePresent(Output output, DyeColor color, boolean trapped) {
        String colorName = color.getSerializedName();
        String type = trapped ? "trapped_present" : "present";
        String plural = trapped ? "trapped_presents" : "presents";
        String name = type + "_" + colorName;
        String modelBase = ModCompat.SUPPLEMENTARIES + ":block/" + plural + "/" + colorName;

        JsonObject variants = new JsonObject();
        if (trapped) {
            for (String facing : FACINGS) {
                for (boolean packed : new boolean[]{false, true}) {
                    for (boolean triggered : new boolean[]{false, true}) {
                        variants.add(
                                "facing=" + facing + ",packed=" + packed + ",triggered=" + triggered,
                                modelVariant(modelBase + (packed ? "_closed" : "_opened"))
                        );
                    }
                }
            }
        } else {
            variants.add("packed=false", modelVariant(modelBase + "_opened"));
            variants.add("packed=true", modelVariant(modelBase + "_closed"));
        }
        output.accept(asset(ModCompat.SUPPLEMENTARIES, "blockstates", name), blockState(variants));

        for (boolean closed : new boolean[]{false, true}) {
            String suffix = closed ? "_closed" : "_opened";
            JsonObject textures = new JsonObject();
            textures.addProperty("bottom", ModCompat.SUPPLEMENTARIES + ":block/presents/bottom_" + colorName);
            textures.addProperty("top", ModCompat.SUPPLEMENTARIES + ":block/presents/top_" + colorName);
            textures.addProperty("side", ModCompat.SUPPLEMENTARIES + ":block/" + plural + "/side_" + colorName);
            textures.addProperty("particle", ModCompat.SUPPLEMENTARIES + ":block/" + plural + "/side_" + colorName);
            output.accept(
                    asset(ModCompat.SUPPLEMENTARIES, "models/block/" + plural, colorName + suffix),
                    model(ModCompat.SUPPLEMENTARIES + ":block/present" + suffix + "_template", textures)
            );
        }

        output.accept(asset(ModCompat.SUPPLEMENTARIES, "models/item", name), parentModel(modelBase + "_closed"));
        output.accept(asset(ModCompat.SUPPLEMENTARIES, "items", name),
                clientItem(ModCompat.SUPPLEMENTARIES + ":item/" + name));
    }

    private void generateCandleHolder(Output output, DyeColor color, String namespace, String type) {
        String colorName = color.getSerializedName();
        String name = type + "_" + colorName;
        JsonObject variants = new JsonObject();

        for (int count = 1; count <= 4; count++) {
            for (String face : FACES) {
                for (int facingIndex = 0; facingIndex < FACINGS.length; facingIndex++) {
                    for (boolean lit : new boolean[]{false, true}) {
                        String modelName = colorName + "_" + face + "_" + count + (lit ? "_lit" : "");
                        JsonObject variant = modelVariant(namespace + ":block/candle_holders/" + modelName);
                        if (Y_ROTATIONS[facingIndex] != 0) {
                            variant.addProperty("y", Y_ROTATIONS[facingIndex]);
                        }
                        variants.add(
                                "candles=" + count + ",face=" + face + ",facing=" + FACINGS[facingIndex] + ",lit=" + lit,
                                variant
                        );
                    }
                }
            }
        }
        output.accept(asset(namespace, "blockstates", name), blockState(variants));

        for (int count = 1; count <= 4; count++) {
            for (String face : FACES) {
                for (boolean lit : new boolean[]{false, true}) {
                    String modelName = colorName + "_" + face + "_" + count + (lit ? "_lit" : "");
                    JsonObject textures = new JsonObject();
                    textures.addProperty("all", "dye_depot:block/" + colorName + "_candle" + (lit ? "_lit" : ""));
                    output.accept(
                            asset(namespace, "models/block/candle_holders", modelName),
                            model(namespace + ":block/candle_holders/" + face + "_" + count, textures)
                    );
                }
            }
        }

        JsonObject itemTextures = new JsonObject();
        itemTextures.addProperty("layer0", namespace + ":item/candle_holders/" + colorName);
        output.accept(asset(namespace, "models/item", name), model("minecraft:item/generated", itemTextures));
        output.accept(asset(namespace, "items", name), clientItem(namespace + ":item/" + name));
    }

    private void generatePreservedQuirks(Output output, DyeColor color) {
        String colorName = color.getSerializedName();
        output.accept(
                asset("dye_depot", "models/item", colorName + "_wall_banner"),
                parentModel("minecraft:block/banner")
        );
        output.accept(
                asset("dye_depot", "models/item", "flag_" + colorName),
                parentModel(ModCompat.SUPPLEMENTARIES + ":block/flag")
        );
    }

    private static JsonObject simpleBlockState(String model) {
        JsonObject variants = new JsonObject();
        variants.add("", modelVariant(model));
        return blockState(variants);
    }

    private static JsonObject blockState(JsonObject variants) {
        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return root;
    }

    private static JsonObject modelVariant(String model) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", model);
        return variant;
    }

    private static JsonObject parentModel(String parent) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        return model;
    }

    private static JsonObject model(String parent, JsonObject textures) {
        JsonObject model = parentModel(parent);
        model.add("textures", textures);
        return model;
    }

    private static JsonObject clientItem(String model) {
        JsonObject modelDefinition = new JsonObject();
        modelDefinition.addProperty("type", "minecraft:model");
        modelDefinition.addProperty("model", model);
        JsonObject root = new JsonObject();
        root.add("model", modelDefinition);
        return root;
    }

    @Override
    public String getName() {
        return "Dye Depot optional compatibility models";
    }
}
