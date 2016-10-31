package com.backyardbrains;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
//*/
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
//import android.widget.FrameLayout;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import com.backyardbrains.BackyardBrainsRecordingsFragment;
//*/
import com.backyardbrains.view.*;

public class BackyardBrainsMain extends AppCompatActivity implements View.OnClickListener, Animation.AnimationListener{
	@SuppressWarnings("unused")
	private static final String			TAG						= "BackyardBrainsMain";

	public static final int				OSCILLOSCOPE_VIEW		= 0;
	public static final int				THRESHOLD_VIEW			= 1;
	public static final int				RECORDINGS_LIST			= 2;
	public static final int				ANALYSIS_VIEW			= 3;
	public static final int				FIND_SPIKES_VIEW		= 4;

//	private Animation animShow;
//	private Animation animHide;
//	private Animation animHideButtons;
//	private Animation animShowButtons;

	protected Button buttonScope;
	protected Button buttonThresh;
	protected Button buttonRecordings;
	protected BYBSlidingButton buttonsSlider;
	protected View buttons;
	protected View buttonsView;
	private boolean bShowingButtons;
	private int currentFrag = -1;
	private List<Button> allButtons;



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


		//buttonsView = findViewById(R.id.buttons);
		buttons  = findViewById(R.id.buttons);
		buttonsSlider = new BYBSlidingButton(buttons,this, "top bar buttons");
		//buttons= new BYBSlidingButton(buttonsView, getApplicationContext(),
//buttonsView.setVisibility(View.VISIBLE);
//		buttonsView.bringToFront();

//		showButtons(true);
		bShowingButtons = false;

		buttonRecordings.setOnClickListener(this);
		buttonScope.setOnClickListener(this);
		buttonThresh.setOnClickListener(this);

		allButtons = new ArrayList<>();
		allButtons.add(buttonScope);
		allButtons.add(buttonRecordings);
		allButtons.add(buttonThresh);

//		animShowButtons = AnimationUtils.loadAnimation(this, R.anim.slide_in_top);
//		animShowButtons.setAnimationListener(this);
//		animHideButtons  = AnimationUtils.loadAnimation(this, R.anim.slide_out_top);
//		animHideButtons.setAnimationListener(this);

		hideActionBar();
		loadFragment(OSCILLOSCOPE_VIEW);
	}
	@Override
	protected void onStart() {
		super.onStart();
		hideActionBar();
		registerReceivers();
	}
	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceivers();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
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
		if(fragType != currentFrag) {

			currentFrag = fragType;
			Fragment frag = null;
			String fragName = "";
			Intent i = null;
			switch (fragType) {
				//------------------------------
				case RECORDINGS_LIST:
					frag = new BackyardBrainsRecordingsFragment();
					fragName = "BackyardBrainsRecordingsFragment";
					i = new Intent();
					i.putExtra("tab", RECORDINGS_LIST);
					break;
				//------------------------------
				case THRESHOLD_VIEW:
					frag = new BackyardBrainsThresholdFragment();
					fragName = "BackyardBrainsThresholdFragment";
					i = new Intent();
					i.putExtra("tab", THRESHOLD_VIEW);
					break;
				//------------------------------
				case FIND_SPIKES_VIEW:
					frag = new BackyardBrainsSpikesFragment();
					fragName = "BackyardBrainsSpikesFragment";
					break;
				//------------------------------
				case ANALYSIS_VIEW:
					frag = new BackyardBrainsAnalysisFragment();
					fragName = "BackyardBrainsAnalysisFragment";
					break;
				//------------------------------
				case OSCILLOSCOPE_VIEW:
				default:
					frag = new BackyardBrainsOscilloscopeFragment();
					fragName = "BackyardBrainsOscilloscopeFragment";
					i = new Intent();
					i.putExtra("tab", OSCILLOSCOPE_VIEW);
					break;
				//------------------------------
			}
			setSelectedButton(fragType);
			if (frag != null) {
				showFragment(frag, fragName, R.id.fragment_container, FragTransaction.REPLACE, true, R.anim.slide_in_right, R.anim.slide_out_left);
				if (i != null) {
					i.setAction("BYBonTabSelected");
					getApplicationContext().sendBroadcast(i);
				}
			}
		}
	}
	public void popFragment(){
		if(getSupportFragmentManager().getBackStackEntryCount()>1) {
			int lastFragIndex = getSupportFragmentManager().getBackStackEntryCount() -1;
			String lastFragName =getSupportFragmentManager().getBackStackEntryAt(lastFragIndex).getName();
//			if( lastFragName.equals(EntelWelcomeVidPlayer.TAG) || lastFragName.equals(ELearnDelegate.TAG)){
//				((EntelWelcomeApp) getApplicationContext()).controlViaje.goBack(false);
//			}
			getSupportFragmentManager().popBackStack();
		}else{
			Log.i(TAG, "popFragment noStack");
		}
	}
	public void showFragment(Fragment frag, String fragName, int fragContainer, FragTransaction fragTransaction, boolean bAnimate, int animIn, int animOut){
		android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		if(bAnimate) {
			transaction.setCustomAnimations(animIn, animOut);
		}
		if(fragTransaction == FragTransaction.REPLACE) {
			transaction.replace(fragContainer, frag);
			transaction.addToBackStack(fragName);
		}else if(fragTransaction == FragTransaction.REMOVE) {
			transaction.remove(frag);
		}else if(fragTransaction == FragTransaction.ADD) {
			transaction.add(fragContainer, frag);
			transaction.addToBackStack(fragName);
		}
		transaction.commit();
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
	//                      Animation Callbacks
	//////////////////////////////////////////////////////////////////////////////
	public void 	onAnimationStart(Animation animation){
//		Log.w(TAG, "boton continuar start Animation");
	}
	public void onAnimationEnd(Animation animation){
//		Log.w(TAG, "boton continuar endAnimation");
	}
	public void 	onAnimationRepeat(Animation animation){}
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
	//////////////////////////////////////////////////////////////////////////////
	//                      Private Methods
	//////////////////////////////////////////////////////////////////////////////
	void setSelectedButton(int select){
		//
		Button selectedButton = null;
		switch (select){
			case OSCILLOSCOPE_VIEW:
				selectedButton = buttonScope;
				break;
			case THRESHOLD_VIEW:
				selectedButton = buttonThresh;
				break;
			case RECORDINGS_LIST:
				selectedButton = buttonRecordings;
				break;
			default:
				break;
		}
		for(Button b: allButtons) {
			boolean bIsSelected = ( b == selectedButton);
			b.setSelected(bIsSelected);
			b.setTextColor(bIsSelected?0xFFFF8D08: Color.WHITE);
		}
		showButtons(selectedButton != null);
//*/
	}
	void showButtons (boolean bShow){
		buttonsSlider.show(bShow);
//		if(bShowingButtons != bShow) {
			//buttons.setVisibility(bShow? View.VISIBLE : View.GONE);
//			buttons.startAnimation(bShow ? animShowButtons : animHideButtons);
//			bShowingButtons = bShow;
//		}
//		buttonsView.setVisibility(View.VISIBLE);
	//	buttons.show(bShow);
	}
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS CLASS
	// ---------------------------------------------------------------------------------------------
	private ChangePageListener 			changePageListener;
	private AudioPlaybackStartListener	audioPlaybackStartListener;

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
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- REGISTER RECEIVERS
	// ---------------------------------------------------------------------------------------------
	public void registerReceivers() {
		registerAudioPlaybackStartReceiver(true);
		registerChangePageReceiver(true);
	}
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- UNREGISTER RECEIVERS
	// ---------------------------------------------------------------------------------------------
	public void unregisterReceivers() {
		registerAudioPlaybackStartReceiver(false);
		registerChangePageReceiver(false);
	}
}
