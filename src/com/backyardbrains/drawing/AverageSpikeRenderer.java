package com.backyardbrains.drawing;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.backyardbrains.analysis.BYBAnalysisManager.AverageSpikeData;

import android.content.Context;

public class AverageSpikeRenderer  extends  BYBAnalysisBaseRenderer {

	private static final String TAG = "AverageSpikeRenderer";

	//----------------------------------------------------------------------------------------
	public AverageSpikeRenderer(Context context){
		super(context);
	}
	//----------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------
	@Override
	protected void postDrawingHandler(GL10 gl) {}
	//----------------------------------------------------------------------------------------
	@Override
	protected void drawingHandler(GL10 gl) {
		int margin = 20;
		
		initGL(gl);
		AverageSpikeData [] avg = getManager().getAverageSpikes();
		float aw = width - margin *avg.length;
		float ah = (height - margin *(avg.length +1))/(float)avg.length;
		
		
		
		
		
		if(avg != null){
			BYBMesh mesh = new BYBMesh(BYBMesh.LINES);
			
			
			
			for(int i =0; i < avg.length; i++){
				float xInc = aw/avg[i].averageSpike.length;
				float yOffSet = ((margin + ah)*(i+1));
				for(int j =1; j < avg[i].averageSpike.length; j++){
					mesh.addVertex(xInc * (j -1) + (margin * 2), yOffSet - avg[i].normAverageSpike[j-1] * ah  );					
					mesh.addVertex(xInc * j + (margin * 2), yOffSet - avg[i].normAverageSpike[j] * ah  );
					
					
					
					
//					mesh.addVertex(xInc * (j -1) + (margin * 2), ((margin + ah)*(i) + margin) + avg[i].normAverageSpike[j-1] * ah  );					
//					mesh.addVertex(xInc * j + (margin * 2), ((margin + ah)*(i) + margin)+ avg[i].normAverageSpike[j] * ah  );
					mesh.addColor(BYBColors.getColorAsGlById(i));
					mesh.addColor(BYBColors.getColorAsGlById(i));
					

					
					
				}
				for (int j = 1; j < avg[i].normTopSTDLine.length; j++) {
					mesh.addVertex(xInc * (j - 1) + (margin * 2), yOffSet - avg[i].normTopSTDLine[j - 1] * ah);
					mesh.addVertex(xInc * j + (margin * 2), yOffSet - avg[i].normTopSTDLine[j] * ah);

					mesh.addVertex(xInc * (j - 1) + (margin * 2), yOffSet - avg[i].normBottomSTDLine[j - 1] * ah);
					mesh.addVertex(xInc * j + (margin * 2), yOffSet - avg[i].normBottomSTDLine[j] * ah);
				
					mesh.addColor(BYBColors.getColorAsGlById(BYBColors.white));
					mesh.addColor(BYBColors.getColorAsGlById(BYBColors.white));

					mesh.addColor(BYBColors.getColorAsGlById(BYBColors.white));
					mesh.addColor(BYBColors.getColorAsGlById(BYBColors.white));
				}
				
				mesh.addRectangle(margin*2, margin + (ah+margin)*i, aw, ah, BYBColors.getColorAsGlById(BYBColors.white));
			}
			
			gl.glLineWidth(2.0f);
			
			mesh.draw(gl);
		
		}
//		firstBufferDrawnCheck();
//		autoScaleCheck();
//
//		gl.glMatrixMode(GL10.GL_MODELVIEW);
//		gl.glLoadIdentity();
//
//		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//		gl.glLineWidth(1f);
//		gl.glColor4f(0f, 1f, 0f, 1f);
//		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
//		gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 2);
//		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}
}
