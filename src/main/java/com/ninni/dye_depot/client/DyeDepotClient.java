package com.ninni.dye_depot.client;

import com.ninni.dye_depot.DyeDepot;
import com.ninni.dye_depot.client.particles.PoofParticleProvider;
import com.ninni.dye_depot.registry.DDParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public class DyeDepotClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        addResourcePacks();
        registerParticles();
    }

    public static void addResourcePacks() {
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(DyeDepot.MOD_ID).orElseThrow(() -> new IllegalStateException("Dye Depot's ModContainer couldn't be found!"));
        ResourceManagerHelper.registerBuiltinResourcePack(DyeDepot.modLoc("dye_override"), modContainer, ResourcePackActivationType.DEFAULT_ENABLED);
    }

    private static void registerParticles() {
        ParticleProviderRegistry.getInstance().register(DDParticles.DYE_POOF, PoofParticleProvider::new);
    }

}
