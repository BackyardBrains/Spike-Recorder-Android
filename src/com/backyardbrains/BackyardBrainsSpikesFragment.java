package com.backyardbrains;

import java.util.ArrayList;

import com.backyardbrains.analysis.BYBAnalysisManager;
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.BYBColors;
import com.backyardbrains.drawing.FindSpikesRenderer;
import com.backyardbrains.drawing.InteractiveGLSurfaceView;
import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.view.BYBThresholdHandle;
import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

public class BackyardBrainsSpikesFragment extends BackyardBrainsBaseScopeFragment {

//	public  String					TAG				=  "BackyardBrainsSpikesFragment";

	private BYBThresholdHandle					leftThresholdHandle, rightThresholdHandle;
	private ImageButton[]						thresholdButtons;
	private ImageButton							backButton, addButton, trashButton;

	private float[][]							handleColors			;
	private static final int[] handleColorsHex = { 0xffff0000, 0xffffff00, 0xff00ffff };
	// ----------------------------------------------------------------------------------------
	public BackyardBrainsSpikesFragment(){
		super();
		layoutID = R.layout.backyard_spikes;
		rendererClass = FindSpikesRenderer.class;
		handleColors = new float [3][4];
		
		handleColors[0][0] = 1.0f;
		handleColors[0][1] = 0.0f;
		handleColors[0][2] = 0.0f;
		handleColors[0][3] = 1.0f;
		
		handleColors[1][0] = 1.0f;
		handleColors[1][1] = 1.0f;
		handleColors[1][2] = 0.0f;
		handleColors[1][3] = 1.0f;
		
		handleColors[2][0] = 0.0f;
		handleColors[2][1] = 1.0f;
		handleColors[2][2] = 1.0f;
		handleColors[2][3] = 1.0f;
		TAG = BackyardBrainsMain.BYB_SPIKES_FRAGMENT;
	}

	private FindSpikesRenderer getRenderer(){
		return (FindSpikesRenderer)renderer;
	}
	// ----------------------------------------------------------------------------------------
	public void updateThresholdHandles() {
		if (renderer != null && leftThresholdHandle != null && rightThresholdHandle != null && getAnalysisManager() != null) {
				int thresholdsSize = getAnalysisManager().getThresholdsSize();
				int maxThresholds =  getAnalysisManager().getMaxThresholds();
				int selectedThreshold = getAnalysisManager().getSelectedThresholdIndex();
				if (thresholdsSize > 0 && selectedThreshold >= 0 && selectedThreshold < maxThresholds) {
					int[] t = getAnalysisManager().getSelectedThresholds();
					for (int i = 0; i < 2; i++) {
						getRenderer().setThreshold(t[i], i);
					}
					int l = getRenderer().getThresholdScreenValue(FindSpikesRenderer.LEFT_THRESH_INDEX);
					int r = getRenderer().getThresholdScreenValue(FindSpikesRenderer.RIGHT_THRESH_INDEX);
					leftThresholdHandle.setYPosition(l);
					rightThresholdHandle.setYPosition(r);
					float[] currentColor = handleColors[selectedThreshold];

					getRenderer().setCurrentColor(currentColor);
					leftThresholdHandle.setButtonColor(BYBColors.asARGB(BYBColors.getGlColorAsHex(currentColor)));
					rightThresholdHandle.setButtonColor(BYBColors.asARGB(BYBColors.getGlColorAsHex(currentColor)));
				}
				if (thresholdsSize < maxThresholds) {
					addButton.setVisibility(View.VISIBLE);
				} else {
					addButton.setVisibility(View.GONE);
				}
				for (int i = 0; i < maxThresholds; i++) {
					if (i < thresholdsSize) {
						thresholdButtons[i].setVisibility(View.VISIBLE);
					} else {
						thresholdButtons[i].setVisibility(View.GONE);
					}
				}

		}
	}
	private BYBAnalysisManager getAnalysisManager(){
		if(getContext()!=null){
			return ((BackyardBrainsApplication)getContext()).getAnalysisManager();
		}
		return null;
	}
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- FRAGMENT LIFECYCLE
	// ---------------------------------------------------------------------------------------------
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = super.onCreateView(inflater, container, savedInstanceState);
		setupButtons(rootView);
		((BackyardBrainsMain)getActivity()).showButtons(false);
		return rootView;
	}
	// ----------------------------------------------------------------------------------------
	@Override
	public void onResume() {
		super.onResume();
		updateThresholdHandles();

	}

	protected void reassignSurfaceView() {
		super.reassignSurfaceView();
		updateThresholdHandles();
	}

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- THRESHOLDS
	// ---------------------------------------------------------------------------------------------
	protected void selectThreshold(int index) {
		if(getAnalysisManager() != null){
			getAnalysisManager().selectThreshold(index);
		}
	}
	// ----------------------------------------------------------------------------------------
	protected void addThreshold() {
		if(getAnalysisManager() != null){
			getAnalysisManager().addThreshold();
		}
	}
	// ----------------------------------------------------------------------------------------
	protected void removeSelectedThreshold() {
		if(getAnalysisManager() != null){
			getAnalysisManager().removeSelectedThreshold();
		}
	}
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- UI SETUPS
	// ---------------------------------------------------------------------------------------------
	public void setupButtons(View view) {
		if(thresholdButtons != null){
			for(int i = 0; i < thresholdButtons.length; i++){
				thresholdButtons[i] = null;
			}
			thresholdButtons = null;
		}
		thresholdButtons = new ImageButton[3];
		thresholdButtons[0] = ((ImageButton) view.findViewById(R.id.threshold0));
		thresholdButtons[1] = ((ImageButton) view.findViewById(R.id.threshold1));
		thresholdButtons[2] = ((ImageButton) view.findViewById(R.id.threshold2));
		
		
		backButton = ((ImageButton) view.findViewById(R.id.backButton));
		addButton = ((ImageButton) view.findViewById(R.id.new_threshold));
		trashButton = ((ImageButton) view.findViewById(R.id.trash_can));
        if(getContext() != null) {
            leftThresholdHandle = new BYBThresholdHandle(context, ((ImageView) view.findViewById(R.id.leftThresholdHandle)),view.findViewById(R.id.leftThresholdHandleLayout), "LeftSpikesHandle");
            rightThresholdHandle = new BYBThresholdHandle(context, ((ImageView) view.findViewById(R.id.rightThresholdHandle)),view.findViewById(R.id.rightThresholdHandleLayout), "RightSpikesHandle");
        }

		thresholdButtons[0].setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (v.getVisibility() == View.VISIBLE) {
					if (event.getActionIndex() == 0) {
						if (event.getActionMasked() == MotionEvent.ACTION_UP) {
							selectThreshold(0);
						}
					}
					return true;
				}
				return false;
			}
		});

		thresholdButtons[1].setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (v.getVisibility() == View.VISIBLE) {
					if (event.getActionIndex() == 0) {
						if (event.getActionMasked() == MotionEvent.ACTION_UP) {
							selectThreshold(1);
						}
					}
					return true;
				}
				return false;
			}
		});

		thresholdButtons[2].setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (v.getVisibility() == View.VISIBLE) {
					if (event.getActionIndex() == 0) {
						if (event.getActionMasked() == MotionEvent.ACTION_UP) {
							selectThreshold(2);
						}
					}
					return true;
				}
				return false;
			}
		});

		// }
		for (int i = 0; i < thresholdButtons.length; i++) {
			float [] c = new float[4];
			for(int k = 0; k < 4; k ++){
				c[k] = handleColors[i][k];
			}
			thresholdButtons[i].setColorFilter(handleColorsHex[i]);
		}

		backButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (v.getVisibility() == View.VISIBLE) {
					if (event.getActionIndex() == 0) {
						if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                            if(getContext() != null) {
                                Intent j = new Intent();
                                j.setAction("BYBChangePage");
                                j.putExtra("page", BackyardBrainsMain.RECORDINGS_LIST);
                                context.sendBroadcast(j);
                            }
						}
					}
					return true;
				}
				return false;
			}
		});

		addButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (v.getVisibility() == View.VISIBLE) {
					if (event.getActionIndex() == 0) {
						if (event.getActionMasked() == MotionEvent.ACTION_UP) {
							addThreshold();
						}
					}
					return true;
				}
				return false;
			}
		});
		trashButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (v.getVisibility() == View.VISIBLE) {
					if (event.getActionIndex() == 0) {
						if (event.getActionMasked() == MotionEvent.ACTION_UP) {
							removeSelectedThreshold();
						}
					}
					return true;
				}
				return false;
			}
		});
		final SeekBar sk = (SeekBar) view.findViewById(R.id.playheadBar);

		sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					if (renderer != null) {
						getRenderer().setStartSample((float) sk.getProgress() / (float) sk.getMax());
					}
				}
			}
		});
		sk.setProgress(0);
	}
	// ---------------------------------------------------------------------------------------------
	// --------------------------------------------------- LISTENERS
	// ---------------------------------------------------------------------------------------------

	// ------------------------------------------------------------------------ BROADCAST RECEIVERS OBJECTS
	private ThresholdHandlePosListener thresholdHandlePosListener;
	private UpdateThresholdHandleListener updateThresholdHandleListener;
	// ------------------------------------------------------------------------ BROADCAST RECEIVERS CLASS
	private class UpdateThresholdHandleListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			updateThresholdHandles();
		}
	}
    // ------------------------------------------------------------------------
	private class ThresholdHandlePosListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context mContext, android.content.Intent intent) {
			if (intent.hasExtra("y")) {
				float pos = intent.getFloatExtra("y", 0);
				if (intent.hasExtra("name")) {
					if (renderer != null) {
						int index = -1;
						if (intent.getStringExtra("name").equals("LeftSpikesHandle")) {
							index = FindSpikesRenderer.LEFT_THRESH_INDEX;
						} else if (intent.getStringExtra("name").equals("RightSpikesHandle")) {
							index = FindSpikesRenderer.RIGHT_THRESH_INDEX;
						}
						if(getAnalysisManager() != null){

							int thresholdsSize = getAnalysisManager().getThresholdsSize();
							if (thresholdsSize > 0 && index >= 0) {
									int t = (int) getRenderer().pixelHeightToGlHeight(pos);
//									((BackyardBrainsApplication) (context)).
									getAnalysisManager().setThreshold(thresholdsSize - 1, index, t);
									getRenderer().setThreshold(t, index);
								}
//							}
						}
					}
				}
			}
		};
	}
    // ------------------------------------------------------------------------ BROADCAST RECEIVERS TOGGLES
	private void registerUpdateThresholdHandleListener(boolean reg) {
        if(getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBUpdateThresholdHandle");
                updateThresholdHandleListener = new UpdateThresholdHandleListener();

                context.registerReceiver(updateThresholdHandleListener, intentFilter);
            } else {
                context.unregisterReceiver(updateThresholdHandleListener);
                updateThresholdHandleListener = null;
            }
        }
	}
	private void registerThresholdHandlePosListener(boolean reg) {
        if(getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBThresholdHandlePos");
                thresholdHandlePosListener = new ThresholdHandlePosListener();
                context.registerReceiver(thresholdHandlePosListener, intentFilter);
            } else {
                context.unregisterReceiver(thresholdHandlePosListener);
                thresholdHandlePosListener = null;
            }
        }
	}
// ---------------------------------------------------------------------------- REGISTER RECEIVERS
	public void registerReceivers(boolean bReg) {
		super.registerReceivers(bReg);
		registerThresholdHandlePosListener(bReg);
		leftThresholdHandle.registerUpdateThresholdHandleListener(bReg);
		rightThresholdHandle.registerUpdateThresholdHandleListener(bReg);
		registerUpdateThresholdHandleListener(bReg);
	}
	// ------------------------------------------------------------------------ UNREGISTER RECEIVERS
	public void unregisterReceivers() {
		registerReceivers(false);
	}
}