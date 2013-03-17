package com.funzio.pure2D.samples.activities.particles;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.funzio.pure2D.R;
import com.funzio.pure2D.Scene;
import com.funzio.pure2D.gl.gl10.textures.Texture;
import com.funzio.pure2D.gl.gl10.textures.TextureOptions;
import com.funzio.pure2D.samples.activities.StageActivity;

public class DynamicEmitterActivity extends StageActivity {
    public static final int NUM = 3;

    private Texture mSmokeTexture;
    private Texture mFireTexture;
    private ArrayList<DynamicEmitter> mEmitters = new ArrayList<DynamicEmitter>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // mScene.setColor(new GLColor(0, 0.7f, 0, 1));
        // need to get the GL reference first
        mScene.setListener(new Scene.Listener() {

            @Override
            public void onSurfaceCreated(final GL10 gl) {
                loadTextures();

                // create emitters
                DynamicEmitter emitter = new DynamicEmitter(mDisplaySize, mFireTexture, mSmokeTexture);
                mScene.addChild(emitter);
                mEmitters.add(emitter);

                emitter = new DynamicEmitter(mDisplaySize, mFireTexture, mSmokeTexture);
                emitter.setType(2);
                mScene.addChild(emitter);
                mEmitters.add(emitter);

                emitter = new DynamicEmitter(mDisplaySize, mFireTexture, mSmokeTexture);
                emitter.setType(3);
                mScene.addChild(emitter);
                mEmitters.add(emitter);
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see com.funzio.pure2D.samples.activities.StageActivity#onStop()
     */
    @Override
    protected void onStop() {
        super.onStop();
        Particle1.clearPool();
    }

    @Override
    protected int getLayout() {
        return R.layout.stage_simple;
    }

    private void loadTextures() {
        TextureOptions options = TextureOptions.getDefault();
        options.inMipmaps = 1;

        // smoke
        mSmokeTexture = mScene.getTextureManager().createDrawableTexture(R.drawable.smoke_small, options);

        // fire
        mFireTexture = mScene.getTextureManager().createDrawableTexture(R.drawable.fireball_small, options);
    }

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < NUM; i++) {
                DynamicEmitter emitter = mEmitters.get(i);
                emitter.setDestination(event.getX(), mDisplaySize.y - event.getY());
                emitter.lockDestination(true);
            }
        } else if (action == MotionEvent.ACTION_UP) {
            for (int i = 0; i < NUM; i++) {
                DynamicEmitter emitter = mEmitters.get(i);
                emitter.lockDestination(false);
            }
        }

        return true;
    }
}