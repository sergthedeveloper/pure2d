/**
 * 
 */
package com.funzio.crimecity.particles.explosions;

import com.funzio.crimecity.particles.ParticleAdapter;
import com.funzio.pure2D.gl.gl10.textures.Texture;

/**
 * @author long
 */
public class ExplosionFireParticle extends ExplosionParticle {

    public ExplosionFireParticle(final Texture texture) {
        super(texture);

        setBlendFunc(ParticleAdapter.BF_ADD);
    }

    /*
     * (non-Javadoc)
     * @see com.funzio.crimecity.particles.ExplosionParticle#update(int)
     */
    @Override
    public boolean update(final int deltaTime) {
        if (mLifeStage == 0) {
            // nothing
        } else if (mLifeStage == 1) {
            mDeltaScale.x *= .65;
            mDeltaScale.y *= .65;
        } else if (mLifeStage == 2) {
            if (mScale.x <= 0) {
                mVisible = false;
            } else {
                mDeltaScale.x -= .07;
                mDeltaScale.y -= .07;
            }
        }

        return super.update(deltaTime);
    }
}