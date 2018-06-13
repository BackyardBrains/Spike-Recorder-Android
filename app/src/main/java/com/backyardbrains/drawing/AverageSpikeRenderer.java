package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.AverageSpike;
import com.backyardbrains.utils.GlUtils;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class AverageSpikeRenderer extends BYBAnalysisBaseRenderer {

    private static final String TAG = makeLogTag(AverageSpikeRenderer.class);

    @SuppressWarnings("WeakerAccess") AverageSpike[] averageSpikeAnalysis;

    public AverageSpikeRenderer(@NonNull BaseFragment fragment) {
        super(fragment);
    }

    @Override protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight) {
        if (getAverageSpikeAnalysis()) {
            if (averageSpikeAnalysis != null) {
                int len = averageSpikeAnalysis.length;
                if (len > 0) {
                    // calculate width and height of average spike graph per spike train
                    float aw = surfaceWidth - MARGIN * len;
                    float ah = (surfaceHeight - MARGIN * (len + 1)) / (float) len;

                    for (int i = 0; i < len; i++) {
                        float xInc = aw / averageSpikeAnalysis[i].getAverageSpike().length;
                        float yOffSet = (MARGIN + ah) * (i + 1);

                        if (averageSpikeAnalysis[i].getNormTopSTDLine().length > 0) {
                            float v0x = (MARGIN * 2);
                            float v0y = surfaceHeight - (yOffSet - averageSpikeAnalysis[i].getNormTopSTDLine()[0] * ah);
                            float v1y =
                                surfaceHeight - (yOffSet - averageSpikeAnalysis[i].getNormBottomSTDLine()[0] * ah);
                            BYBMesh mesh = new BYBMesh(BYBMesh.TRIANGLES);
                            for (int j = 1; j < averageSpikeAnalysis[i].getNormTopSTDLine().length; j++) {
                                float x = xInc * j + (MARGIN * 2);
                                float yTop =
                                    surfaceHeight - (yOffSet - averageSpikeAnalysis[i].getNormTopSTDLine()[j] * ah);
                                float yBot =
                                    surfaceHeight - (yOffSet - averageSpikeAnalysis[i].getNormBottomSTDLine()[j] * ah);

                                mesh.addQuadSmooth(v0x, v0y, v0x, v1y, x, yTop, x, yBot,
                                    GlUtils.SPIKE_TRAIN_COLORS[i]);

                                v0x = x;
                                v0y = yTop;
                                v1y = yBot;
                            }
                            mesh.draw(gl);
                        }

                        // draw average line
                        BYBMesh line = new BYBMesh(BYBMesh.LINE_STRIP);
                        for (int j = 0; j < averageSpikeAnalysis[i].getAverageSpike().length; j++) {
                            line.addVertex(xInc * j + (MARGIN * 2),
                                surfaceHeight - (yOffSet - averageSpikeAnalysis[i].getNormAverageSpike()[j] * ah));
                        }
                        gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
                        gl.glLineWidth(3f);
                        line.draw(gl);
                    }
                }
            }
        }
    }

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    private boolean getAverageSpikeAnalysis() {
        if (averageSpikeAnalysis != null && averageSpikeAnalysis.length > 0) return true;

        if (getAnalysisManager() != null) {
            averageSpikeAnalysis = getAnalysisManager().getAverageSpike();
            if (averageSpikeAnalysis != null) {
                LOGD(TAG, "AVERAGE SPIKE ANALYSIS RETURNED: " + averageSpikeAnalysis.length);
            }
            return averageSpikeAnalysis != null;
        }

        return false;
    }
}
