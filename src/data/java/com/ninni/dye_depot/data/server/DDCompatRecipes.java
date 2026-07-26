package com.ninni.dye_depot.data.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ninni.dye_depot.data.DDJsonProvider;
import com.ninni.dye_depot.data.ModCompat;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.world.item.DyeColor;

/**
 * The 48 optional recipes and matching advancements, generated without
 * resolving the optional result items in the live item registry.
 */
public final class DDCompatRecipes extends DDJsonProvider {

    public DDCompatRecipes(FabricPackOutput output) {
        super(output);
    }

    @Override
    protected void generate(Output output) {
        ModCompat.colors().forEach(color -> {
            generateCandleHolder(output, color, ModCompat.SUPPLEMENTARIES, "candle_holder", "minecraft:iron_ingot", false);
            generateCandleHolder(output, color, ModCompat.SUPPLEMENTARIES_SQUARED, "gold_candle_holder", "minecraft:gold_ingot", true);
            generateFlag(output, color);
        });
    }

    private void generateCandleHolder(
            Output output,
            DyeColor color,
            String namespace,
            String type,
            String ingot,
            boolean squared
    ) {
        String colorName = color.getSerializedName();
        String id = namespace + ":" + type + "_" + colorName;
        String candle = "dye_depot:" + colorName + "_candle";

        JsonObject key = new JsonObject();
        key.addProperty("C", candle);
        key.addProperty("N", ingot);
        JsonArray pattern = new JsonArray();
        if (squared) {
            pattern.add("C");
            pattern.add("N");
        } else {
            pattern.add("NCN");
            pattern.add(" N ");
        }

        JsonObject recipe = shapedRecipe(id, type, key, pattern);
        ModCompat.addSupplementariesConditions(recipe, namespace, "candle_holder");
        output.accept(data(namespace, "recipe", type + "_" + colorName), recipe);

        JsonObject advancement = recipeAdvancement(id, "has_candle", candle);
        ModCompat.addSupplementariesConditions(advancement, namespace, "candle_holder");
        output.accept(data(namespace, "advancement/recipes/decorations", type + "_" + colorName), advancement);
    }

    private void generateFlag(Output output, DyeColor color) {
        String colorName = color.getSerializedName();
        String name = "flag_" + colorName;
        String id = ModCompat.SUPPLEMENTARIES + ":" + name;
        String wool = "dye_depot:" + colorName + "_wool";

        JsonObject key = new JsonObject();
        key.addProperty("#", wool);
        key.addProperty("|", "minecraft:stick");
        JsonArray pattern = new JsonArray();
        pattern.add("###");
        pattern.add("###");
        pattern.add("|  ");

        JsonObject recipe = shapedRecipe(id, "flag", key, pattern);
        ModCompat.addSupplementariesConditions(recipe, ModCompat.SUPPLEMENTARIES, "flag");
        output.accept(data(ModCompat.SUPPLEMENTARIES, "recipe", name), recipe);

        JsonObject advancement = recipeAdvancement(id, "has_wool", wool);
        ModCompat.addSupplementariesConditions(advancement, ModCompat.SUPPLEMENTARIES, "flag");
        output.accept(data(ModCompat.SUPPLEMENTARIES, "advancement/recipes/decorations", name), advancement);
    }

    private static JsonObject shapedRecipe(String id, String group, JsonObject key, JsonArray pattern) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("category", "misc");
        recipe.addProperty("group", group);
        recipe.add("key", key);
        recipe.add("pattern", pattern);
        JsonObject result = new JsonObject();
        result.addProperty("id", id);
        recipe.add("result", result);
        return recipe;
    }

    private static JsonObject recipeAdvancement(String recipeId, String criterionName, String itemId) {
        JsonObject advancement = new JsonObject();
        advancement.addProperty("parent", "minecraft:recipes/root");

        JsonObject criteria = new JsonObject();
        criteria.add(criterionName, inventoryCriterion(itemId));
        JsonObject hasRecipe = new JsonObject();
        hasRecipe.addProperty("trigger", "minecraft:recipe_unlocked");
        JsonObject recipeCondition = new JsonObject();
        recipeCondition.addProperty("recipe", recipeId);
        hasRecipe.add("conditions", recipeCondition);
        criteria.add("has_the_recipe", hasRecipe);
        advancement.add("criteria", criteria);

        JsonArray requirement = new JsonArray();
        requirement.add("has_the_recipe");
        requirement.add(criterionName);
        JsonArray requirements = new JsonArray();
        requirements.add(requirement);
        advancement.add("requirements", requirements);

        JsonObject rewards = new JsonObject();
        JsonArray recipes = new JsonArray();
        recipes.add(recipeId);
        rewards.add("recipes", recipes);
        advancement.add("rewards", rewards);
        return advancement;
    }

    private static JsonObject inventoryCriterion(String itemId) {
        JsonObject criterion = new JsonObject();
        criterion.addProperty("trigger", "minecraft:inventory_changed");
        JsonObject predicate = new JsonObject();
        predicate.addProperty("items", itemId);
        JsonArray items = new JsonArray();
        items.add(predicate);
        JsonObject conditions = new JsonObject();
        conditions.add("items", items);
        criterion.add("conditions", conditions);
        return criterion;
    }

    @Override
    public String getName() {
        return "Dye Depot optional compatibility recipes";
    }
}
