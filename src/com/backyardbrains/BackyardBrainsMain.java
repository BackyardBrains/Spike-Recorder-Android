package com.backyardbrains;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

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

import com.backyardbrains.BackyardBrainsRecordingsFragment;

//*/
import com.backyardbrains.view.*;

public class BackyardBrainsMain extends FragmentActivity implements ActionBar.TabListener {
	@SuppressWarnings("unused")
	private static final String			TAG						= BackyardBrainsMain.class.getCanonicalName();
	AppSectionsPagerAdapter				mAppSectionsPagerAdapter;
	NonSwipeableViewPager				mViewPager;

//	private AudioWriteDoneListener		writeDoneListener;
	private CloseButtonListener			closeListener;
	//private ChangePageListener			changePageListener;
	private AudioPlaybackStartListener	audioPlaybackStartListener;
	private boolean						bBroadcastTabSelected	= true;

// ----------------------------------------------------------------------------------------
	public BackyardBrainsMain() {
		super();
	}

// ----------------------------------------------------------------------------------------
	private void initTabsAndFragments() {
		mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager(), this);

		mViewPager = (NonSwipeableViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mAppSectionsPagerAdapter);

		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayUseLogoEnabled(false);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab().setText(mAppSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
		}

	}

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- LIFECYCLE OVERRIDES
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		//registerAudioWriteDoneReceiver(true);
		initTabsAndFragments();
	}

	// ----------------------------------------------------------------------------------------
	@Override
	protected void onStart() {
		super.onStart();
		// registerPlayAudioFileReceiver(true);
		registerCloseButtonReceiver(true);
		///registerChangePageReceiver(true);
		registerAudioPlaybackStartReceiver(true);
	}

	// ----------------------------------------------------------------------------------------
	@Override
	protected void onStop() {
		super.onStop();
		// registerPlayAudioFileReceiver(false);
		registerCloseButtonReceiver(false);
	//	registerChangePageReceiver(false);
		registerAudioPlaybackStartReceiver(false);
	}

	// ----------------------------------------------------------------------------------------
	@Override
	protected void onDestroy() {
//		registerAudioWriteDoneReceiver(false);
		super.onDestroy();
	}

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- TOUCH OVERRIDES
// -----------------------------------------------------------------------------------------------------------------------------
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

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- TAB LISTENER OVERRIDES
// -----------------------------------------------------------------------------------------------------------------------------
	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

// ----------------------------------------------------------------------------------------
	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		Log.d(TAG, "onTabSelected " + tab.getPosition());
		if (tab.getPosition() < 0 || tab.getPosition() >= 4) {
			return;
		}
		
//*
		if (tab.getPosition() == 0 || tab.getPosition() == 1) {
			mViewPager.setCurrentItem(0);
// List<Fragment> frags = getSupportFragmentManager().getFragments();
// if (frags != null) {
// for (int i = 0; i < frags.size(); i++) {
// if (frags.get(i) instanceof BackyardBrainsOscilloscopeFragment) {
// ((BackyardBrainsOscilloscopeFragment)
// frags.get(i)).setRenderer(tab.getPosition());
// break;
// }
// }
// }
		} else if (mViewPager.getChildCount() > 1) {
			mViewPager.setCurrentItem(tab.getPosition() - 1);
		}
		//*/
		// if (bBroadcastTabSelected) {
		Intent ii = new Intent();
		ii.setAction("BYBonTabSelected");
		ii.putExtra("tab", tab.getPosition());
		getApplicationContext().sendBroadcast(ii);
		// }
		bBroadcastTabSelected = true;
	}

// ----------------------------------------------------------------------------------------
	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		Log.d("BackyardBrainsMain", "tab reselected");
	}

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- FRAGMENT ADAPTER
// -----------------------------------------------------------------------------------------------------------------------------
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
				return new BackyardBrainsOscilloscopeFragment(context);
			case 1:
				return new BackyardBrainsRecordingsFragment(context);
			case 2:
				return new BackyardBrainsAnalysisFragment(context);	
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
			case 3:
				return "Analysis";
			}
		}
	}

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- BROADCAST RECEIVERS CLASS
// -----------------------------------------------------------------------------------------------------------------------------
//	private class ChangePageListener extends BroadcastReceiver {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			if (intent.hasExtra("page")) {
//				bBroadcastTabSelected = false;
//				// getActionBar().setSelectedNavigationItem(intent.getIntExtra("page",
//				// 0));
//			}
//		}
//	}

// ----------------------------------------------------------------------------------------
	private class CloseButtonListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// changePage(2, false);
			// bChangePageBroadcastMessage = false;
			bBroadcastTabSelected = false;
			Log.d("CloseButtonListener","onReceive");
			getActionBar().setSelectedNavigationItem(2);
		}
	}

//// ----------------------------------------------------------------------------------------
//	private class AudioWriteDoneListener extends BroadcastReceiver {
//		@Override
//		public void onReceive(android.content.Context context, android.content.Intent intent) {
//			Log.d(TAG, "BYBRecordingSaverSuccessfulSave");
//		}
//	}

	// ----------------------------------------------------------------------------------------
	private class AudioPlaybackStartListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			Log.d("AudioPlayBacjStartListener", "onReceive");
			getActionBar().setSelectedNavigationItem(0);
			Intent ii = new Intent();
			ii.setAction("BYBShowCloseButton");
			context.getApplicationContext().sendBroadcast(ii);

		}
	}
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- BROADCAST RECEIVERS TOGGLES
// -----------------------------------------------------------------------------------------------------------------------------

	private void registerCloseButtonReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBCloseButton");
			closeListener = new CloseButtonListener();
			getApplicationContext().registerReceiver(closeListener, intentFilter);
		} else {
			getApplicationContext().unregisterReceiver(closeListener);
		}
	}

//// ----------------------------------------------------------------------------------------
//	private void registerAudioWriteDoneReceiver(boolean reg) {
//		if (reg) {
//			IntentFilter intentFilter = new IntentFilter("BYBRecordingSaverSuccessfulSave");
//			writeDoneListener = new AudioWriteDoneListener();
//			getApplicationContext().registerReceiver(writeDoneListener, intentFilter);
//		} else {
//			getApplicationContext().unregisterReceiver(writeDoneListener);
//		}
//	}

	// ----------------------------------------------------------------------------------------
	private void registerAudioPlaybackStartReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBAudioPlaybackStart");
			audioPlaybackStartListener = new AudioPlaybackStartListener();
			getApplicationContext().registerReceiver(audioPlaybackStartListener, intentFilter);
		} else {
			getApplicationContext().unregisterReceiver(audioPlaybackStartListener);
		}
	}

//	// ----------------------------------------------------------------------------------------
//	private void registerChangePageReceiver(boolean reg) {
//		if (reg) {
//			IntentFilter intentFilter = new IntentFilter("BYBChangePage");
//			changePageListener = new ChangePageListener();
//			getApplicationContext().registerReceiver(changePageListener, intentFilter);
//		} else {
//			getApplicationContext().unregisterReceiver(changePageListener);
//		}
//	}
}
