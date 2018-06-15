package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import com.android.texample.GLText;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.AverageSpike;
import com.backyardbrains.utils.GlUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class AverageSpikeRenderer extends BYBAnalysisBaseRenderer {

    private static final String TAG = makeLogTag(AverageSpikeRenderer.class);

    private static final String TIME_MS = "Time (ms)";
    private static final float H_AXIS_WIDTH = 2f;
    private static final int H_AXIS_VERTICES_COUNT = 20;
    private static final short[] H_AXiS_INDICES = { 0, 1, 0, 2, 2, 3, 2, 4, 4, 5, 4, 6, 6, 7, 6, 8, 8, 9 };
    private static final float H_AXIS_VALUES_MARGIN = 20f;
    private static final float H_AXiS_SCALE_HEIGHT = 10f;
    private static final int[] H_AXIS_VALUES = new int[5];

    static {
        H_AXIS_VALUES[0] = -2;
        H_AXIS_VALUES[1] = -1;
        H_AXIS_VALUES[2] = 0;
        H_AXIS_VALUES[3] = 1;
        H_AXIS_VALUES[4] = 2;
    }

    private final Context context;

    private GLText glText;
    private GlAverageSpikeGraph glAverageSpikeGraph;
    private final FloatBuffer hAxisVFB;
    private final ShortBuffer hAxisISB;

    @SuppressWarnings("WeakerAccess") AverageSpike[] averageSpikeAnalysis;

    public AverageSpikeRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();

        ByteBuffer hAxisVBB = ByteBuffer.allocateDirect(H_AXIS_VERTICES_COUNT * 4);
        hAxisVBB.order(ByteOrder.nativeOrder());
        hAxisVFB = hAxisVBB.asFloatBuffer();

        ByteBuffer hAxisIBB = ByteBuffer.allocateDirect(H_AXiS_INDICES.length * 2);
        hAxisIBB.order(ByteOrder.nativeOrder());
        hAxisISB = hAxisIBB.asShortBuffer();
        hAxisISB.put(H_AXiS_INDICES);
        hAxisISB.position(0);
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        glAverageSpikeGraph = new GlAverageSpikeGraph(context, gl);
        glText = new GLText(gl, context.getAssets());
        glText.load("dos-437.ttf", 32, 0, 0);
    }

    @Override protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight) {
        if (getAverageSpikeAnalysis()) {
            if (averageSpikeAnalysis != null) {
                int len = averageSpikeAnalysis.length;
                if (len > 0) {
                    float textH = glText.getHeight();
                    float hOffset = MARGIN + glAverageSpikeGraph.getHOffset();
                    float vOffset = 2 * (MARGIN + textH + H_AXIS_VALUES_MARGIN) + H_AXiS_SCALE_HEIGHT;
                    float graphW = surfaceWidth - 2 * MARGIN;
                    float graphH = (surfaceHeight - MARGIN * (len + 1) - vOffset) / (float) len;

                    // draw average spike graphs
                    for (int i = 0; i < len; i++) {
                        float y = surfaceHeight - (MARGIN + graphH) * (i + 1);
                        glAverageSpikeGraph.draw(gl, MARGIN, y, graphW, graphH, averageSpikeAnalysis[i],
                            GlUtils.SPIKE_TRAIN_COLORS[i]);
                    }

                    int hAxisLen = H_AXIS_VALUES.length;
                    float hAxisValuesStep = (graphW - glAverageSpikeGraph.getHOffset()) / (hAxisLen - 1);
                    float[] hAxisValuesVertices = new float[H_AXIS_VERTICES_COUNT];
                    float[] values = new float[hAxisLen];
                    float midScaleValue = (float) surfaceWidth / 2;

                    // draw horizontal axis scales
                    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
                    gl.glColor4f(1f, 1f, 1f, 1f);
                    gl.glLineWidth(H_AXIS_WIDTH);
                    int j = 0;
                    for (int i = 0; i < hAxisLen; i++) {
                        float value = hOffset + hAxisValuesStep * i;
                        hAxisValuesVertices[j++] = value;
                        hAxisValuesVertices[j++] = vOffset - MARGIN;
                        hAxisValuesVertices[j++] = value;
                        hAxisValuesVertices[j++] = vOffset - MARGIN - H_AXiS_SCALE_HEIGHT;

                        values[i] = value;
                        if (i == hAxisLen / 2) midScaleValue = value;
                    }
                    hAxisVFB.put(hAxisValuesVertices);
                    hAxisVFB.position(0);
                    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, hAxisVFB);
                    gl.glDrawElements(GL10.GL_LINE_STRIP, H_AXiS_INDICES.length, GL10.GL_UNSIGNED_SHORT, hAxisISB);
                    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

                    // draw horizontal axis values
                    gl.glEnable(GL10.GL_TEXTURE_2D);
                    glText.begin(1f, 1f, 1f, 1f);
                    for (int i = 0; i < hAxisLen; i++) {
                        String value = String.valueOf(H_AXIS_VALUES[i]);
                        glText.drawCX(value, values[i], vOffset - (textH + H_AXIS_VALUES_MARGIN + MARGIN));
                    }
                    glText.drawCX(TIME_MS, midScaleValue, MARGIN);
                    glText.end();
                    gl.glDisable(GL10.GL_TEXTURE_2D);
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
