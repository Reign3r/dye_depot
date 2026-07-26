package com.ninni.dye_depot.registry;

import com.ninni.dye_depot.DyeDepot;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

public class DDPoiTypes {

    public static final ResourceKey<PoiType> HOME = register("home", DDBlocks.BEDS.values()
            .map(Block::getStateDefinition)
            .flatMap(it -> it.getPossibleStates().stream())
            .filter(it -> it.getValue(BedBlock.PART) == BedPart.HEAD)
    );

    private static ResourceKey<PoiType> register(String name, Stream<BlockState> possibleStates) {
        var possibleStatesSet = possibleStates.collect(Collectors.toSet());
        PoiHelper.register(DyeDepot.modLoc(name), 1, 1, possibleStatesSet);
        return DyeDepot.key(Registries.POINT_OF_INTEREST_TYPE, name);
    }

}
