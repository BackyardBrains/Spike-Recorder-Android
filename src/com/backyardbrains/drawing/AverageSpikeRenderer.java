package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.analysis.BYBAverageSpike;
import javax.microedition.khronos.opengles.GL10;

public class AverageSpikeRenderer extends BYBAnalysisBaseRenderer {

    public AverageSpikeRenderer(@NonNull BaseFragment fragment) {
        super(fragment);
    }

    @Override protected void drawingHandler(GL10 gl) {
        int margin = 20;

        initGL(gl);
        if (getAnalysisManager() != null) {
            BYBAverageSpike[] avg = getAnalysisManager().getAverageSpike();
            if (avg != null) {
                float aw = width - margin * avg.length;
                float ah = (height - margin * (avg.length + 1)) / (float) avg.length;

                BYBMesh rectsMesh = new BYBMesh(BYBMesh.LINES);
                for (int i = 0; i < avg.length; i++) {

                    BYBMesh line = new BYBMesh(BYBMesh.LINE_STRIP);
                    float xInc = aw / avg[i].getAverageSpike().length;
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
                    if (avg[i].getNormTopSTDLine().length > 0) {
                        float v0x = (margin * 2);
                        float v0y = yOffSet - avg[i].getNormTopSTDLine()[0] * ah;
                        float v1y = yOffSet - avg[i].getNormBottomSTDLine()[0] * ah;
                        BYBMesh mesh = new BYBMesh(BYBMesh.TRIANGLES);
                        for (int j = 1; j < avg[i].getNormTopSTDLine().length; j++) {

                            float x = xInc * j + (margin * 2);
                            float yTop = yOffSet - avg[i].getNormTopSTDLine()[j] * ah;
                            float yBot = yOffSet - avg[i].getNormBottomSTDLine()[j] * ah;

                            mesh.addQuadSmooth(v0x, v0y, v0x, v1y, x, yTop, x, yBot, lc);

                            v0x = x;
                            v0y = yTop;
                            v1y = yBot;
                        }
                        mesh.draw(gl);
                    }

                    for (int j = 0; j < avg[i].getAverageSpike().length; j++) {
                        line.addVertex(xInc * j + (margin * 2), yOffSet - avg[i].getNormAverageSpike()[j] * ah);
                    }

                    gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
                    line.draw(gl);

                    float[] c = { 0.2f, 0.2f, 0.2f, 1.0f };
                    rectsMesh.addRectangle(margin * 2, margin + (ah + margin) * i, aw, ah, c);
                }

                rectsMesh.draw(gl);
            }
        }
    }
}
