package com.backyardbrains;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
//*/
import android.util.Log;
//import android.widget.FrameLayout;
import android.view.MotionEvent;

import java.util.List;

import com.backyardbrains.audio.AudioService;

import com.backyardbrains.audio.AudioService.AudioServiceBinder;

import com.backyardbrains.BackyardBrainsRecordingsFragment;

//*/
import com.backyardbrains.view.*;

public class BackyardBrainsMain extends FragmentActivity implements ActionBar.TabListener {
	@SuppressWarnings("unused")
	private static final String		TAG	= BackyardBrainsMain.class.getCanonicalName();
	AppSectionsPagerAdapter			mAppSectionsPagerAdapter;
	NonSwipeableViewPager			mViewPager;
	private AudioWriteDoneListener	writeDoneListener;
	private PlayAudioFileListener	playListener;
	private CloseButtonListener 	closeListener;
//	private boolean  bChangePageBroadcastMessage = true;

// private SharedPreferences settings;
// ----------------------------------------------------------------------------------------
	public BackyardBrainsMain() {
		super();
	}
	// ----------------------------------------------------------------------------------------
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
// BackyardBrainsApplication application = (BackyardBrainsApplication)
// getApplication();
// application.startAudioService();
// bindAudioService(true);
		registerAudioWriteDoneReceiver(true);
		initTabsAndFragments();
	}
	// ----------------------------------------------------------------------------------------
	private void initTabsAndFragments() {
// if(mAudioService == null){
// Log.d("BackyardBrainsMain", "initTabsAndFragments: audioservice is null!!");
// }else{
// Log.d("BackyardBrainsMain", "initTabsAndFragments: audioservice is OK");
// }
//

		mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager(), this);// ,
																									// mAudioService);

		mViewPager = (NonSwipeableViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mAppSectionsPagerAdapter);

		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayUseLogoEnabled(false);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		/*
		 * mViewPager.setOnPageChangeListener(new
		 * ViewPager.SimpleOnPageChangeListener() {
		 * @Override public void onPageSelected(int position) {
		 * Log.d("ViewPager.SimpleOnPageChangeListener", "onPageSelected "
		 * +position); actionBar.setSelectedNavigationItem(position); } }); //
		 */

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab().setText(mAppSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
		}

	}

	// ----------------------------------------------------------------------------------------
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		List<Fragment> frags = getSupportFragmentManager().getFragments();
		boolean ret = false;
		if (frags != null) {
			for (int i = 0; i < frags.size(); i++) {
				if (frags.get(i) instanceof BackyardBrainsOscilloscopeFragment) {
					((BackyardBrainsOscilloscopeFragment) frags.get(i)).onTouchEvent(event);
					ret = true;
					break;
				}
			}
		}
		return super.onTouchEvent(event) || ret;
	}

	/*
	 * //-----------------------------------------------------------------------
	 * -----------------
	 * @Override protected void onResume() { //
	 * UIFactory.getUi().registerReceivers(this); // reassignSurfaceView();
	 * super.onResume(); }
	 * //-----------------------------------------------------------------------
	 * -----------------
	 * @Override protected void onPause() { // mAndroidSurface = null; //
	 * UIFactory.getUi().unregisterReceivers(this); super.onPause(); }
	 */ // -----------------------------------------------------------------------

	@Override
	protected void onStart() {
		super.onStart();
		registerPlayAudioFileReceiver(true);
		registerCloseButtonReceiver(true);
		
	}
	// -----------------------------------------------------------------------

	@Override
	protected void onStop() {
		// SharedPreferences.Editor editor =
		// settings.edit(); editor.putBoolean("triggerAutoscaled", false);
		// editor.putBoolean("continuousAutoscaled", false); editor.commit();
		super.onStop();
		registerPlayAudioFileReceiver(false);
		registerCloseButtonReceiver(false);
	} //

	// -----
	/*
	 * @Override public boolean onTouchEvent(MotionEvent event) { //Log.d(TAG,
	 * "touch event! " + event.getAction()); //Log.d(TAG,
	 * "mViewPager child count: " + mViewPager.getChildCount()); int currentItem
	 * = mViewPager.getCurrentItem(); //mViewPager.
	 * mViewPager.getChildAt(currentItem).onTouchEvent(event); return
	 * true;//super.onTouchEvent(event); } //
	 */
	// -----------------------------------------------------------------------------------
	@Override
	protected void onDestroy() {
		// *
		// bindAudioService(false);
// BackyardBrainsApplication application = (BackyardBrainsApplication)
// getApplication();
// application.stopAudioService();
		registerAudioWriteDoneReceiver(false);
		// */
		super.onDestroy();
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		Log.d(TAG, "onTabSelected " + tab.getPosition());
		//changePage(tab.getPosition(), true);
	//	Log.d("BackyardBrainsMain", "changePage " + tab.getPosition());
		if (tab.getPosition() < 0 || tab.getPosition() >= mAppSectionsPagerAdapter.getCount()) {
			return;
		}
		Intent ii = new Intent();
		ii.setAction("BYBonTabSelected");
		ii.putExtra("tab", tab.getPosition());
		getApplicationContext().sendBroadcast(ii);
//		if(tab.getPosition() <2 && bChangePageBroadcastMessage){
//			Intent i = new Intent();
//			i.setAction("BYBSetLiveAudioInput");
//			getApplicationContext().sendBroadcast(i);
//		}
//		bChangePageBroadcastMessage = true;
		if (tab.getPosition() == 0 || tab.getPosition() == 1) {
			mViewPager.setCurrentItem(0);
			List<Fragment> frags = getSupportFragmentManager().getFragments();
			if (frags != null) {
				for (int i = 0; i < frags.size(); i++) {
					if (frags.get(i) instanceof BackyardBrainsOscilloscopeFragment) {
						((BackyardBrainsOscilloscopeFragment) frags.get(i)).setRenderer(tab.getPosition());
//						if(setTab){
//						getActionBar().setSelectedNavigationItem(page);
//						}
						break;
					}
				}
			}
		} else if (mViewPager.getChildCount() > 1) {
			mViewPager.setCurrentItem(1);
//			if(setTab){
//				getActionBar().setSelectedNavigationItem(2);
//			}
		}
// if(tab.getPosition() == 0 || tab.getPosition() == 1){
// mViewPager.setCurrentItem(0);
// List<Fragment> frags = getSupportFragmentManager().getFragments();
// if(frags != null){
// for(int i =0; i < frags.size(); i++){
// if(frags.get(i) instanceof BackyardBrainsOscilloscopeFragment){
// ((BackyardBrainsOscilloscopeFragment)
// frags.get(i)).setRenderer(tab.getPosition());
// break;
// }
// }
// }
// }else if(mViewPager.getChildCount() > 1){
// mViewPager.setCurrentItem(1);
// }
// // mViewPager.setCurrentItem(tab.getPosition());
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		Log.d("BackyardBrainsMain","tab reselected");
	}

	// ----------------------------------------------------------------------------------------
//	public void changePage(int page, boolean broadcastMessage) {
//		Log.d("BackyardBrainsMain", "changePage " + page);
//		if (page < 0 || page >= mAppSectionsPagerAdapter.getCount()) {
//			return;
//		}
//		if(page <2 && bChangePageBroadcastMessage){
//			Intent i = new Intent();
//			i.setAction("BYBSetLiveAudioInput");
//			getApplicationContext().sendBroadcast(i);
//		}
//		bChangePageBroadcastMessage = true;
//		if (page == 0 || page == 1) {
//			mViewPager.setCurrentItem(0);
//			List<Fragment> frags = getSupportFragmentManager().getFragments();
//			if (frags != null) {
//				for (int i = 0; i < frags.size(); i++) {
//					if (frags.get(i) instanceof BackyardBrainsOscilloscopeFragment) {
//						((BackyardBrainsOscilloscopeFragment) frags.get(i)).setRenderer(page);
////						if(setTab){
////						getActionBar().setSelectedNavigationItem(page);
////						}
//						break;
//					}
//				}
//			}
//		} else if (mViewPager.getChildCount() > 1) {
//			mViewPager.setCurrentItem(1);
////			if(setTab){
////				getActionBar().setSelectedNavigationItem(2);
////			}
//		}
//	}

	// ----------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------
	// *
	public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {
		Context context;

		public AppSectionsPagerAdapter(FragmentManager fm, Context ctx) {
			super(fm);
			context = ctx;
		}

		@Override
		public Fragment getItem(int i) {
			Log.d("AppSectionsPagerAdapter", "getItem" + i);
			switch (i) {
			case 0:
			default:
				return new BackyardBrainsOscilloscopeFragment(context);// ,
																		// audioService);
			case 1:
				// return new BackyardBrainsThresholdFragment();
				// case 2:
				// BackyardBrainsRecordingsFragment frag = new
				// BackyardBrainsRecordingsFragment(context);
				// return frag;
				return new BackyardBrainsRecordingsFragment(context);
			}

		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
			default:
				return "Oscilloscope View";
			case 1:
				return "Threshold View";
			case 2:
				return "Recordings List";
			}
		}
	}// */
		// ----------------------------------------------------------------------------------------

	private void registerAudioWriteDoneReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBRecordingSaverSuccessfulSave");
			writeDoneListener = new AudioWriteDoneListener();
			registerReceiver(writeDoneListener, intentFilter);
		} else {
			unregisterReceiver(writeDoneListener);
		}
	}

	// ----------------------------------------------------------------------------------------
	private class AudioWriteDoneListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			Log.d(TAG, "BYBRecordingSaverSuccessfulSave");
		}
	}

	// ----------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------
	/*
	 * protected void bindAudioService(boolean on) { if (on) {
	 * //Log.d(getClass().getCanonicalName(),
	 * "Binding audio service to main activity."); Intent intent = new
	 * Intent(this, AudioService.class); bindService(intent, mConnection,
	 * Context.BIND_AUTO_CREATE); mBindingsCount++;
	 * Log.d(getClass().getCanonicalName(), "Binder called" + mBindingsCount +
	 * "bindings"); } else { //Log.d(getClass().getCanonicalName(),
	 * "unBinding audio service from main activity.");
	 * unbindService(mConnection); mBindingsCount--;
	 * Log.d(getClass().getCanonicalName(), "Unbinder called" + mBindingsCount +
	 * "bindings"); } }
	 * //-----------------------------------------------------------------------
	 * ----------------- protected ServiceConnection mConnection = new
	 * ServiceConnection() { private boolean mAudioServiceIsBound; // Sets a
	 * reference in this activity to the {@link AudioService}, which // allows
	 * for {@link ByteBuffer}s full of audio information to be passed // from
	 * the {@link AudioService} down into the local // {@link
	 * OscilloscopeGLSurfaceView} // // @see
	 * android.content.ServiceConnection#onServiceConnected(android.content.
	 * ComponentName, // android.os.IBinder)
	 * @Override public void onServiceConnected(ComponentName className, IBinder
	 * service) { // We've bound to LocalService, cast the IBinder and get //
	 * LocalService instance AudioServiceBinder binder = (AudioServiceBinder)
	 * service; mAudioService = binder.getService(); mAudioServiceIsBound =
	 * true; Log.d(getClass().getCanonicalName(), "Service connected and bound"
	 * ); initTabsAndFragments(); } // Clean up bindings // // @see
	 * android.content.ServiceConnection#onServiceDisconnected(android.content.
	 * ComponentName)
	 * @Override public void onServiceDisconnected(ComponentName arg0) {
	 * mAudioService = null; mAudioServiceIsBound = false;
	 * Log.d(getClass().getCanonicalName(), "Service disconnected."); } };
	 * //-----------------------------------------------------------------------
	 * ----------------- public AudioService getmAudioService() { return
	 * mAudioService; } //
	 */
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS CLASS
	// -----------------------------------------------------------------------------------------------------------------------------
	private class PlayAudioFileListener extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			//mViewPager.setCurrentItem(0);
			List<Fragment> frags = getSupportFragmentManager().getFragments();
			if (frags != null) {
				for (int i = 0; i < frags.size(); i++) {
					if (frags.get(i) instanceof BackyardBrainsOscilloscopeFragment) {
						((BackyardBrainsOscilloscopeFragment) frags.get(i)).showCloseButton();
						//changePage(0, false);
						//bChangePageBroadcastMessage = false;
						getActionBar().setSelectedNavigationItem(0);
						break;
					}
				}
			}		 
		}
	}

	private class CloseButtonListener extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			//changePage(2, false);
			//bChangePageBroadcastMessage = false;
			getActionBar().setSelectedNavigationItem(2);
		}
	}
	
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS TOGGLES
	// -----------------------------------------------------------------------------------------------------------------------------

	private void registerPlayAudioFileReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBPlayAudioFile");
			playListener = new PlayAudioFileListener();
			getApplicationContext().registerReceiver(playListener, intentFilter);
		} else {
			getApplicationContext().unregisterReceiver(playListener);
		}
	}
	private void registerCloseButtonReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBCloseButton");
			closeListener = new  CloseButtonListener();
			getApplicationContext().registerReceiver(closeListener, intentFilter);
		} else {
			getApplicationContext().unregisterReceiver(closeListener);
		}
	}
}
