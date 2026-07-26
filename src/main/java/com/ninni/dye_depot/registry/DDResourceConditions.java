package com.ninni.dye_depot.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;

/**
 * Keeps Supplementaries-owned load conditions decodable when the optional mod
 * is absent.
 *
 * <p>Fabric 26.2 decodes an entire condition list before testing its
 * {@code all_mods_loaded} entry. The fallback is therefore registered only
 * without Supplementaries and always evaluates false. When Supplementaries is
 * installed, its real condition remains authoritative and retains config-flag
 * behavior.</p>
 */
public final class DDResourceConditions {
    private static final String SUPPLEMENTARIES = "supplementaries";
    private static final Identifier SUPPLEMENTARIES_FLAG =
            Identifier.fromNamespaceAndPath(SUPPLEMENTARIES, "flag");
    private static final ResourceConditionType<MissingSupplementariesFlag> FALLBACK_TYPE =
            ResourceConditionType.create(SUPPLEMENTARIES_FLAG, MissingSupplementariesFlag.CODEC);

    private DDResourceConditions() {
    }

    public static void registerFallbacks() {
        if (!FabricLoader.getInstance().isModLoaded(SUPPLEMENTARIES)
                && ResourceConditions.getConditionType(SUPPLEMENTARIES_FLAG) == null) {
            ResourceConditions.register(FALLBACK_TYPE);
        }
    }

    private record MissingSupplementariesFlag(String flag) implements ResourceCondition {
        private static final MapCodec<MissingSupplementariesFlag> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.fieldOf("flag").forGetter(MissingSupplementariesFlag::flag)
                ).apply(instance, MissingSupplementariesFlag::new));

        @Override
        public ResourceConditionType<?> getType() {
            return FALLBACK_TYPE;
        }

        @Override
        public boolean test(RegistryOps.RegistryInfoLookup registryInfoLookup) {
            return false;
        }
    }
}
