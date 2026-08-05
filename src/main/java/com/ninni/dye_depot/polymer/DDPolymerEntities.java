package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.mixin.CatDataAccessor;
import com.ninni.dye_depot.mixin.EntityDataAccessors;
import com.ninni.dye_depot.mixin.SheepDataAccessor;
import com.ninni.dye_depot.mixin.WolfDataAccessor;
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
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import org.joml.Quaternionf;
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
                cat -> new CatCollarOverlay((Cat) cat));
        PolymerEntityUtils.registerPolymerEntityConstructor(
                EntityTypes.WOLF,
                wolf -> new WolfCollarOverlay((Wolf) wolf));
    }

    public static byte vanillaSheepData(byte value) {
        DyeColor color = DyeColor.byId(value & 31);
        return vanillaSheepData(value, DDDyes.isModDye(color));
    }

    static byte vanillaSheepData(byte value, boolean useVirtualWool) {
        DyeColor color = DyeColor.byId(value & 31);
        boolean sheared = (value & 32) != 0;
        // 26.2 renders a colored adult undercoat even while a sheep is sheared.
        // A white, sheared proxy suppresses both native layers; the virtual
        // model then supplies the exact outer wool and undercoat as applicable.
        DyeColor vanilla = useVirtualWool ? DyeColor.WHITE : DDPolymerColors.vanillaColor(color);
        return (byte) (vanilla.getId() | ((sheared || useVirtualWool) ? 16 : 0));
    }

    public static int vanillaCollarData(int value) {
        // The native collar masks are transparent in the generated Polymer
        // pack. A fixed in-range value keeps the vanilla client decoder safe;
        // the exact 32-color collar is baked into the synchronized variant.
        return DyeColor.RED.getId();
    }

    static Identifier collarVariantId(String entityType, ResourceKey<?> originalVariant, DyeColor color) {
        Identifier original = originalVariant.identifier();
        return DyeDepot.modLoc("polymer/collar/" + entityType + "/"
                + original.getNamespace() + "/" + original.getPath() + "/" + color.getName());
    }

    private static boolean isJebSheep(Sheep sheep) {
        Component name = sheep.getCustomName();
        return name != null && isJebName(name);
    }

    private static boolean isJebName(Component name) {
        return "jeb_".equals(name.getString());
    }

    private static Component clientSafeJebName(Component name) {
        // U+200C is a zero-advance glyph in vanilla's built-in space font.
        // Appending it keeps the visible name identical while preventing the
        // vanilla client's 16-color magic-name undercoat from rendering.
        Style sentinelStyle = Style.EMPTY
                .withFont(FontDescription.DEFAULT)
                .withBold(false)
                .withItalic(false)
                .withUnderlined(false)
                .withStrikethrough(false)
                .withObfuscated(false);
        return name.copy().append(Component.literal("\u200C").setStyle(sentinelStyle));
    }

    static ElementHolder sheepWoolHolder(PolymerEntity overlay) {
        if (overlay instanceof SheepOverlay sheepOverlay) {
            sheepOverlay.syncWoolAttachment();
            return sheepOverlay.woolHolder();
        }
        return null;
    }

    public static void destroySheepWool(Sheep sheep) {
        EntityAttachment attachment = SHEEP_WOOL_ATTACHMENTS.remove(sheep);
        if (attachment != null) {
            attachment.destroy();
        }
    }

    private static final class SheepOverlay implements PolymerEntity {
        private final Sheep sheep;
        private EntityAttachment woolAttachment;

        private SheepOverlay(Sheep sheep) {
            this.sheep = sheep;
        }

        @Override
        public EntityType<?> getPolymerEntityType(PacketContext context) {
            return EntityTypes.SHEEP;
        }

        @Override
        public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
            syncWoolAttachment();
            EntityDataAccessor<Byte> woolAccessor = SheepDataAccessor.dyeDepot$getWoolData();
            EntityDataAccessor<Optional<Component>> nameAccessor = EntityDataAccessors.dyeDepot$getCustomNameData();
            boolean woolUpdated = false;
            boolean nameUpdated = false;
            for (int index = 0; index < data.size(); index++) {
                SynchedEntityData.DataValue<?> value = data.get(index);
                if (value.id() == woolAccessor.id() && value.value() instanceof Byte raw) {
                    data.set(index, SynchedEntityData.DataValue.create(
                            woolAccessor,
                            vanillaSheepData(raw, usesVirtualWool())
                    ));
                    woolUpdated = true;
                } else if (value.id() == nameAccessor.id()) {
                    nameUpdated = true;
                    if (value.value() instanceof Optional<?> optional
                            && optional.orElse(null) instanceof Component name
                            && isJebName(name)) {
                        data.set(index, SynchedEntityData.DataValue.create(
                                nameAccessor,
                                Optional.of(clientSafeJebName(name))
                        ));
                    }
                }
            }
            // Renaming to or from jeb_ changes whether the client proxy must be
            // hidden, but the vanilla name update does not include wool data.
            if (nameUpdated && !woolUpdated) {
                byte raw = sheep.getEntityData().get(woolAccessor);
                data.add(SynchedEntityData.DataValue.create(
                        woolAccessor,
                        vanillaSheepData(raw, usesVirtualWool())
                ));
            }
        }

        private synchronized void syncWoolAttachment() {
            if (hasVisibleCustomWool()) {
                if (woolAttachment == null) {
                    woolAttachment = EntityAttachment.ofTicking(new SheepWoolHolder(this, sheep), sheep);
                    EntityAttachment previous = SHEEP_WOOL_ATTACHMENTS.put(sheep, woolAttachment);
                    if (previous != null && previous != woolAttachment) {
                        previous.destroy();
                    }
                }
            } else {
                removeWoolAttachment();
            }
        }

        private boolean hasVisibleCustomWool() {
            return usesVirtualWool()
                    && (!sheep.isSheared() || !sheep.isBaby())
                    && !sheep.isInvisible()
                    && sheep.isAlive()
                    && !sheep.isRemoved();
        }

        private boolean usesVirtualWool() {
            return DDDyes.isModDye(sheep.getColor()) || isJebSheep(sheep);
        }

        private synchronized ElementHolder woolHolder() {
            return woolAttachment == null ? null : woolAttachment.holder();
        }

        private synchronized void removeWoolAttachment() {
            EntityAttachment attachment = woolAttachment;
            woolAttachment = null;
            if (attachment != null) {
                SHEEP_WOOL_ATTACHMENTS.remove(sheep, attachment);
                attachment.destroy();
            }
        }

        private synchronized void attachmentRemoved(HolderAttachment attachment) {
            if (woolAttachment == attachment) {
                woolAttachment = null;
            }
            SHEEP_WOOL_ATTACHMENTS.remove(sheep, attachment);
        }
    }

    private abstract static class CollarOverlay<V> implements PolymerEntity {
        private final TamableAnimal animal;
        private final EntityType<?> type;
        private final EntityDataAccessor<Integer> collarAccessor;
        private final EntityDataAccessor<Holder<V>> variantAccessor;

        private CollarOverlay(
                TamableAnimal animal,
                EntityType<?> type,
                EntityDataAccessor<Integer> collarAccessor,
                EntityDataAccessor<Holder<V>> variantAccessor
        ) {
            this.animal = animal;
            this.type = type;
            this.collarAccessor = collarAccessor;
            this.variantAccessor = variantAccessor;
        }

        protected abstract ResourceKey<Registry<V>> registryKey();

        protected abstract String variantType();

        @Override
        public EntityType<?> getPolymerEntityType(PacketContext context) {
            return type;
        }

        @Override
        public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
            int rawCollar = animal.getEntityData().get(collarAccessor);
            Holder<V> originalVariant = animal.getEntityData().get(variantAccessor);
            Holder<V> clientVariant = clientVariant(originalVariant, DyeColor.byId(rawCollar));
            boolean variantUpdated = false;
            for (int index = 0; index < data.size(); index++) {
                SynchedEntityData.DataValue<?> value = data.get(index);
                if (value.id() == collarAccessor.id() && value.value() instanceof Integer) {
                    data.set(index, SynchedEntityData.DataValue.create(
                            collarAccessor,
                            vanillaCollarData(rawCollar)
                    ));
                } else if (value.id() == variantAccessor.id()) {
                    data.set(index, SynchedEntityData.DataValue.create(variantAccessor, clientVariant));
                    variantUpdated = true;
                }
            }
            // Collar changes and tame-state changes do not dirty the variant
            // accessor. Send it alongside every metadata update so the body
            // texture always follows the authoritative server collar/state.
            if (!variantUpdated) {
                data.add(SynchedEntityData.DataValue.create(variantAccessor, clientVariant));
            }
        }

        private Holder<V> clientVariant(Holder<V> original, DyeColor collarColor) {
            if (!animal.isTame()) {
                return original;
            }
            Optional<ResourceKey<V>> originalKey = original.unwrapKey();
            if (originalKey.isEmpty()) {
                return original;
            }
            if (!DDPolymerCollarPack.supportsVariant(variantType(), originalKey.get().identifier())) {
                return original;
            }
            Identifier syntheticId = collarVariantId(variantType(), originalKey.get(), collarColor);
            Registry<V> registry = animal.registryAccess().lookupOrThrow(registryKey());
            return registry.get(ResourceKey.create(registryKey(), syntheticId))
                    .<Holder<V>>map(holder -> holder)
                    .orElse(original);
        }
    }

    private static final class CatCollarOverlay extends CollarOverlay<CatVariant> {
        private CatCollarOverlay(Cat cat) {
            super(
                    cat,
                    EntityTypes.CAT,
                    CatDataAccessor.dyeDepot$getCollarData(),
                    CatDataAccessor.dyeDepot$getVariantData()
            );
        }

        @Override
        protected ResourceKey<Registry<CatVariant>> registryKey() {
            return Registries.CAT_VARIANT;
        }

        @Override
        protected String variantType() {
            return "cat";
        }
    }

    private static final class WolfCollarOverlay extends CollarOverlay<WolfVariant> {
        private WolfCollarOverlay(Wolf wolf) {
            super(
                    wolf,
                    EntityTypes.WOLF,
                    WolfDataAccessor.dyeDepot$getCollarData(),
                    WolfDataAccessor.dyeDepot$getVariantData()
            );
        }

        @Override
        protected ResourceKey<Registry<WolfVariant>> registryKey() {
            return Registries.WOLF_VARIANT;
        }

        @Override
        protected String variantType() {
            return "wolf";
        }
    }

    private static final class SheepWoolHolder extends ElementHolder {
        private static final float MODEL_BASE_Y = 1.501f;
        private static final float PIXEL = 1.0f / 16.0f;

        private final Sheep sheep;
        private final ItemDisplayElement head = createPart();
        private final ItemDisplayElement body = createPart();
        private final ItemDisplayElement rightHindLeg = createPart();
        private final ItemDisplayElement leftHindLeg = createPart();
        private final ItemDisplayElement rightFrontLeg = createPart();
        private final ItemDisplayElement leftFrontLeg = createPart();
        private final List<ItemDisplayElement> parts = List.of(
                head,
                body,
                rightHindLeg,
                leftHindLeg,
                rightFrontLeg,
                leftFrontLeg
        );
        private int displayedRgb = -1;
        private boolean displayedBaby;
        private boolean displayedSheared;
        private boolean outlineInitialized;
        private boolean displayedGlowing;
        private int displayedGlowColor = -1;
        private final SheepOverlay owner;

        private SheepWoolHolder(SheepOverlay owner, Sheep sheep) {
            this.owner = owner;
            this.sheep = sheep;
            parts.forEach(this::addElement);
            updateCoat(true);
            updateOutline();
            updatePose();
        }

        @Override
        protected void onTick() {
            if (!owner.hasVisibleCustomWool()) {
                owner.removeWoolAttachment();
                return;
            }
            updateCoat(false);
            updateOutline();
            updatePose();
        }

        private void updateOutline() {
            boolean glowing = sheep.isCurrentlyGlowing();
            int glowColor = glowing ? sheep.getTeamColor() : -1;
            if (!outlineInitialized || glowing != displayedGlowing || glowColor != displayedGlowColor) {
                for (ItemDisplayElement part : parts) {
                    part.setGlowing(glowing);
                    part.setGlowColorOverride(glowColor);
                }
                outlineInitialized = true;
                displayedGlowing = glowing;
                displayedGlowColor = glowColor;
            }
        }

        private void updateCoat(boolean force) {
            int rgb = sheepWoolRgb(sheep);
            boolean baby = sheep.isBaby();
            boolean sheared = sheep.isSheared();
            if (force || rgb != displayedRgb || baby != displayedBaby || sheared != displayedSheared) {
                setPartItem(head, rgb, baby, sheared, "head");
                setPartItem(body, rgb, baby, sheared, "body");
                setPartItem(rightHindLeg, rgb, baby, sheared, baby ? "right_hind_leg" : "right_leg");
                setPartItem(leftHindLeg, rgb, baby, sheared, baby ? "left_hind_leg" : "leg");
                setPartItem(rightFrontLeg, rgb, baby, sheared, baby ? "right_front_leg" : "right_leg");
                setPartItem(leftFrontLeg, rgb, baby, sheared, baby ? "left_front_leg" : "leg");
                displayedRgb = rgb;
                displayedBaby = baby;
                displayedSheared = sheared;
            }
        }

        private void updatePose() {
            boolean baby = sheep.isBaby();
            float bodyYaw = sheep.yBodyRot;
            float headPitch = sheep.getHeadEatAngleScale(1.0f);
            float headYaw = Mth.wrapDegrees(sheep.yHeadRot - sheep.yBodyRot) * Mth.DEG_TO_RAD;
            float eatOffset = sheep.getHeadEatPositionScale(1.0f) * 9.0f * sheep.getAgeScale();
            // Polymer constructs the overlay from Entity's base constructor,
            // before LivingEntity initializes this final field. The first
            // attachment tick sees the real state; use an idle pose meanwhile.
            float animationPosition = sheep.walkAnimation == null
                    ? 0.0f
                    : sheep.walkAnimation.position(1.0f);
            float animationSpeed = sheep.walkAnimation == null
                    ? 0.0f
                    : sheep.walkAnimation.speed(1.0f);
            float rightHindAngle = Mth.cos(animationPosition * 0.6662f) * 1.4f * animationSpeed;
            float leftHindAngle = Mth.cos(animationPosition * 0.6662f + Mth.PI) * 1.4f * animationSpeed;

            if (baby) {
                setPose(head, bodyYaw, pivot(0.0f, 15.5f + eatOffset, -2.5f), rotation(headPitch, -headYaw));
                setPose(body, bodyYaw, pivot(0.0f, 17.0f, 0.5f), rotation(0.0f, 0.0f));
                setPose(rightHindLeg, bodyYaw, pivot(-2.0f, 19.0f, 3.0f), rotation(rightHindAngle, 0.0f));
                setPose(leftHindLeg, bodyYaw, pivot(2.0f, 19.0f, 3.0f), rotation(leftHindAngle, 0.0f));
                setPose(rightFrontLeg, bodyYaw, pivot(-2.0f, 19.0f, -2.0f), rotation(leftHindAngle, 0.0f));
                setPose(leftFrontLeg, bodyYaw, pivot(2.0f, 19.0f, -2.0f), rotation(rightHindAngle, 0.0f));
            } else {
                setPose(head, bodyYaw, pivot(0.0f, 6.0f + eatOffset, -8.0f), rotation(headPitch, -headYaw));
                setPose(body, bodyYaw, pivot(0.0f, 5.0f, 2.0f), rotation(Mth.HALF_PI, 0.0f));
                setPose(rightHindLeg, bodyYaw, pivot(-3.0f, 12.0f, 7.0f), rotation(rightHindAngle, 0.0f));
                setPose(leftHindLeg, bodyYaw, pivot(3.0f, 12.0f, 7.0f), rotation(leftHindAngle, 0.0f));
                setPose(rightFrontLeg, bodyYaw, pivot(-3.0f, 12.0f, -5.0f), rotation(leftHindAngle, 0.0f));
                setPose(leftFrontLeg, bodyYaw, pivot(3.0f, 12.0f, -5.0f), rotation(rightHindAngle, 0.0f));
            }
        }

        private static ItemDisplayElement createPart() {
            ItemDisplayElement part = new ItemDisplayElement();
            part.setItemDisplayContext(ItemDisplayContext.NONE);
            part.setInterpolationDuration(1);
            part.setTeleportDuration(1);
            part.setViewRange(1.25f);
            return part;
        }

        private static void setPartItem(
                ItemDisplayElement part,
                int rgb,
                boolean baby,
                boolean sheared,
                String partName
        ) {
            ItemStack stack = new ItemStack(Items.PAPER);
            String modelPrefix = baby ? "baby_" : sheared ? "adult_undercoat_" : "adult_";
            stack.set(
                    DataComponents.ITEM_MODEL,
                    DyeDepot.modLoc("polymer/sheep/" + modelPrefix + partName)
            );
            stack.set(
                    DataComponents.DYED_COLOR,
                    new DyedItemColor(rgb)
            );
            part.setItem(stack);
        }

        private static int sheepWoolRgb(Sheep sheep) {
            if (!isJebSheep(sheep)) {
                return nativeSheepArgb(sheep.getColor()) & 0xFFFFFF;
            }
            DyeColor[] colors = DyeColor.values();
            int phase = sheep.tickCount / 25;
            int first = Math.floorMod(phase, colors.length);
            int second = (first + 1) % colors.length;
            float progress = Math.floorMod(sheep.tickCount, 25) / 25.0f;
            return ARGB.srgbLerp(
                    progress,
                    nativeSheepArgb(colors[first]),
                    nativeSheepArgb(colors[second])
            ) & 0xFFFFFF;
        }

        private static int nativeSheepArgb(DyeColor color) {
            if (color == DyeColor.WHITE) {
                return 0xFFE6E6E6;
            }
            int rgb = color.getTextureDiffuseColor();
            int red = (int) (((rgb >> 16) & 0xFF) * 0.75f);
            int green = (int) (((rgb >> 8) & 0xFF) * 0.75f);
            int blue = (int) ((rgb & 0xFF) * 0.75f);
            return ARGB.color(255, red, green, blue);
        }

        /**
         * Converts the native sheep-model pivot (pixels, Y down) into an item
         * display transform (blocks, Y up). The item renderer supplies the same
         * 180-degree Y turn used by LivingEntityRenderer, so Z is mirrored here.
         */
        private static Vector3f pivot(float x, float y, float z) {
            return new Vector3f(x * PIXEL, MODEL_BASE_Y - y * PIXEL, -z * PIXEL);
        }

        private static Quaternionf rotation(float x, float y) {
            return new Quaternionf().rotationZYX(0.0f, y, x);
        }

        private static void setPose(
                ItemDisplayElement part,
                float bodyYaw,
                Vector3f translation,
                Quaternionf rotation
        ) {
            part.setYaw(bodyYaw);
            part.setTranslation(translation);
            part.setLeftRotation(rotation);
            part.startInterpolationIfDirty();
        }

        @Override
        protected void onAttachmentRemoved(HolderAttachment attachment) {
            owner.attachmentRemoved(attachment);
            super.onAttachmentRemoved(attachment);
        }
    }
}
