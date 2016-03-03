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

	private static final String					TAG				= "BackyardBrainsSpikesFragment";

	private SharedPreferences					settings		= null;
	private Context								context			= null;

	protected TwoDimensionScaleGestureDetector	mScaleDetector;
	private ScaleListener						mScaleListener;

// protected ContinuousGLSurfaceView mAndroidSurface = null;
	protected GLSurfaceView						mAndroidSurface	= null;
	protected FindSpikesRenderer				renderer		= null;
	private FrameLayout							mainscreenGLLayout;

	private BYBThresholdHandle					leftThresholdHandle, rightThresholdHandle;
	private ImageButton[]						thresholdButtons;
	private ImageButton							backButton, addButton, trashButton;

// private int selectedThreshold = 0;
// private ArrayList<int[]> thresholds;
// public static final int maxThresholds = 3;
//

	private float[][]							colors			= { BYBColors.getColorAsGlById(BYBColors.red), BYBColors.getColorAsGlById(BYBColors.yellow), BYBColors.getColorAsGlById(BYBColors.cyan) };

	// ----------------------------------------------------------------------------------------
	public BackyardBrainsSpikesFragment(Context context) {
		super();
		this.context = context.getApplicationContext();
// thresholds = new ArrayList<int[]>();
// thresholds.add(new int[2]);
	}

	// ----------------------------------------------------------------------------------------
	public void updateThresholdHandles() {
		if (renderer != null && leftThresholdHandle != null && rightThresholdHandle != null && context != null) {
			if (((BackyardBrainsApplication) context).getAnalysisManager() != null) {
				int thresholdsSize = ((BackyardBrainsApplication) context).getAnalysisManager().getThresholdsSize();
				int maxThresholds = ((BackyardBrainsApplication) context).getAnalysisManager().getMaxThresholds();
				int selectedThreshold = ((BackyardBrainsApplication) context).getAnalysisManager().getSelectedThresholdIndex();

				if (thresholdsSize > 0 && selectedThreshold >= 0 && selectedThreshold < maxThresholds) {
					int[] t = ((BackyardBrainsApplication) context).getAnalysisManager().getSelectedThresholds();
					for (int i = 0; i < 2; i++) {
						renderer.setThreshold(t[i], i);
					}
					leftThresholdHandle.setYPosition(renderer.getThresholdScreenValue(FindSpikesRenderer.LEFT_THRESH_INDEX));
					rightThresholdHandle.setYPosition(renderer.getThresholdScreenValue(FindSpikesRenderer.RIGHT_THRESH_INDEX));
					float[] currentColor = colors[selectedThreshold];// (thresholds.size()
																		// - 1)
																		// %
																		// maxThresholds];
					renderer.setCurrentColor(currentColor);
					leftThresholdHandle.setButtonColor(BYBColors.asARGB(BYBColors.getGlColorAsHex(currentColor)));
					rightThresholdHandle.setButtonColor(BYBColors.asARGB(BYBColors.getGlColorAsHex(currentColor)));
					//Log.d(TAG, "updateThresholdHandle!");
				}
				if (thresholdsSize < maxThresholds) {
					addButton.setVisibility(View.VISIBLE);
				} else {
					addButton.setVisibility(View.GONE);
				}
				if (thresholdsSize == 0) {
					trashButton.setVisibility(View.GONE);
					leftThresholdHandle.hide();
					rightThresholdHandle.hide();
				} else {
					trashButton.setVisibility(View.VISIBLE);
					leftThresholdHandle.show();
					rightThresholdHandle.show();
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

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- FRAGMENT LIFECYCLE
	// -----------------------------------------------------------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "----------------------------------------- onCreate begin");
		super.onCreate(savedInstanceState);
		if (context != null) {
			mScaleListener = new ScaleListener();
			mScaleDetector = new TwoDimensionScaleGestureDetector(context, mScaleListener);
		}
		//Log.d(TAG, "***************************************** onCreate end");
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "----------------------------------------- onCreateView begin");
		View rootView = inflater.inflate(R.layout.backyard_spikes, container, false);
		getSettings();
		mainscreenGLLayout = (FrameLayout) rootView.findViewById(R.id.glContainer2);
		setupButtons(rootView);
		//Log.d(TAG, "***************************************** onCreateView end");
		return rootView;
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onStart() {
		//Log.d(TAG, "----------------------------------------- onStart begin");
		reassignSurfaceView();
		readSettings();

		super.onStart();
		//Log.d(TAG, "***************************************** onStart end");
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onResume() {

		//Log.d(TAG, "----------------------------------------- onResume begin");
		registerReceivers();
		readSettings();
		if (mAndroidSurface != null) {
			mAndroidSurface.onResume();
		}
		updateThresholdHandles();
		super.onResume();
		//Log.d(TAG, "***************************************** onResume end");
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onPause() {
		//Log.d(TAG, "----------------------------------------- onPause begin");
		if (mAndroidSurface != null) {
			mAndroidSurface.onPause();
		}
		unregisterReceivers();
		saveSettings();
		super.onPause();
		//Log.d(TAG, "***************************************** onPause end");
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onStop() {
		//Log.d(TAG, "----------------------------------------- onStop begin");
		saveSettings();
		super.onStop();
		mAndroidSurface = null;
		//Log.d(TAG, "***************************************** onStop end");
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onDestroy() {
		//Log.d(TAG, "----------------------------------------- onDestroy begin");
		destroyRenderers();
		super.onDestroy();
		//Log.d(TAG, "***************************************** onDestroy end");
	}

	// -----------------------------------------------------------------------------------------------------------------------------
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
		//Log.d(TAG, "----------------------------------------- reassignSurfaceView begin");
		if (context != null) {
			mAndroidSurface = null;
			mainscreenGLLayout.removeAllViews();
			if (renderer != null) {
				saveSettings();
				renderer = null;
			}
			if (renderer == null) {
				renderer = new FindSpikesRenderer(context);

			}

// mAndroidSurface = new ContinuousGLSurfaceView(context, renderer);
			mAndroidSurface = new GLSurfaceView(context);
			mAndroidSurface.setRenderer(renderer);

			mScaleListener.setRenderer(renderer);

			mainscreenGLLayout.addView(mAndroidSurface);

			readSettings();

			updateThresholdHandles();

			//Log.d(getClass().getCanonicalName(), "Reassigned FindSpikesGLSurfaceView");

		} else {
			//Log.d(TAG, "context == null");
		}
		//Log.d(TAG, "***************************************** reassignSurfaceView end");

	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- TOUCH
	// -----------------------------------------------------------------------------------------------------------------------------
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		if (mAndroidSurface != null) {
			return mAndroidSurface.onTouchEvent(event);
		}
		return false;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- SETTINGS
	// -----------------------------------------------------------------------------------------------------------------------------
	private void getSettings() {
		if (settings == null) {
			settings = getActivity().getPreferences(BackyardBrainsMain.MODE_PRIVATE);
		}
	}

	// ----------------------------------------------------------------------------------------
	protected void readSettings() {
		if (settings != null) {

			if (renderer != null) {
				renderer.setAutoScaled(settings.getBoolean("spikesRendererAutoscaled", renderer.isAutoScaled()));
				renderer.setGlWindowHorizontalSize(settings.getInt("spikesRendererGlWindowHorizontalSize", renderer.getGlWindowHorizontalSize()));
				renderer.setGlWindowVerticalSize(settings.getInt("spikesRendererGlWindowVerticalSize", renderer.getGlWindowVerticalSize()));
				//Log.d(TAG, "renderer readsettings");
				//Log.d(TAG, "isAutoScaled: " + settings.getBoolean("spikesRendererAutoscaled", false));
				//Log.d(TAG, "GlHorizontalSize: " + settings.getInt("spikesRendererGlWindowHorizontalSize", 0)); //
				//Log.d(TAG, "GlVerticalSize: " + settings.getInt("spikesRendererGlWindowVerticalSize", 0));
			} //

		} else {
			//Log.d(TAG, "Cant Read settings. settings == null");
		}
	}

	// ----------------------------------------------------------------------------------------
	protected void saveSettings() {
		if (settings != null) {
			final SharedPreferences.Editor editor = settings.edit();

			if (renderer != null) {
				editor.putBoolean("spikesRendererAutoscaled", renderer.isAutoScaled());
				editor.putInt("spikesRendererGlWindowHorizontalSize", renderer.getGlWindowHorizontalSize());
				editor.putInt("spikesRendererGlWindowVerticalSize", renderer.getGlWindowVerticalSize());
				editor.commit();
				//Log.d(TAG, "renderer saved settings");
				//Log.d(TAG, "rendererAutoscaled " + renderer.isAutoScaled());
				//Log.d(TAG, "rendererGlWindowHorizontalSize " + renderer.getGlWindowHorizontalSize());
				//Log.d(TAG, "rendererGlWindowVerticalSize " + renderer.getGlWindowVerticalSize());
			}

		} else {
			//Log.d(TAG, "Cant Save settings. settings == null");
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- THRESHOLDS
	// -----------------------------------------------------------------------------------------------------------------------------
	protected void selectThreshold(int index) {
// if (index >= 0 && index < maxThresholds) {
		if (context != null) {
			if (((BackyardBrainsApplication) context).getAnalysisManager() != null) {
				((BackyardBrainsApplication) context).getAnalysisManager().selectThreshold(index);
			}
		}
// selectedThreshold = index;
// updateThresholdHandles();
// Log.d(TAG, "selectThreshold " + index);
// for (int i = 0; i < maxThresholds; i++) {
// thresholdButtons[i].
// }
// }
	}

	// ----------------------------------------------------------------------------------------
	protected void addThreshold() {
		if (context != null) {
			if (((BackyardBrainsApplication) context).getAnalysisManager() != null) {
				((BackyardBrainsApplication) context).getAnalysisManager().addThreshold();
			}
		}
// if(thresholds.size() < maxThresholds){
// thresholds.add(new int[2]);
// selectedThreshold = thresholds.size()-1;
// updateThresholdHandles();
// }
	}

	// ----------------------------------------------------------------------------------------
	protected void removeSelectedThreshold() {
		if (context != null) {
			if (((BackyardBrainsApplication) context).getAnalysisManager() != null) {
				((BackyardBrainsApplication) context).getAnalysisManager().removeSelectedThreshold();
			}
		}
// if(thresholds.size() > 0 && thresholds.size() > selectedThreshold){
// thresholds.remove(selectedThreshold);
// selectedThreshold = thresholds.size()-1;
// updateThresholdHandles();
// }
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- UI SETUPS
	// -----------------------------------------------------------------------------------------------------------------------------

	// ----------------------------------------------------------------------------------------

	public void setupButtons(View view) {

		thresholdButtons = new ImageButton[3];
		thresholdButtons[0] = ((ImageButton) view.findViewById(R.id.threshold0));
		thresholdButtons[1] = ((ImageButton) view.findViewById(R.id.threshold1));
		thresholdButtons[2] = ((ImageButton) view.findViewById(R.id.threshold2));
		backButton = ((ImageButton) view.findViewById(R.id.backButton));
		addButton = ((ImageButton) view.findViewById(R.id.new_threshold));
		trashButton = ((ImageButton) view.findViewById(R.id.trash_can));

		leftThresholdHandle = new BYBThresholdHandle(context, ((ImageButton) view.findViewById(R.id.leftThresholdHandle)), "LeftSpikesHandle");
		rightThresholdHandle = new BYBThresholdHandle(context, ((ImageButton) view.findViewById(R.id.rightThresholdHandle)), "RightSpikesHandle");

		// for (int j = 0; j < thresholdButtons.length; j++) {
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
			thresholdButtons[i].setColorFilter(BYBColors.asARGB(BYBColors.getGlColorAsHex(colors[i])));
		}

		backButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (v.getVisibility() == View.VISIBLE) {
					if (event.getActionIndex() == 0) {
						if (event.getActionMasked() == MotionEvent.ACTION_UP) {
							Intent j = new Intent();
							j.setAction("BYBChangePage");
							j.putExtra("page", BackyardBrainsMain.RECORDINGS_LIST);
							context.sendBroadcast(j);
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

	// -----------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------- LISTENERS
	// -----------------------------------------------------------------------------------------------------------------------------

	// ----------------------------------------- BROADCAST RECEIVERS OBJECTS
	private ThresholdHandlePosListener thresholdHandlePosListener;
	private UpdateThresholdHandleListener updateThresholdHandleListener;
	// ----------------------------------------- BROADCAST RECEIVERS CLASS
	// *
	// ------------------------------------------------------------------------
	private class UpdateThresholdHandleListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			updateThresholdHandles();
		}
	}
	// ------------------------------------------------------------------------
	private class ThresholdHandlePosListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			if (intent.hasExtra("y")) {
				float pos = intent.getFloatExtra("y", 0);
				if (intent.hasExtra("name")) {
					if (renderer != null) {
						int index = -1;
						//Log.d(TAG, intent.getStringExtra("name"));
						if (intent.getStringExtra("name").equals("LeftSpikesHandle")) {
							index = FindSpikesRenderer.LEFT_THRESH_INDEX;
						} else if (intent.getStringExtra("name").equals("RightSpikesHandle")) {
							index = FindSpikesRenderer.RIGHT_THRESH_INDEX;
						}
						if (((BackyardBrainsApplication) (context.getApplicationContext())) != null) {
							if (((BackyardBrainsApplication) (context.getApplicationContext())).getAnalysisManager() != null) {
								int thresholdsSize = ((BackyardBrainsApplication) (context.getApplicationContext())).getAnalysisManager().getThresholdsSize();

								if (thresholdsSize > 0 && index >= 0) {
									int t = (int) renderer.pixelHeightToGlHeight(pos);
									((BackyardBrainsApplication) (context.getApplicationContext())).getAnalysisManager().setThreshold(thresholdsSize - 1, index, t);
									// thresholds.get(thresholdsSize - 1)[index]
									// = t;
									renderer.setThreshold(t, index);
								}
							}
						}
					}
				}
			}
		};
	}

// */
// ----------------------------------------- BROADCAST RECEIVERS TOGGLES
// *
	private void registerUpdateThresholdHandleListener(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBUpdateThresholdHandle");
			updateThresholdHandleListener = new UpdateThresholdHandleListener();
			
			context.registerReceiver(updateThresholdHandleListener , intentFilter);
		} else {
			context.unregisterReceiver(updateThresholdHandleListener );
			updateThresholdHandleListener  = null;
		}
	}
	private void registerThresholdHandlePosListener(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBThresholdHandlePos");
			thresholdHandlePosListener = new ThresholdHandlePosListener();
			context.registerReceiver(thresholdHandlePosListener, intentFilter);
		} else {
			context.unregisterReceiver(thresholdHandlePosListener);
			thresholdHandlePosListener = null;
		}
	}

// */
// ----------------------------------------- REGISTER RECEIVERS
	public void registerReceivers() {
		registerThresholdHandlePosListener(true);
		leftThresholdHandle.registerUpdateThresholdHandleListener(true);
		rightThresholdHandle.registerUpdateThresholdHandleListener(true);
		registerUpdateThresholdHandleListener(true);
	}

	// ----------------------------------------- UNREGISTER RECEIVERS
	public void unregisterReceivers() {
		registerThresholdHandlePosListener(false);
		leftThresholdHandle.registerUpdateThresholdHandleListener(false);
		rightThresholdHandle.registerUpdateThresholdHandleListener(false);
		registerUpdateThresholdHandleListener(false);
	}
}