/**
 * 
 */
package com.funzio.pure2D.particles.nova.vo;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.funzio.pure2D.Manipulatable;
import com.funzio.pure2D.animators.Animator;
import com.funzio.pure2D.animators.SkewAnimator;
import com.funzio.pure2D.particles.nova.NovaConfig;

/**
 * @author long
 */
public class SkewAnimatorVO extends TweenAnimatorVO {

    public ArrayList<Float> x_from;
    public ArrayList<Float> x_to;
    public ArrayList<Float> y_from;
    public ArrayList<Float> y_to;

    public SkewAnimatorVO(final JSONObject json) throws JSONException {
        super(json);

        x_from = NovaVO.getListFloat(json, "x_from");
        x_to = NovaVO.getListFloat(json, "x_to");
        y_from = NovaVO.getListFloat(json, "y_from");
        y_to = NovaVO.getListFloat(json, "y_to");
    }

    @Override
    public Animator createAnimator(final int emitIndex, final Manipulatable target, final Animator... animators) {
        return init(emitIndex, target, new SkewAnimator(NovaConfig.getInterpolator(interpolation)));
    }

    @Override
    public void resetAnimator(final int emitIndex, final Manipulatable target, final Animator animator) {
        super.resetAnimator(emitIndex, target, animator);

        final SkewAnimator skew = (SkewAnimator) animator;
        skew.setValues(NovaConfig.getFloat(x_from, emitIndex, 0), //
                NovaConfig.getFloat(y_from, emitIndex, 0), //
                NovaConfig.getFloat(x_to, emitIndex, 0), //
                NovaConfig.getFloat(y_to, emitIndex, 0));

        skew.setDuration(NovaConfig.getInt(duration, emitIndex, 0));
    }
}