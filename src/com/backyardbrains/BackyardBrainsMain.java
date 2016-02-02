package com.backyardbrains;


import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
//import android.widget.FrameLayout;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
//*
import java.nio.ByteBuffer;
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.AudioService.AudioServiceBinder;
import com.backyardbrains.drawing.ContinuousGLSurfaceView;
//*/
import com.backyardbrains.view.*;


public class BackyardBrainsMain extends FragmentActivity implements ActionBar.TabListener {
	@SuppressWarnings("unused")
	private static final String TAG = BackyardBrainsMain.class.getCanonicalName();
    AppSectionsPagerAdapter mAppSectionsPagerAdapter;
	protected AudioService mAudioService;
	private int mBindingsCount;
	//ViewPager mViewPager;
	//NonSwipeableViewPager mViewPager;
	
	//private SharedPreferences settings;
//----------------------------------------------------------------------------------------
	public BackyardBrainsMain(){
		super();
	}
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager(), this);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
/*
        mViewPager = (NonSwipeableViewPager) findViewById(R.id.pager);
       // mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
    
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            	Log.d("ViewPager.SimpleOnPageChangeListener", "onPageSelected "+position);
                actionBar.setSelectedNavigationItem(position);
            }
        });
        
//*/
        
        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mAppSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
        //*
        BackyardBrainsApplication application = (BackyardBrainsApplication) getApplication();
		application.startAudioService();
		bindAudioService(true);
		//*/
    }
	/*
	//----------------------------------------------------------------------------------------
	@Override
	protected void onResume() {
	//	UIFactory.getUi().registerReceivers(this);
	//	reassignSurfaceView();
		super.onResume();
	}
	//----------------------------------------------------------------------------------------
	@Override
	protected void onPause() {
//		mAndroidSurface = null;
	//	UIFactory.getUi().unregisterReceivers(this);
		super.onPause();
	}
	//----------------------------------------------------------------------------------------
	@Override
	protected void onStart() {
		super.onStart();
	}
	//----------------------------------------------------------------------------------------	
	@Override
	protected void onStop() {
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("triggerAutoscaled", false);
		editor.putBoolean("continuousAutoscaled", false);
		editor.commit();
		super.onStop();
	}
	//*/
	//-----
	/*
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//Log.d(TAG, "touch event! " + event.getAction());
		//Log.d(TAG, "mViewPager child count: " + mViewPager.getChildCount());
		int currentItem = mViewPager.getCurrentItem();
		
			//mViewPager.
		mViewPager.getChildAt(currentItem).onTouchEvent(event);
		return true;//super.onTouchEvent(event);
	}
	//*/
	//-----------------------------------------------------------------------------------
	@Override
	protected void onDestroy() {
		//*
		bindAudioService(false);
		BackyardBrainsApplication application = (BackyardBrainsApplication) getApplication();
		application.stopAudioService();
		//*/
		super.onDestroy();
	}
	//----------------------------------------------------------------------------------------
    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {}
  //----------------------------------------------------------------------------------------
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    	Log.d(TAG, "onTabSelected " + tab.getPosition());
   //     mViewPager.setCurrentItem(tab.getPosition());
    }
  //----------------------------------------------------------------------------------------
    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }
  //----------------------------------------------------------------------------------------
  //----------------------------------------------------------------------------------------
    //*
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {
    	BackyardBrainsMain context;
        public AppSectionsPagerAdapter(FragmentManager fm, BackyardBrainsMain ctx) {
            super(fm);
            context= ctx;
        }

        @Override
        public Fragment getItem(int i) {
        	Log.d("AppSectionsPagerAdapter", "getItem"+i);
        	switch(i){
        	case 0:
        	default:
        		return new BackyardBrainsOscilloscopeFragment(context);
        	case 1:
        		return new BackyardBrainsThresholdFragment();
        	case 2:
        		return new BackyardBrainsRecordingsFragment();
        	}
        
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	switch(position){
        	case 0: 
        	default:
        		return "Oscilloscope View";
        	case 1:
        		return "Threshold View";
        	case 2:
        		return "Recordings List";
        	}
        }
    }//*/
  //----------------------------------------------------------------------------------------
  //----------------------------------------------------------------------------------------
    //*
    protected void bindAudioService(boolean on) {
		if (on) {
			//Log.d(getClass().getCanonicalName(), "Binding audio service to main activity.");
			Intent intent = new Intent(this, AudioService.class);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
			mBindingsCount++;
			Log.d(getClass().getCanonicalName(), "Binder called" + mBindingsCount + "bindings");
		} else {
			//Log.d(getClass().getCanonicalName(), "unBinding audio service from main activity.");
			unbindService(mConnection);
			mBindingsCount--;
			Log.d(getClass().getCanonicalName(), "Unbinder called" + mBindingsCount + "bindings");
		}
	}
  //----------------------------------------------------------------------------------------
	protected ServiceConnection mConnection = new ServiceConnection() {

		private boolean mAudioServiceIsBound;

		
		 // Sets a reference in this activity to the {@link AudioService}, which
		 // allows for {@link ByteBuffer}s full of audio information to be passed
		 // from the {@link AudioService} down into the local
		 // {@link OscilloscopeGLSurfaceView}
		 // 
		 // @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
		 //      android.os.IBinder)
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			AudioServiceBinder binder = (AudioServiceBinder) service;
			mAudioService = binder.getService();
			mAudioServiceIsBound = true;
			Log.d(getClass().getCanonicalName(), "Service connected and bound");
		}

	
		 // Clean up bindings
		 // 
		 // @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
		 
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mAudioService = null;
			mAudioServiceIsBound = false;
			Log.d(getClass().getCanonicalName(), "Service disconnected.");
		}
	};
	
	//----------------------------------------------------------------------------------------
	public AudioService getmAudioService() {
		return mAudioService;
	}
//*/
}
