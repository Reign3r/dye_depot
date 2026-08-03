package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.mixin.CatDataAccessor;
import com.ninni.dye_depot.mixin.SheepDataAccessor;
import com.ninni.dye_depot.mixin.WolfDataAccessor;
import com.ninni.dye_depot.registry.DDBlocks;
import com.ninni.dye_depot.registry.DDDyes;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;

public final class DDPolymerEntities {
    private static final Map<Sheep, EntityAttachment> SHEEP_WOOL_ATTACHMENTS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private DDPolymerEntities() {
    }

    static void register() {
        // These are vanilla entity types, so attach only the per-entity packet overlay.
        // registerOverlay also marks a type as server-only for registry sync, which would
        // shift vanilla raw IDs and make an unmodded client decode later entity types as
        // the wrong entity (for example, a chicken as a chest minecart).
        PolymerEntityUtils.registerPolymerEntityConstructor(EntityTypes.SHEEP, SheepOverlay::new);
        PolymerEntityUtils.registerPolymerEntityConstructor(
                EntityTypes.CAT,
                cat -> new CollarOverlay(EntityTypes.CAT, CatDataAccessor.dyeDepot$getCollarData()));
        PolymerEntityUtils.registerPolymerEntityConstructor(
                EntityTypes.WOLF,
                wolf -> new CollarOverlay(EntityTypes.WOLF, WolfDataAccessor.dyeDepot$getCollarData()));
    }

    public static byte vanillaSheepData(byte value) {
        DyeColor color = DyeColor.byId(value & 31);
        boolean sheared = (value & 32) != 0;
        DyeColor vanilla = DDPolymerColors.vanillaColor(color);
        // The exact custom wool color is drawn by SheepWoolHolder. Marking the
        // vanilla proxy sheared prevents a nearest-color wool layer underneath.
        boolean hideVanillaWool = DDDyes.isModDye(color) && !sheared;
        return (byte) (vanilla.getId() | ((sheared || hideVanillaWool) ? 16 : 0));
    }

    public static int vanillaCollarData(int value) {
        return DDPolymerColors.vanillaColor(DyeColor.byId(value)).getId();
    }

    static ElementHolder sheepWoolHolder(PolymerEntity overlay) {
        return overlay instanceof SheepOverlay sheepOverlay ? sheepOverlay.woolAttachment.holder() : null;
    }

    public static void destroySheepWool(Sheep sheep) {
        EntityAttachment attachment = SHEEP_WOOL_ATTACHMENTS.remove(sheep);
        if (attachment != null) {
            attachment.destroy();
        }
    }

    private static final class SheepOverlay implements PolymerEntity {
        private final EntityAttachment woolAttachment;

        private SheepOverlay(Sheep sheep) {
            woolAttachment = EntityAttachment.ofTicking(new SheepWoolHolder(sheep), sheep);
            EntityAttachment previous = SHEEP_WOOL_ATTACHMENTS.put(sheep, woolAttachment);
            if (previous != null && previous != woolAttachment) {
                previous.destroy();
            }
        }

        @Override
        public EntityType<?> getPolymerEntityType(PacketContext context) {
            return EntityTypes.SHEEP;
        }

        @Override
        public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
            EntityDataAccessor<Byte> accessor = SheepDataAccessor.dyeDepot$getWoolData();
            for (int index = 0; index < data.size(); index++) {
                SynchedEntityData.DataValue<?> value = data.get(index);
                if (value.id() == accessor.id() && value.value() instanceof Byte raw) {
                    data.set(index, SynchedEntityData.DataValue.create(accessor, vanillaSheepData(raw)));
                }
            }
        }
    }

    private record CollarOverlay(EntityType<?> type, EntityDataAccessor<Integer> accessor) implements PolymerEntity {
        @Override
        public EntityType<?> getPolymerEntityType(PacketContext context) {
            return type;
        }

        @Override
        public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
            for (int index = 0; index < data.size(); index++) {
                SynchedEntityData.DataValue<?> value = data.get(index);
                if (value.id() == accessor.id() && value.value() instanceof Integer raw) {
                    data.set(index, SynchedEntityData.DataValue.create(accessor, vanillaCollarData(raw)));
                }
            }
        }
    }

    private static final class SheepWoolHolder extends ElementHolder {
        private final Sheep sheep;
        private final ItemDisplayElement coat = new ItemDisplayElement();
        private DyeColor displayedColor;
        private boolean displayedBaby;

        private SheepWoolHolder(Sheep sheep) {
            this.sheep = sheep;
            coat.setItemDisplayContext(ItemDisplayContext.NONE);
            coat.setTeleportDuration(1);
            coat.setViewRange(1.25f);
            addElement(coat);
            updateCoat(true);
        }

        @Override
        protected void onTick() {
            updateCoat(false);
            coat.setYaw(-sheep.getYRot());
        }

        private void updateCoat(boolean force) {
            DyeColor color = sheep.getColor();
            boolean baby = sheep.isBaby();
            boolean visible = DDDyes.isModDye(color) && !sheep.isSheared();
            if (force || color != displayedColor || baby != displayedBaby || visible != !coat.getItem().isEmpty()) {
                if (visible) {
                    ItemStack stack = new ItemStack(DDBlocks.WOOL.getOrThrow(color).asItem());
                    stack.set(DataComponents.ITEM_MODEL, DyeDepot.modLoc("polymer/" + color.getName() + "_sheep_wool"));
                    coat.setItem(stack);
                    float scale = baby ? 0.5f : 1.0f;
                    coat.setScale(new Vector3f(scale, scale, scale));
                    coat.setTranslation(new Vector3f(0.0f, baby ? 0.45f : 0.75f, 0.0f));
                } else {
                    coat.setItem(new ItemStack(Items.AIR));
                }
                displayedColor = color;
                displayedBaby = baby;
            }
        }

        @Override
        protected void onAttachmentRemoved(HolderAttachment attachment) {
            SHEEP_WOOL_ATTACHMENTS.remove(sheep, attachment);
            super.onAttachmentRemoved(attachment);
        }
    }
}
