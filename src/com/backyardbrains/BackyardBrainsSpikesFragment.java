package com.backyardbrains;

import java.util.ArrayList;

import com.backyardbrains.drawing.BYBColors;
import com.backyardbrains.drawing.FindSpikesRenderer;
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
import android.widget.SeekBar;

public class BackyardBrainsSpikesFragment extends Fragment {

	public static final String					TAG				= "BackyardBrainsSpikesFragment";

	private SharedPreferences					settings		= null;
	private Context								context			= null;

	protected TwoDimensionScaleGestureDetector	mScaleDetector;
	private ScaleListener						mScaleListener;

	protected GLSurfaceView						mAndroidSurface	= null;
	protected FindSpikesRenderer				renderer		= null;
	private FrameLayout							mainscreenGLLayout;

	private BYBThresholdHandle					leftThresholdHandle, rightThresholdHandle;
	private ImageButton[]						thresholdButtons;
	private ImageButton							backButton, addButton, trashButton;

	private float[][]							handleColors			;
	private static final int[] handleColorsHex = { 0xffff0000, 0xffffff00, 0xff00ffff };
	// ----------------------------------------------------------------------------------------
	public BackyardBrainsSpikesFragment(){
		super();
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
	}
    @Override
	public Context getContext(){
		if(context == null){
			FragmentActivity act = getActivity();
			if(act == null) {
				return null;
			}
			context = act.getApplicationContext();
		}
		return context;
	}
	// ----------------------------------------------------------------------------------------
	public void updateThresholdHandles() {
		if (renderer != null && leftThresholdHandle != null && rightThresholdHandle != null && getContext() != null) {
			if (((BackyardBrainsApplication) context).getAnalysisManager() != null) {
				int thresholdsSize = ((BackyardBrainsApplication) context).getAnalysisManager().getThresholdsSize();
				int maxThresholds = ((BackyardBrainsApplication) context).getAnalysisManager().getMaxThresholds();
				int selectedThreshold = ((BackyardBrainsApplication) context).getAnalysisManager().getSelectedThresholdIndex();
				if (thresholdsSize > 0 && selectedThreshold >= 0 && selectedThreshold < maxThresholds) {
					int[] t = ((BackyardBrainsApplication) context).getAnalysisManager().getSelectedThresholds();
					for (int i = 0; i < 2; i++) {
						renderer.setThreshold(t[i], i);
					}
					int l = renderer.getThresholdScreenValue(FindSpikesRenderer.LEFT_THRESH_INDEX);
					int r = renderer.getThresholdScreenValue(FindSpikesRenderer.RIGHT_THRESH_INDEX);
					leftThresholdHandle.setYPosition(l);
					rightThresholdHandle.setYPosition(r);
					float[] currentColor = handleColors[selectedThreshold];

					renderer.setCurrentColor(currentColor);
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
	}
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- FRAGMENT LIFECYCLE
	// ---------------------------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getContext()!= null) {
			mScaleListener = new ScaleListener();
			mScaleDetector = new TwoDimensionScaleGestureDetector(context, mScaleListener);
		}
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.backyard_spikes, container, false);
		getSettings();
		mainscreenGLLayout = (FrameLayout) rootView.findViewById(R.id.glContainer2);
		setupButtons(rootView);
		((BackyardBrainsMain)getActivity()).showButtons(false);
		return rootView;
	}
	// ----------------------------------------------------------------------------------------
	@Override
	public void onStart() {
		reassignSurfaceView();
		readSettings();
		super.onStart();
	}
	// ----------------------------------------------------------------------------------------
	@Override
	public void onResume() {
		registerReceivers();
		readSettings();
		if (mAndroidSurface != null) {
			mAndroidSurface.onResume();
		}
		updateThresholdHandles();
		super.onResume();
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onPause() {
		if (mAndroidSurface != null) {
			mAndroidSurface.onPause();
		}
		unregisterReceivers();
		saveSettings();
		super.onPause();
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onStop() {
		saveSettings();
		super.onStop();
		mAndroidSurface = null;
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onDestroy() {
		destroyRenderers();
		super.onDestroy();
	}

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- GL RENDERING
	// ----------------------------------------------------------------------------------------
	private void destroyRenderers() {
		if (renderer != null) {
			renderer.close();
			renderer = null;
		}
	}
	// ----------------------------------------------------------------------------------------
	protected void reassignSurfaceView() {
		if (getContext() != null) {
            if(mainscreenGLLayout != null) {
                mainscreenGLLayout.removeAllViews();
                if (renderer != null) {
                    saveSettings();
                    renderer = null;
                }
                if (renderer == null) {
                    renderer = new FindSpikesRenderer(context);
                }
                mAndroidSurface = null;
                mAndroidSurface = new GLSurfaceView(context);
                mAndroidSurface.setRenderer(renderer);
                if(mScaleListener != null) {
                    mScaleListener.setRenderer(renderer);
                }
                mainscreenGLLayout.addView(mAndroidSurface);
            }
			readSettings();
			updateThresholdHandles();
		} else {
			////Log.d(TAG, "context == null");
		}
	}
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- TOUCH
	// ---------------------------------------------------------------------------------------------
	public boolean onTouchEvent(MotionEvent event) {
        if(mScaleDetector != null) {
            mScaleDetector.onTouchEvent(event);
        }
		if (mAndroidSurface != null) {
			return mAndroidSurface.onTouchEvent(event);
		}
		return false;
	}
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- SETTINGS
	// ---------------------------------------------------------------------------------------------
	private SharedPreferences getSettings() {
		if (settings == null) {
			settings = getActivity().getPreferences(BackyardBrainsMain.MODE_PRIVATE);
		}
        return settings;
	}
	// ----------------------------------------------------------------------------------------
	protected void readSettings() {
		if (getSettings() != null) {
			if (renderer != null) {
				renderer.setAutoScaled(settings.getBoolean("spikesRendererAutoscaled", renderer.isAutoScaled()));
				renderer.setGlWindowHorizontalSize(settings.getInt("spikesRendererGlWindowHorizontalSize", renderer.getGlWindowHorizontalSize()));
				renderer.setGlWindowVerticalSize(settings.getInt("spikesRendererGlWindowVerticalSize", renderer.getGlWindowVerticalSize()));
			}
		} else {
			////Log.d(TAG, "Cant Read settings. settings == null");
		}
	}

	// ----------------------------------------------------------------------------------------
	protected void saveSettings() {
		if (getSettings() != null) {
			final SharedPreferences.Editor editor = settings.edit();

			if (renderer != null) {
				editor.putBoolean("spikesRendererAutoscaled", renderer.isAutoScaled());
				editor.putInt("spikesRendererGlWindowHorizontalSize", renderer.getGlWindowHorizontalSize());
				editor.putInt("spikesRendererGlWindowVerticalSize", renderer.getGlWindowVerticalSize());
				editor.commit();
			}

		} else {
			////Log.d(TAG, "Cant Save settings. settings == null");
		}
	}

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- THRESHOLDS
	// ---------------------------------------------------------------------------------------------
	protected void selectThreshold(int index) {
		if (getContext() != null) {
			if (((BackyardBrainsApplication) context).getAnalysisManager() != null) {
				((BackyardBrainsApplication) context).getAnalysisManager().selectThreshold(index);
			}
		}
	}

	// ----------------------------------------------------------------------------------------
	protected void addThreshold() {
		if (getContext() != null) {
			if (((BackyardBrainsApplication) context).getAnalysisManager() != null) {
				((BackyardBrainsApplication) context).getAnalysisManager().addThreshold();
			}
		}
	}
	// ----------------------------------------------------------------------------------------
	protected void removeSelectedThreshold() {
		if (getContext() != null) {
			if (((BackyardBrainsApplication) context).getAnalysisManager() != null) {
				((BackyardBrainsApplication) context).getAnalysisManager().removeSelectedThreshold();
			}
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
            leftThresholdHandle = new BYBThresholdHandle(context, ((ImageButton) view.findViewById(R.id.leftThresholdHandle)),view.findViewById(R.id.leftThresholdHandleLayout), "LeftSpikesHandle");
            rightThresholdHandle = new BYBThresholdHandle(context, ((ImageButton) view.findViewById(R.id.rightThresholdHandle)),view.findViewById(R.id.rightThresholdHandleLayout), "RightSpikesHandle");
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
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					if (renderer != null) {
						renderer.setStartSample((float) sk.getProgress() / (float) sk.getMax());
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
						if (getContext() != null) {
							if (((BackyardBrainsApplication) (context)).getAnalysisManager() != null) {
								int thresholdsSize = ((BackyardBrainsApplication) (context)).getAnalysisManager().getThresholdsSize();
								if (thresholdsSize > 0 && index >= 0) {
									int t = (int) renderer.pixelHeightToGlHeight(pos);
									((BackyardBrainsApplication) (context)).getAnalysisManager().setThreshold(thresholdsSize - 1, index, t);
									renderer.setThreshold(t, index);
								}
							}
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
	public void registerReceivers() {
		registerThresholdHandlePosListener(true);
		leftThresholdHandle.registerUpdateThresholdHandleListener(true);
		rightThresholdHandle.registerUpdateThresholdHandleListener(true);
		registerUpdateThresholdHandleListener(true);
	}
	// ------------------------------------------------------------------------ UNREGISTER RECEIVERS
	public void unregisterReceivers() {
		registerThresholdHandlePosListener(false);
		leftThresholdHandle.registerUpdateThresholdHandleListener(false);
		rightThresholdHandle.registerUpdateThresholdHandleListener(false);
		registerUpdateThresholdHandleListener(false);
	}
}