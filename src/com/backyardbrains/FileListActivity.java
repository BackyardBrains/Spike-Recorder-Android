package com.backyardbrains;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FileListActivity extends ListActivity {

	private File bybDirectory;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_list);

		bybDirectory = new File(Environment.getExternalStorageDirectory()
				+ "/BackyardBrains/");
		rescanFiles();
	}

	void rescanFiles() {
		
		File[] files = bybDirectory.listFiles();

		ListAdapter adapter = new FileListAdapter(this,
				R.layout.file_list_row_layout, files);
		setListAdapter(adapter);
	}
	
	private void emailFile(File f) {
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent
				.putExtra(Intent.EXTRA_SUBJECT, "My BackyardBrains Recording");
		sendIntent.putExtra(Intent.EXTRA_STREAM,
				Uri.parse("file://" + f.getAbsolutePath()));
		sendIntent.setType("audio/wav");
		startActivity(Intent.createChooser(sendIntent, "Email file"));
	}

	private void playAudioFile(File f) {
		MediaPlayer mp = new MediaPlayer();
		try {
			mp.setDataSource(f.getAbsolutePath());
			mp.prepare();
			mp.start();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void deleteFile(final File f) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Delete File");
		builder.setMessage("Are you sure you want to delete "+f.getName()+"?");
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if(!f.delete()) {
					throw new IllegalStateException("Could not delete " + f.getName());
				}
				rescanFiles();
			}
		});
		builder.setNegativeButton("Cancel", null);
		builder.create().show();
	}

	protected void renameFile(final File f) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Rename File");
		builder.setMessage("Please enter the new name for your file.");
		final EditText e = new EditText(this);
		e.setText(f.getName());
		builder.setView(e);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String filename = e.getText().toString();
				File newFile = new File(f.getParent(), filename + ".wav");
				if(!f.renameTo(newFile)) {
					throw new IllegalStateException("Could not rename file to " + newFile.getAbsolutePath());
				}
				rescanFiles();
			}
		});
		builder.setNegativeButton("Cancel", null);
		builder.create().show();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final File f = (File) this.getListAdapter().getItem(position);

		final CharSequence[] actions = { "Email this file", "Play this file",
				"Rename this file", "Delete this file" };

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose an action");
		builder.setCancelable(true);
		builder.setItems(actions, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0:
					emailFile(f);
					break;
				case 1:
					playAudioFile(f);
					break;
				case 2:
					renameFile(f);
					break;
				case 3:
					deleteFile(f);
					break;
				}
			}
		});
		// builder.setIcon(R.drawable.icon);
		AlertDialog d = builder.create();
		d.show();
		super.onListItemClick(l, v, position, id);
	}

	static class FileListViewHolder {
		public TextView filenameView;
		public TextView filesizeView;
		public TextView filedateView;
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
				rowView = balloon.inflate(R.layout.file_list_row_layout, null,
						true);
				holder = new FileListViewHolder();
				holder.filenameView = (TextView) rowView
						.findViewById(R.id.filename);
				holder.filesizeView = (TextView) rowView
						.findViewById(R.id.filesize);
				holder.filedateView = (TextView) rowView
						.findViewById(R.id.file_date);
				rowView.setTag(holder);
			} else {
				holder = (FileListViewHolder) rowView.getTag();
			}
			holder.filenameView.setText(mFiles[position].getName());
			holder.filesizeView.setText(getWaveLengthString(mFiles[position]
					.length()));
			holder.filedateView.setText(new SimpleDateFormat(
					"MMM d, yyyy HH:mm a").format(new Date(mFiles[position]
					.lastModified())));
			return rowView;
		}

		private CharSequence getWaveLengthString(long length) {
			length -= 44;
			long seconds = length / 88200;

			if (seconds >= 60) {
				long minutes = seconds / 60;
				seconds -= minutes * 60;
				return minutes + "m " + seconds + "s";
			} else {
				return seconds + "s";
			}
		}

	}

	@Override
	protected void onDestroy() {
		bybDirectory = null;
		super.onDestroy();
	}

}
