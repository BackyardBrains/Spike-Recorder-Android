package com.backyardbrains;

import java.io.File;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

public class FileListActivity extends ListActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_list);
		
		File BybDirectory = new File(Environment.getExternalStorageDirectory() + "/BackyardBrains/");
		File[] files = BybDirectory.listFiles();
		//String[] files = new String[] {"My files.wav", "Derp_dehurr.mp3"};
		
		ListAdapter adapter = new FileListAdapter(this, R.layout.file_list_row_layout, files);
		//ListAdapter adapter = new ArrayAdapter<String>(this, R.layout.file_list_row_layout, R.id.filename, files);
		setListAdapter(adapter);
		
		BybDirectory = null;
	}

	static class FileListViewHolder {
		public TextView filenameView;
		public TextView filesizeView;
	}
		
	private class FileListAdapter extends ArrayAdapter<File> {

		private Activity mContext;
		private File[] mFiles;

		public FileListAdapter(Activity context, int textViewResourceId,
				File[] objects) {
			super(context, textViewResourceId, objects);
			mContext = context;
			mFiles = objects;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			FileListViewHolder holder;
			
			View rowView = convertView;
			if (rowView == null) {
				LayoutInflater balloon = mContext.getLayoutInflater();
				rowView = balloon.inflate(R.layout.file_list_row_layout, null, true);
				holder = new FileListViewHolder();
				holder.filenameView = (TextView) rowView.findViewById(R.id.filename);
				holder.filesizeView = (TextView) rowView.findViewById(R.id.filesize);
				rowView.setTag(holder);
			} else {
				holder = (FileListViewHolder) rowView.getTag();
			}
			holder.filenameView.setText(mFiles[position].getName());
			
			
			
			holder.filesizeView.setText(getWaveLengthString(mFiles[position].length()));
			return rowView;
		}

		private CharSequence getWaveLengthString(long length) {
			length -= 44;
			long seconds = length / 88200;
			
			if (seconds >= 60) {
				long minutes = seconds / 60;
				seconds -= minutes * 60;
				return minutes + "m " + seconds +"s";
			} else {
				return seconds + "s";
			}
		}
		
	}

}
