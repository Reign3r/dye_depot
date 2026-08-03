package com.ninni.dye_depot.polymer;

import com.ninni.dye_depot.block.DyeBasketBlock;
import eu.pb4.polymer.core.api.other.PolymerParticleType;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.item.DyeColor;

final class DDPolymerParticles {
    static final PolymerParticleType<BlockParticleOption> DYE_POOF = new PolymerParticleType<>() {
        @Override
        public ParticleOptions getPolymerParticleReplacement(BlockParticleOption value, PacketContext context) {
            int color = 0xffffff;
            if (value.getState().getBlock() instanceof DyeBasketBlock basket) {
                DyeColor dye = basket.getDyeColor();
                color = switch (dye) {
                    case BLUE -> 0x345eb6;
                    case GREEN -> 0x5b8221;
                    case MAGENTA -> 0xcb69c5;
                    case CYAN -> 0x29a9b0;
                    case RED -> 0xb53c39;
                    case ORANGE -> 0xd7710a;
                    default -> dye.getTextColor();
                };
            }
            return new DustParticleOptions(color, 1.0f);
        }
    };

    private DDPolymerParticles() {
    }
}
