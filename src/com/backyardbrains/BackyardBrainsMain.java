package com.backyardbrains;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import android.support.v4.app.Fragment;
//*/
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
//import android.widget.FrameLayout;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

//*/
import com.backyardbrains.view.*;

public class BackyardBrainsMain extends AppCompatActivity implements View.OnClickListener{
	@SuppressWarnings("unused")
	private static final String			TAG						= "BackyardBrainsMain";

	public static final int				INVALID_VIEW			= -1;
	public static final int				OSCILLOSCOPE_VIEW		= 0;
	public static final int				THRESHOLD_VIEW			= 1;
	public static final int				RECORDINGS_LIST			= 2;
	public static final int				ANALYSIS_VIEW			= 3;
	public static final int				FIND_SPIKES_VIEW		= 4;

	public static final String BYB_RECORDINGS_FRAGMENT	= "BackyardBrainsRecordingsFragment";
	public static final String BYB_THRESHOLD_FRAGMENT	= "BackyardBrainsThresholdFragment";
	public static final String BYB_SPIKES_FRAGMENT		= "BackyardBrainsSpikesFragment";
	public static final String BYB_ANALYSIS_FRAGMENT	= "BackyardBrainsAnalysisFragment";
	public static final String BYB_OSCILLOSCOPE_FRAGMENT= "BackyardBrainsOscilloscopeFragment";


	protected Button buttonScope;
	protected Button buttonThresh;
	protected Button buttonRecordings;
	protected BYBSlidingView buttonsSlider;
	protected View buttons;
	protected View buttonsView;
	protected View recordings_drawer;
	protected BYBSlidingView sliding_drawer;
	private boolean bShowingButtons;
	private int currentFrag = -1;
	private List<Button> allButtons;

	private boolean bShowScalingInstructions = true;
	private boolean bShowingScalingInstructions = false;


	public enum FragTransaction{
		ADD,REPLACE,REMOVE
	}
	//////////////////////////////////////////////////////////////////////////////
	//                       CONSTRUCTOR
	//////////////////////////////////////////////////////////////////////////////
	public BackyardBrainsMain() {
		super();
	}

	//////////////////////////////////////////////////////////////////////////////
	//                       LIFECYCLE OVERRIDES
	//////////////////////////////////////////////////////////////////////////////
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		buttonRecordings = (Button)findViewById(R.id.buttonRecordings);
		buttonScope = (Button)findViewById(R.id.buttonScope);
		buttonThresh = (Button)findViewById(R.id.buttonThresh);

		recordings_drawer = findViewById(R.id.fragment_recordings_list);
		if(recordings_drawer != null){
			sliding_drawer = new BYBSlidingView(recordings_drawer, this, "recordings sliding drawer",R.anim.slide_in_right, R.anim.slide_out_right);
			//recordings_drawer.setVisibility(View.GONE);
			showFragment(new BackyardBrainsRecordingsFragment(), BYB_RECORDINGS_FRAGMENT, R.id.fragment_recordings_list, FragTransaction.REPLACE, false, R.anim.slide_in_right, R.anim.slide_out_right);
		}
		//buttonsView = findViewById(R.id.buttons);
		buttons  = findViewById(R.id.buttons);
		buttonsSlider = new BYBSlidingView(buttons,this, "top bar buttons");
		//buttons= new BYBSlidingView(buttonsView, getApplicationContext(),
//buttonsView.setVisibility(View.VISIBLE);
//		buttonsView.bringToFront();

//		showButtons(true);
		bShowingButtons = false;

		buttonRecordings.setOnClickListener(this);
		buttonScope.setOnClickListener(this);
		buttonThresh.setOnClickListener(this);

		allButtons = new ArrayList<>();
		if(buttonScope!=null) {
			allButtons.add(buttonScope);
		}
		if(buttonRecordings!=null) {
			allButtons.add(buttonRecordings);
		}
		if(buttonThresh!=null) {
			allButtons.add(buttonThresh);
		}
		hideActionBar();
		loadFragment(OSCILLOSCOPE_VIEW);
	}
	@Override
	protected void onStart() {
		super.onStart();
		readSettings();
		hideActionBar();
		registerReceivers();
	}
	@Override
	protected void onStop() {
		super.onStop();
		saveSettings();
		unregisterReceivers();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	//////////////////////////////////////////////////////////////////////////////
	//                       OnKey methods
	//////////////////////////////////////////////////////////////////////////////
	@Override
	public void onBackPressed() {
		boolean bShouldPop = true;
		if(currentFrag == ANALYSIS_VIEW){
			Fragment frag = getSupportFragmentManager().findFragmentByTag(BYB_ANALYSIS_FRAGMENT);
			if(frag != null && frag instanceof BackyardBrainsAnalysisFragment){
				bShouldPop  = false;
				((BackyardBrainsAnalysisFragment)frag).onBackPressed();
			}
		}
		if(bShouldPop){
			popFragment();
		}
	}
	//////////////////////////////////////////////////////////////////////////////
	//                       OnClickListener methods
	//////////////////////////////////////////////////////////////////////////////
	@Override
	public void onClick(View view) {
		loadFragment(view.getId());
	}
	//////////////////////////////////////////////////////////////////////////////
	//                      Fragment managment
	//////////////////////////////////////////////////////////////////////////////
	public void loadFragment(int fragType) {
		if (fragType == R.id.buttonRecordings) {
			fragType =  RECORDINGS_LIST;
		}else if (fragType == R.id.buttonScope) {
			fragType =  OSCILLOSCOPE_VIEW;
		}else if (fragType == R.id.buttonThresh) {
			fragType =  THRESHOLD_VIEW;
		}
		Log.d(TAG, "loadFragment()  fragType: " + fragType + "  currentFrag: " + currentFrag );
		if(fragType != currentFrag) {

			currentFrag = fragType;
			Fragment frag = null;
			String fragName = "";
			switch (fragType) {
				//------------------------------
				case RECORDINGS_LIST:
					frag = new BackyardBrainsRecordingsFragment();
					fragName = BYB_RECORDINGS_FRAGMENT;
					break;
				//------------------------------
				case THRESHOLD_VIEW:
					frag = new BackyardBrainsThresholdFragment();
					fragName = BYB_THRESHOLD_FRAGMENT;
					break;
				//------------------------------
				case FIND_SPIKES_VIEW:
					frag = new BackyardBrainsSpikesFragment();
					fragName = BYB_SPIKES_FRAGMENT;
					break;
				//------------------------------
				case ANALYSIS_VIEW:
					frag = new BackyardBrainsAnalysisFragment();
					fragName = BYB_ANALYSIS_FRAGMENT;
					break;
				//------------------------------
				case OSCILLOSCOPE_VIEW:
				default:
					frag = new BackyardBrainsOscilloscopeFragment();
					fragName = BYB_OSCILLOSCOPE_FRAGMENT;
					break;
				//------------------------------
			}
			if (frag != null) {
				setSelectedButton(fragType);
				if(recordings_drawer != null && fragType==RECORDINGS_LIST){
					Intent i = new Intent();
					i.setAction("BYBRescanFiles");
					getApplicationContext().sendBroadcast(i);
					//showFragment(frag, fragName, R.id.fragment_recordings_list, FragTransaction.REPLACE, false, R.anim.slide_in_right, R.anim.slide_out_right);
				//	sliding_drawer.show(true);
				}else {

					showFragment(frag, fragName, R.id.fragment_container, FragTransaction.REPLACE, true, R.anim.slide_in_right, R.anim.slide_out_left);
				}
			}
		}
	}
	public boolean popFragment(String fragName){
		boolean bPopped = false;
		if(getSupportFragmentManager().getBackStackEntryCount()>1) {
			bPopped = getSupportFragmentManager().popBackStackImmediate(fragName,0);
			Log.w(TAG, "popFragment name: " + fragName);
			int fragType =getFragmentTypeFromName(fragName);
			if( fragType != INVALID_VIEW) {
				Log.w(TAG, "popFragment type: " + fragType);
				setSelectedButton(fragType);
				currentFrag = fragType;
			}
		}else{
			Log.i(TAG, "popFragment noStack");
		}
		return bPopped;
	}
	public boolean popFragment(){
		if(getSupportFragmentManager().getBackStackEntryCount()>1) {
			int lastFragIndex = getSupportFragmentManager().getBackStackEntryCount() -2;
			String lastFragName =getSupportFragmentManager().getBackStackEntryAt(lastFragIndex).getName();
			return popFragment(lastFragName);
		}else{
			Log.i(TAG, "popFragment noStack");
			return false;
		}
	}
	private void printBackstack(){
		if(getSupportFragmentManager().getBackStackEntryCount()>0) {
			String s = "Backstack:\n";
			for(int i = 0; i <getSupportFragmentManager().getBackStackEntryCount(); i++){
				s+=getSupportFragmentManager().getBackStackEntryAt(i).getName()+"\n";
			}
			Log.e(TAG, s);
		}else{
			Log.i(TAG, "printBackstack noStack");
		}
	}
	public void showFragment(Fragment frag, String fragName, int fragContainer, FragTransaction fragTransaction, boolean bAnimate, int animIn, int animOut, boolean bAddToBackstack){
		if(!popFragment(fragName)) {
			android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			if(bAnimate) {
				transaction.setCustomAnimations(animIn, animOut);
			}
			if (fragTransaction == FragTransaction.REPLACE) {
				transaction.replace(fragContainer, frag, fragName);
				if(bAddToBackstack) {
					transaction.addToBackStack(fragName);
				}
			} else if (fragTransaction == FragTransaction.REMOVE) {
				transaction.remove(frag);
			} else if (fragTransaction == FragTransaction.ADD) {
				transaction.add(fragContainer, frag, fragName);
				if(bAddToBackstack) {
					transaction.addToBackStack(fragName);
				}
			}
			transaction.commit();
		}
		printBackstack();
	}
	public void showFragment(Fragment frag, String fragName, int fragContainer, FragTransaction fragTransaction, boolean bAnimate, int animIn, int animOut){
		showFragment(frag, fragName, fragContainer, fragTransaction, bAnimate, animIn, animOut,true);
//		if(!popFragment(fragName)) {
//			android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//			if(bAnimate) {
//				transaction.setCustomAnimations(animIn, animOut);
//			}
//			if (fragTransaction == FragTransaction.REPLACE) {
//				transaction.replace(fragContainer, frag, fragName);
//				transaction.addToBackStack(fragName);
//			} else if (fragTransaction == FragTransaction.REMOVE) {
//				transaction.remove(frag);
//			} else if (fragTransaction == FragTransaction.ADD) {
//				transaction.add(fragContainer, frag, fragName);
//				transaction.addToBackStack(fragName);
//			}
//			transaction.commit();
//		}
//		printBackstack();
	}
	//////////////////////////////////////////////////////////////////////////////
	//                      Action Bar
	//////////////////////////////////////////////////////////////////////////////
	public void setActionBarTitle(String title){
		getSupportActionBar().setTitle(title);
	}
	public void showActionBar(){
//        getSupportActionBar().show();
		android.support.v7.app.ActionBar bar = getSupportActionBar();
		if(bar != null) {
			bar.show();
		}else{
			Log.d(TAG, "show action bar fail. null action bar.");
		}
	}
	public void hideActionBar() {
//        getSupportActionBar().hide();
		android.support.v7.app.ActionBar bar = getSupportActionBar();
		if (bar != null) {
			bar.hide();
		} else {
			Log.d(TAG, "hide action bar fail. null action bar.");
		}
	}
	//////////////////////////////////////////////////////////////////////////////
	//                      Public Methods
	//////////////////////////////////////////////////////////////////////////////
	public String getPageTitle(int position) {
		switch (position) {
			case OSCILLOSCOPE_VIEW:
			default:
				return "Oscilloscope View";
			case THRESHOLD_VIEW:
				return "Threshold View";
			case RECORDINGS_LIST:
				return "Recordings List";
			case ANALYSIS_VIEW:
				return "Analysis";
			case FIND_SPIKES_VIEW:
				return "FindSpikes view";
		}
	}
	public boolean isTouchSupported(){
		return getPackageManager().hasSystemFeature("android.hardware.touchscreen");
	}
	//////////////////////////////////////////////////////////////////////////////
	//                      Private Methods
	//////////////////////////////////////////////////////////////////////////////
	String getFragmentNameFromType(int fragType){
		switch (fragType) {
			case RECORDINGS_LIST:
				return BYB_RECORDINGS_FRAGMENT;
			case THRESHOLD_VIEW:
				return BYB_THRESHOLD_FRAGMENT;

			case FIND_SPIKES_VIEW:
				return BYB_SPIKES_FRAGMENT;

			case ANALYSIS_VIEW:
				return BYB_ANALYSIS_FRAGMENT;

			case OSCILLOSCOPE_VIEW:
				return BYB_OSCILLOSCOPE_FRAGMENT;

			case INVALID_VIEW:
			default:
				return "";
		}
	}
	int getFragmentTypeFromName(String fragName){
		if(fragName.equals(BYB_RECORDINGS_FRAGMENT)) {
			return RECORDINGS_LIST;
		}else if(fragName.equals(BYB_THRESHOLD_FRAGMENT)) {
			return THRESHOLD_VIEW;
		}else if(fragName.equals(BYB_SPIKES_FRAGMENT)) {
			return FIND_SPIKES_VIEW;
		}else if(fragName.equals(BYB_ANALYSIS_FRAGMENT)) {
			return ANALYSIS_VIEW;
		}else if(fragName.equals(BYB_OSCILLOSCOPE_FRAGMENT)) {
			return OSCILLOSCOPE_VIEW;
		}else{
			return INVALID_VIEW;
		}
	}
	void setSelectedButton(String tag){
		setSelectedButton(getFragmentTypeFromName(tag));
	}
	void setSelectedButton(int select){

		Button selectedButton = null;
		Intent i = null;
		Log.e(TAG, "setSelectedButton");
		switch (select){
			case OSCILLOSCOPE_VIEW:
				selectedButton = buttonScope;
				i = new Intent();
				i.putExtra("tab", OSCILLOSCOPE_VIEW);
				break;
			case THRESHOLD_VIEW:
				selectedButton = buttonThresh;
				i = new Intent();
				i.putExtra("tab", THRESHOLD_VIEW);
				break;
			case RECORDINGS_LIST:
				selectedButton = buttonRecordings;
				i = new Intent();
				i.putExtra("tab", RECORDINGS_LIST);
				break;
			default:
				break;
		}
		if(allButtons != null) {
			for (Button b : allButtons) {
				boolean bIsSelected = (b == selectedButton) && (selectedButton != null);
				b.setSelected(bIsSelected);
				b.setTextColor(bIsSelected ? 0xFFFF8D08 : Color.WHITE);
			}
		}
		showButtons(selectedButton != null);
		if(recordings_drawer != null &&  selectedButton != null && sliding_drawer != null){
			sliding_drawer.show(selectedButton == buttonRecordings);
//			recordings_drawer.setVisibility((selectedButton == buttonRecordings)?View.VISIBLE:View.GONE);
		}
		if (i != null) {
			Log.e(TAG, "setSelectedButton: " + getFragmentNameFromType(i.getIntExtra("tab",-1)));
			i.setAction("BYBonTabSelected");
			getApplicationContext().sendBroadcast(i);
		}
	}
	void showButtons (boolean bShow){
		buttonsSlider.show(bShow);
	}

	private void showScalingInstructions(){
			if(bShowScalingInstructions && !bShowingScalingInstructions){
				bShowingScalingInstructions = true;
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle("Instructions");
				alertDialog.setMessage(getString(R.string.scaling_instructions));
				alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "NO",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								bShowScalingInstructions = false;
								bShowingScalingInstructions = false;
								saveSettings();
								dialog.dismiss();
							}
						});
				alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "YES",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								bShowingScalingInstructions = false;
								dialog.dismiss();
							}
						});
				alertDialog.show();
			}
	}


	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS CLASS
	// ---------------------------------------------------------------------------------------------
	private ChangePageListener 			changePageListener;
	private AudioPlaybackStartListener	audioPlaybackStartListener;

	private ShowScalingInstructionsListener showScalingInstructionsListener;

	private class ChangePageListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("page")) {
				if(intent.hasExtra("page")){
					loadFragment(intent.getIntExtra("page", 0));
				}
			}
		}
	}
	// ----------------------------------------------------------------------------------------
	private class AudioPlaybackStartListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
//			getActionBar().setSelectedNavigationItem(0);
			Log.w(TAG, "AudioPlaybackStartListener .onReceive");
			loadFragment(OSCILLOSCOPE_VIEW);
		}
	}
	private class ShowScalingInstructionsListener extends  BroadcastReceiver{
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			showScalingInstructions();
		}
	}
	// ---------------------------------------------------------------------------------------------
// ----------------------------------------- BROADCAST RECEIVERS TOGGLES
// -------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------
	private void registerAudioPlaybackStartReceiver(boolean reg) {
		Log.w(TAG, "registerAudioPlaybackStartReceiver");
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBAudioPlaybackStart");
			audioPlaybackStartListener = new AudioPlaybackStartListener();
			getApplicationContext().registerReceiver(audioPlaybackStartListener, intentFilter);
		} else {
			getApplicationContext().unregisterReceiver(audioPlaybackStartListener);
		}
	}
	// ----------------------------------------------------------------------------------------
	private void registerChangePageReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBChangePage");
			changePageListener = new ChangePageListener();
			getApplicationContext().registerReceiver(changePageListener, intentFilter);
		} else {
			getApplicationContext().unregisterReceiver(changePageListener);
		}
	}
	// ----------------------------------------------------------------------------------------
	private void registerShowScalingInstructionsReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("showScalingInstructions");
			showScalingInstructionsListener = new ShowScalingInstructionsListener();
			getApplicationContext().registerReceiver(showScalingInstructionsListener, intentFilter);
		} else {
			getApplicationContext().unregisterReceiver(showScalingInstructionsListener);
		}
	}
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- REGISTER RECEIVERS
	// ---------------------------------------------------------------------------------------------
	public void registerReceivers() {
		registerAudioPlaybackStartReceiver(true);
		registerChangePageReceiver(true);
		registerShowScalingInstructionsReceiver(true);
	}
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- UNREGISTER RECEIVERS
	// ---------------------------------------------------------------------------------------------
	public void unregisterReceivers() {
		registerAudioPlaybackStartReceiver(false);
		registerChangePageReceiver(false);
		registerShowScalingInstructionsReceiver(false);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// ----------------------------------------- SETTINGS
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected SharedPreferences getSettings() {
		return getPreferences(BackyardBrainsMain.MODE_PRIVATE);
	}
	public void readSettings() {
		if (getSettings() != null) {
			bShowScalingInstructions = getSettings().getBoolean(TAG+"_ShowScalingInstructions", bShowScalingInstructions);
		}
	}
	// ----------------------------------------------------------------------------------------
	public void saveSettings() {
		if (getSettings() != null) {
			final SharedPreferences.Editor editor = getSettings().edit();
			editor.putBoolean(TAG+"_ShowScalingInstructions", bShowScalingInstructions);
			editor.commit();

		}
	}
}
