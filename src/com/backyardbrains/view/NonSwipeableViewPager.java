package com.backyardbrains.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class NonSwipeableViewPager extends ViewPager {

	public NonSwipeableViewPager(Context context) {
		super(context);
	}

	public NonSwipeableViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// *
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		// Never allow swiping to switch between pages

		// Log.d("NonSwipeableViewPager", "onInterceptTouchEvent: "+
		// event.getAction());
		super.onInterceptTouchEvent(event);
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Log.d("NonSwipeableViewPager", "onTouchEvent: "+ event.getAction());
		// Never allow swiping to switch between pages
		super.onTouchEvent(event);
		return false;
	}
	// */
}
