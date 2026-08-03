package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.registry.DDDyes;
import com.ninni.dye_depot.registry.DDItems;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

final class DDPolymerItems {
    private DDPolymerItems() {
    }

    static void register() {
        // This global final pass also protects vanilla containers carrying Dye
        // Depot items or block-entity data.
        PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original, client, context) ->
                DDPolymerItemSanitizer.sanitize(client));

        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && id.getNamespace().equals(DyeDepot.MOD_ID)) {
                PolymerItem.registerOverlay(item, new Overlay(item, vanillaCarrier(item)));
            }
        }
    }

    private static Item vanillaCarrier(Item source) {
        for (DDDyes entry : DDDyes.values()) {
            var color = entry.get();
            var vanillaColor = DDPolymerColors.vanillaColor(color);
            if (source == DDItems.DYES.getOrThrow(color)) {
                return Items.DYE.pick(vanillaColor);
            }
            if (source == DDItems.BANNERS.getOrThrow(color)) {
                return Items.BANNER.pick(vanillaColor);
            }
            if (source == DDItems.SHULKER_BOXES.getOrThrow(color)) {
                return Items.DYED_SHULKER_BOX.pick(vanillaColor);
            }
        }

        return Items.TRIAL_KEY;
    }

    private record Overlay(Item source, Item carrier) implements PolymerItem {
        @Override
        public Item getPolymerItem(ItemStack stack, PacketContext context) {
            return carrier;
        }

        @Override
        public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider registries) {
            Identifier sourceModel = BuiltInRegistries.ITEM.getKey(source);
            Identifier override = stack.get(DataComponents.ITEM_MODEL);
            if (override != null
                    && DyeDepot.MOD_ID.equals(override.getNamespace())
                    && !override.equals(sourceModel)) {
                return override;
            }
            var patterns = stack.get(DataComponents.BANNER_PATTERNS);
            if (carrier instanceof net.minecraft.world.item.BannerItem
                    && patterns != null
                    && !patterns.layers().isEmpty()) {
                // The authored static model preserves an exact custom base but
                // cannot render dynamic layers. Patterned banners use a safe
                // vanilla banner special renderer generated for this color;
                // the outbound sanitizer also makes every layer codec-safe.
                return DyeDepot.modLoc("polymer/" + BuiltInRegistries.ITEM.getKey(source).getPath() + "_patterned");
            }
            return sourceModel;
        }

        @Override
        public void modifyBasePolymerItemStack(
                ItemStack polymer,
                ItemStack original,
                PacketContext context,
                HolderLookup.Provider registries
        ) {
            DDPolymerItemSanitizer.sanitize(polymer);
        }

        @Override
        public boolean isPolymerBlockInteraction(
                BlockState state,
                ServerPlayer player,
                InteractionHand hand,
                ItemStack stack,
                ServerLevel world,
                BlockHitResult hit,
                InteractionResult result
        ) {
            return source instanceof BlockItem;
        }

        @Override
        public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(
                BlockState state,
                ServerPlayer player,
                InteractionHand hand,
                ItemStack stack,
                ServerLevel world,
                BlockHitResult hit
        ) {
            return source instanceof BlockItem;
        }
    }
}
