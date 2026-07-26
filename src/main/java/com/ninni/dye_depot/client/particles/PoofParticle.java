package com.ninni.dye_depot.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;

public class PoofParticle extends SingleQuadParticle {
    private final float rotSpeed;
    private final SpriteSet sprites;

    PoofParticle(ClientLevel clientLevel, double d, double e, double f, float g, float h, float i, SpriteSet spriteSet) {
        super(clientLevel, d, e, f, spriteSet.first());
        this.sprites = spriteSet;
        this.rCol = g;
        this.gCol = h;
        this.bCol = i;
        this.quadSize *= 0.67499995f;
        int k = (int)(32.0 / (this.random.nextFloat() * 0.8 + 0.2));
        this.lifetime = (int)Math.max((float)k * 0.9f, 1.0f);
        this.setSpriteFromAge(spriteSet);
        this.rotSpeed = (this.random.nextFloat() - 0.5f) * 0.1f;
        this.roll = this.random.nextFloat() * ((float)Math.PI * 2);
    }

    @Override
    public Layer getLayer() {
        return Layer.OPAQUE;
    }

    @Override
    public float getQuadSize(float f) {
        return this.quadSize * Mth.clamp(((float)this.age + f) / (float)this.lifetime * 32.0f, 0.0f, 1.0f);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
            this.oRoll = this.roll;
            this.roll += (float)Math.PI * this.rotSpeed * 2.0f;
            if (this.onGround) {
                this.roll = 0.0f;
                this.oRoll = 0.0f;
            }
            this.move(this.xd, this.yd, this.zd);
            this.yd -= 0.003f;
            this.yd = Math.max(this.yd, -0.14f);
        }
    }
}
