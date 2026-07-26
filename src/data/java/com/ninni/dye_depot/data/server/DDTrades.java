package com.ninni.dye_depot.data.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ninni.dye_depot.data.DDJsonProvider;
import com.ninni.dye_depot.data.ModCompat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

/**
 * 26.2 replaces the trade-helper API with data-driven villager-trade records.
 * This provider preserves all 128 villager offers and 16 wandering offers from
 * the Fabric 1.21 implementation.
 */
public final class DDTrades extends DDJsonProvider {

    private static final Set<String> SHEPHERD_LEVEL_2_DYES =
            Set.of("tan", "aqua", "coral", "beige", "amber");
    private static final Set<String> SHEPHERD_LEVEL_3_DYES =
            Set.of("rose", "slate", "mint", "forest", "ginger");
    private static final Set<String> SHEPHERD_LEVEL_4_DYES =
            Set.of("maroon", "indigo", "teal", "verdant", "navy", "olive");

    public DDTrades(FabricPackOutput output) {
        super(output);
    }

    @Override
    protected void generate(Output output) {
        Map<String, JsonArray> tags = new LinkedHashMap<>();

        ModCompat.colors().forEach(color -> {
            String name = color.getSerializedName();

            register(output, tags, "cartographer/level_4", "emerald_" + name + "_banner",
                    trade("minecraft:emerald", 3, item(name, "banner"), 1, 12, 15));

            register(output, tags, "mason/level_4", "emerald_" + name + "_terracotta",
                    trade("minecraft:emerald", 1, item(name, "terracotta"), 1, 12, 15));
            register(output, tags, "mason/level_4", "emerald_" + name + "_glazed_terracotta",
                    trade("minecraft:emerald", 1, item(name, "glazed_terracotta"), 1, 12, 15));

            if (SHEPHERD_LEVEL_2_DYES.contains(name)) {
                register(output, tags, "shepherd/level_2", name + "_dye_emerald",
                        trade(item(name, "dye"), 12, "minecraft:emerald", 1, 16, 30));
            }
            register(output, tags, "shepherd/level_2", "emerald_" + name + "_wool",
                    trade("minecraft:emerald", 1, item(name, "wool"), 1, 16, 5));
            register(output, tags, "shepherd/level_2", "emerald_" + name + "_carpet",
                    trade("minecraft:emerald", 1, item(name, "carpet"), 4, 16, 5));

            if (SHEPHERD_LEVEL_3_DYES.contains(name)) {
                register(output, tags, "shepherd/level_3", name + "_dye_emerald",
                        trade(item(name, "dye"), 12, "minecraft:emerald", 1, 16, 30));
            }
            register(output, tags, "shepherd/level_3", "emerald_" + name + "_bed",
                    trade("minecraft:emerald", 3, item(name, "bed"), 1, 12, 10));

            if (SHEPHERD_LEVEL_4_DYES.contains(name)) {
                register(output, tags, "shepherd/level_4", name + "_dye_emerald",
                        trade(item(name, "dye"), 12, "minecraft:emerald", 1, 16, 30));
            }
            register(output, tags, "shepherd/level_4", "emerald_" + name + "_banner",
                    trade("minecraft:emerald", 3, item(name, "banner"), 1, 12, 15));

            register(output, tags, "wandering_trader/common", "emerald_" + name + "_dye",
                    trade("minecraft:emerald", 1, item(name, "dye"), 3, 12, null));
        });

        tags.forEach((tag, values) -> {
            JsonObject json = new JsonObject();
            json.add("values", values);
            output.accept(data("minecraft", "tags/villager_trade", tag), json);
        });
    }

    private void register(
            Output output,
            Map<String, JsonArray> tags,
            String tag,
            String name,
            JsonObject trade
    ) {
        int separator = tag.lastIndexOf('/');
        String owner = tag.substring(0, separator);
        String group = tag.substring(separator + 1);
        String recordPath = group.startsWith("level_")
                ? owner + "/" + group.substring("level_".length()) + "/" + name
                : owner + "/" + name;
        String id = "dye_depot:" + recordPath;
        output.accept(data("dye_depot", "villager_trade", recordPath), trade);
        tags.computeIfAbsent(tag, ignored -> new JsonArray()).add(id);
    }

    private static JsonObject trade(
            String wanted,
            int wantedCount,
            String given,
            int givenCount,
            int maxUses,
            Integer xp
    ) {
        JsonObject trade = new JsonObject();
        trade.add("gives", stack(given, givenCount));
        trade.addProperty("max_uses", maxUses);
        trade.addProperty("reputation_discount", 0.05F);
        trade.add("wants", stack(wanted, wantedCount));
        if (xp != null) {
            trade.addProperty("xp", xp);
        }
        return trade;
    }

    private static JsonObject stack(String id, int count) {
        JsonObject stack = new JsonObject();
        stack.addProperty("id", id);
        if (count != 1) {
            stack.addProperty("count", count);
        }
        return stack;
    }

    private static String item(String color, String suffix) {
        return "dye_depot:" + color + "_" + suffix;
    }

    @Override
    public String getName() {
        return "Dye Depot villager trades";
    }
}
