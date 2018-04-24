package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.AverageSpike;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class AverageSpikeRenderer extends BYBAnalysisBaseRenderer {

    private static final String TAG = makeLogTag(AverageSpikeRenderer.class);

    @SuppressWarnings("WeakerAccess") AverageSpike[] averageSpikeAnalysis;

    public AverageSpikeRenderer(@NonNull BaseFragment fragment) {
        super(fragment);
    }

    @Override protected void drawingHandler(GL10 gl) {
        int margin = 20;

        initGL(gl);
        if (getAverageSpikeAnalysis()) {
            if (averageSpikeAnalysis != null) {
                float aw = width - margin * averageSpikeAnalysis.length;
                float ah = (height - margin * (averageSpikeAnalysis.length + 1)) / (float) averageSpikeAnalysis.length;

                BYBMesh rectMesh = new BYBMesh(BYBMesh.LINES);
                for (int i = 0; i < averageSpikeAnalysis.length; i++) {
                    BYBMesh line = new BYBMesh(BYBMesh.LINE_STRIP);
                    float xInc = aw / averageSpikeAnalysis[i].getAverageSpike().length;
                    float yOffSet = ((margin + ah) * (i + 1));

                    float[] lc = new float[4];
                    lc[3] = 1.0f;
                    switch (i) {
                        case 0:
                            lc[0] = 1.0f;
                            lc[1] = 0.0f;
                            lc[2] = 0.0f;
                            break;
                        case 1:
                            lc[0] = 1.0f;
                            lc[1] = 1.0f;
                            lc[2] = 0.0f;
                            break;
                        case 2:
                            lc[0] = 0.0f;
                            lc[1] = 1.0f;
                            lc[2] = 1.0f;
                            break;
                    }
                    if (averageSpikeAnalysis[i].getNormTopSTDLine().length > 0) {
                        float v0x = (margin * 2);
                        float v0y = yOffSet - averageSpikeAnalysis[i].getNormTopSTDLine()[0] * ah;
                        float v1y = yOffSet - averageSpikeAnalysis[i].getNormBottomSTDLine()[0] * ah;
                        BYBMesh mesh = new BYBMesh(BYBMesh.TRIANGLES);
                        for (int j = 1; j < averageSpikeAnalysis[i].getNormTopSTDLine().length; j++) {

                            float x = xInc * j + (margin * 2);
                            float yTop = yOffSet - averageSpikeAnalysis[i].getNormTopSTDLine()[j] * ah;
                            float yBot = yOffSet - averageSpikeAnalysis[i].getNormBottomSTDLine()[j] * ah;

                            mesh.addQuadSmooth(v0x, v0y, v0x, v1y, x, yTop, x, yBot, lc);

                            v0x = x;
                            v0y = yTop;
                            v1y = yBot;
                        }
                        mesh.draw(gl);
                    }

                    for (int j = 0; j < averageSpikeAnalysis[i].getAverageSpike().length; j++) {
                        line.addVertex(xInc * j + (margin * 2),
                            yOffSet - averageSpikeAnalysis[i].getNormAverageSpike()[j] * ah);
                    }

                    gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
                    line.draw(gl);

                    float[] c = { 0.2f, 0.2f, 0.2f, 1.0f };
                    rectMesh.addRectangle(margin * 2, margin + (ah + margin) * i, aw, ah, c);
                }

                rectMesh.draw(gl);
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
