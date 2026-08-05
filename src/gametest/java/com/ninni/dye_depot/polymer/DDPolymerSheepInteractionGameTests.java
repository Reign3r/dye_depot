package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.mixin.SheepDataAccessor;
import com.ninni.dye_depot.registry.DDDyes;
import com.ninni.dye_depot.registry.DDItems;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import java.util.ArrayList;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class DDPolymerSheepInteractionGameTests {
    @GameTest
    public void everyCustomDyeColorsSheepThroughRealInteraction(GameTestHelper helper) {
        var player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        var packetPlayer = helper.makeMockServerPlayerInLevel();
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, new BlockPos(1, 2, 1));
        PolymerEntity overlay = PolymerEntity.get(sheep);

        for (DDDyes entry : DDDyes.values()) {
            DyeColor color = entry.get();
            ItemStack dye = new ItemStack(DDItems.DYES.getOrThrow(color), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, dye);

            helper.assertTrue(
                    player.interactOn(sheep, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction(),
                    color.getName() + " dye interaction colors a sheep"
            );
            helper.assertValueEqual(
                    sheep.getColor(),
                    color,
                    color.getName() + " remains the exact authoritative sheep color"
            );
            helper.assertValueEqual(
                    player.getItemInHand(InteractionHand.MAIN_HAND).getCount(),
                    1,
                    color.getName() + " sheep interaction consumes exactly one dye in survival"
            );

            if (DDPolymerSheepShaderPack.isEnabled()) {
                var woolAccessor = SheepDataAccessor.dyeDepot$getWoolData();
                var tracked = new ArrayList<SynchedEntityData.DataValue<?>>();
                tracked.add(SynchedEntityData.DataValue.create(
                        woolAccessor,
                        sheep.getEntityData().get(woolAccessor)
                ));
                overlay.modifyRawTrackedData(tracked, packetPlayer, false);
                byte clientWool = (Byte) tracked.getFirst().value();
                helper.assertValueEqual(
                        clientWool & 15,
                        DDPolymerSheepShaderPack.donorColor(color).getId(),
                        color.getName() + " interaction selects the codec-safe native donor"
                );
                var attributes = new ArrayList<ClientboundUpdateAttributesPacket.AttributeSnapshot>();
                overlay.modifyRawEntityAttributeData(attributes, packetPlayer, false);
                var scale = attributes.stream()
                        .filter(snapshot -> snapshot.attribute().equals(Attributes.SCALE))
                        .findFirst()
                        .orElseThrow();
                helper.assertValueEqual(
                        DDPolymerSheepShaderPack.decodeScaleClass(scale.base()),
                        DDPolymerSheepShaderPack.shaderClass(color),
                        color.getName() + " interaction selects the exact shader residue class"
                );
                helper.assertTrue(
                        DDPolymerEntities.sheepWoolHolder(overlay) == null,
                        color.getName() + " uses no display-entity coat"
                );
            } else {
                ElementHolder holder = DDPolymerEntities.sheepWoolHolder(overlay);
                helper.assertTrue(holder != null, color.getName() + " creates the Polymer sheep coat");
                holder.tick();
                ItemDisplayElement head = (ItemDisplayElement) holder.getElements().getFirst();
                helper.assertValueEqual(
                        head.getItem().get(DataComponents.DYED_COLOR),
                        new DyedItemColor(nativeSheepTint(color) & 0xFFFFFF),
                        color.getName() + " coat carries the exact 26.2 sheep tint"
                );
            }

            ItemStack duplicateDye = new ItemStack(DDItems.DYES.getOrThrow(color), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, duplicateDye);
            helper.assertFalse(
                    player.interactOn(sheep, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction(),
                    color.getName() + " does not re-dye an already matching sheep"
            );
            helper.assertValueEqual(
                    player.getItemInHand(InteractionHand.MAIN_HAND).getCount(),
                    2,
                    color.getName() + " is not consumed when the sheep already has that color"
            );
        }

        ElementHolder holder = DDPolymerEntities.sheepWoolHolder(overlay);
        if (DDPolymerSheepShaderPack.isEnabled()) {
            helper.assertTrue(holder == null, "native shader mode leaves glow and outline to the real sheep renderer");
            sheep.setGlowingTag(true);
            helper.assertTrue(sheep.isCurrentlyGlowing(), "native sheep glow state remains authoritative");
            sheep.setGlowingTag(false);
            helper.assertFalse(sheep.isCurrentlyGlowing(), "native sheep glow state clears normally");
            helper.succeed();
            return;
        }
        helper.assertTrue(holder != null, "custom sheep retains its coat for outline coverage");
        sheep.setGlowingTag(true);
        holder.tick();
        for (var element : holder.getElements()) {
            ItemDisplayElement part = (ItemDisplayElement) element;
            helper.assertTrue(part.isGlowing(), "visible glowing sheep outlines every coat part");
            helper.assertValueEqual(
                    part.getGlowColorOverride(),
                    sheep.getTeamColor(),
                    "coat outline uses the sheep's current team color"
            );
        }

        sheep.setGlowingTag(false);
        holder.tick();
        for (var element : holder.getElements()) {
            ItemDisplayElement part = (ItemDisplayElement) element;
            helper.assertFalse(part.isGlowing(), "clearing glow also clears every coat-part outline");
            helper.assertValueEqual(part.getGlowColorOverride(), -1, "non-glowing coat has no forced outline color");
        }
        helper.succeed();
    }

    private static int nativeSheepTint(DyeColor color) {
        if (color == DyeColor.WHITE) {
            return 0xFFE6E6E6;
        }
        int rgb = color.getTextureDiffuseColor();
        return ARGB.color(
                255,
                (int) (((rgb >> 16) & 0xFF) * 0.75f),
                (int) (((rgb >> 8) & 0xFF) * 0.75f),
                (int) ((rgb & 0xFF) * 0.75f)
        );
    }
}
