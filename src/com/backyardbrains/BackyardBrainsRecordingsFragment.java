package com.backyardbrains;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;



import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class BackyardBrainsRecordingsFragment extends ListFragment {

	private static final String	TAG	= "BackyardBrainsRecordingsFragment";

	private File				bybDirectory;
	private Context				context;
	

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- CONSTRUCTOR
// -----------------------------------------------------------------------------------------------------------------------------
	public BackyardBrainsRecordingsFragment(Context context) {
		super();
		this.context = context.getApplicationContext();
	}

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- FRAGMENT LIFECYCLE
// -----------------------------------------------------------------------------------------------------------------------------
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.file_list, container, false);
		return rootView;
	}

// ----------------------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bybDirectory = new File(Environment.getExternalStorageDirectory() + "/BackyardBrains/");
	//	//Log.d(TAG, "bybDirectory: " + bybDirectory.getAbsolutePath());
		rescanFiles();
		registerReceivers();
	}

// ----------------------------------------------------------------------------------------
	@Override
	public void onDestroy() {
		bybDirectory = null;
		unregisterReceivers();
		super.onDestroy();
	}


// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- LIST TASKS
// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final File f = (File) this.getListAdapter().getItem(position);

		final CharSequence[] actions = { "File Details", "Play this file", "Find Spikes", "Autocorrelation", "ISI", "Cross Correlation", "Average Spike", "Email this file", "Rename this file", "Delete this file" };

		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		builder.setTitle("Choose an action");
		builder.setCancelable(true);
		builder.setItems(actions, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0:
					fileDetails(f);
					break;
				case 1:
					playAudioFile(f);
					break;
				case 2:
					findSpikes(f);
					break;
				case 3:
					autocorrelation(f);
					break;
				case 4:
					ISI(f);
					break;
				case 5:
					crossCorrelation(f);
					break;
				case 6:
					averageSpike(f);
					break;
				case 7:
					emailFile(f);
					break;
				case 8:
					renameFile(f);
					break;
				case 9:
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

// ----------------------------------------------------------------------------------------
	void rescanFiles() {
		File[] files = bybDirectory.listFiles();
		if (files != null && files.length > 1) {

			        Arrays.sort(files, new Comparator<File>() {
			             @Override
			             public int compare(File object1, File object2) {
			                return (int) ((object1.lastModified() > object2.lastModified()) ? object1.lastModified(): object2.lastModified());
			            	 //return object1.getName().compareTo(object2.getName());
			             }
			    });

			//Log.d(TAG, "RESCAN FILES!!!!!");
			ListAdapter adapter = new FileListAdapter(this.getActivity(), R.layout.file_list_row_layout, files);
			setListAdapter(adapter);
	
		}
	}

// ----------------------------------------------------------------------------------------
	private void fileDetails(File f) {
	}

// ----------------------------------------------------------------------------------------
	private void playAudioFile(File f) {
		//Log.d(TAG, "----------------playAudioFile------------------");
		Intent i = new Intent();
		i.setAction("BYBPlayAudioFile");
		i.putExtra("filePath", f.getAbsolutePath());
		context.sendBroadcast(i);

	}

// ----------------------------------------------------------------------------------------
	private void findSpikes(File f) {
		if(f.exists()){
			Intent i = new Intent();
			i.setAction("BYBAnalizeFile");
			i.putExtra("filePath", f.getAbsolutePath());
			context.sendBroadcast(i);			
			Intent j = new Intent();
			j.setAction("BYBChangePage");
			j.putExtra("page", BackyardBrainsMain.FIND_SPIKES_VIEW);
			context.sendBroadcast(j);			
		}
	}

// ----------------------------------------------------------------------------------------
	private void autocorrelation(File f) {
		doAnalysis(f, "doAutoCorrelation", "AutoCorrelation");	
	}

// ----------------------------------------------------------------------------------------
	private void ISI(File f) {
		doAnalysis(f, "doISI", "ISI");
	}
// ----------------------------------------------------------------------------------------
	private void crossCorrelation(File f) {
		doAnalysis(f, "doCrossCorrelation", "CrossCorrelation");
	}

// ----------------------------------------------------------------------------------------
	private void averageSpike(File f) {
		doAnalysis(f, "doAverageSpike", "AverageSpike");
	}
	// ----------------------------------------------------------------------------------------
	private void doAnalysis(File f, String process, String render){
		if(f.exists()){
			Intent i = new Intent();
			i.setAction("BYBAnalizeFile");
			i.putExtra("filePath", f.getAbsolutePath());
			i.putExtra(process, true);
			context.sendBroadcast(i);			
			Intent j = new Intent();
			j.setAction("BYBChangePage");
			j.putExtra("page", BackyardBrainsMain.ANALYSIS_VIEW);
			context.sendBroadcast(j);			
			Intent k = new Intent();
			k.setAction("BYBRenderAnalysis");
			k.putExtra(render, true);
			context.sendBroadcast(k);
		}
		
	}
// ----------------------------------------------------------------------------------------
	private void emailFile(File f) {
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "My BackyardBrains Recording");
		sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + f.getAbsolutePath()));
		sendIntent.setType("audio/wav");
		startActivity(Intent.createChooser(sendIntent, "Email file"));
	}

// ----------------------------------------------------------------------------------------
	protected void renameFile(final File f) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		builder.setTitle("Rename File");
		builder.setMessage("Please enter the new name for your file.");
		final EditText e = new EditText(this.getActivity());
		e.setText(f.getName());
		builder.setView(e);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String filename = e.getText().toString();
				File newFile = new File(f.getParent(), filename + ".wav");
				if (!f.renameTo(newFile)) {
					throw new IllegalStateException("Could not rename file to " + newFile.getAbsolutePath());
				}
				rescanFiles();
			}
		});
		builder.setNegativeButton("Cancel", null);
		builder.create().show();
	}

// ----------------------------------------------------------------------------------------
	protected void deleteFile(final File f) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		builder.setTitle("Delete File");
		builder.setMessage("Are you sure you want to delete " + f.getName() + "?");
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (!f.delete()) {
					throw new IllegalStateException("Could not delete " + f.getName());
				}
				rescanFiles();
			}
		});
		builder.setNegativeButton("Cancel", null);
		builder.create().show();
	}

// ----------------------------------------------------------------------------------------
	static class FileListViewHolder {
		public TextView	filenameView;
		public TextView	filesizeView;
		public TextView	filedateView;
	}

// ----------------------------------------------------------------------------------------
	private class FileListAdapter extends ArrayAdapter<File> {

		private Activity	mContext;
		private File[]		mFiles;

		public FileListAdapter(Activity context, int textViewResourceId, File[] objects) {
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
				holder.filedateView = (TextView) rowView.findViewById(R.id.file_date);
				rowView.setTag(holder);
			} else {
				holder = (FileListViewHolder) rowView.getTag();
			}
			holder.filenameView.setText(mFiles[position].getName());
			holder.filesizeView.setText(getWaveLengthString(mFiles[position].length()));
			holder.filedateView.setText(new SimpleDateFormat("MMM d, yyyy HH:mm a").format(new Date(mFiles[position].lastModified())));
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
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS INSTANCES
	// -----------------------------------------------------------------------------------------------------------------------------
	private ToggleRecordingListener toggleRecorder;
	private FileReadReceiver	readReceiver;
	private SuccessfulSaveListener successfulSaveListener;
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS CLASS
	// -----------------------------------------------------------------------------------------------------------------------------
		private class ToggleRecordingListener extends BroadcastReceiver {
			@Override
			public void onReceive(android.content.Context context, android.content.Intent intent) {
				rescanFiles();
			};
		}
		private class FileReadReceiver extends BroadcastReceiver {
			@Override
			public void onReceive(android.content.Context context, android.content.Intent intent) {
			}
		}

		private class SuccessfulSaveListener extends BroadcastReceiver {
			@Override
			public void onReceive(android.content.Context context, android.content.Intent intent) {
				rescanFiles();
			}
		}
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS TOGGLES
	// -----------------------------------------------------------------------------------------------------------------------------
		private void registerSuccessfulSaveReceiver(boolean reg) {
			if (reg) {
				IntentFilter intentFilter = new IntentFilter("BYBRecordingSaverSuccessfulSave");
				successfulSaveListener = new SuccessfulSaveListener();
				context.registerReceiver(successfulSaveListener, intentFilter);
			} else {
				context.unregisterReceiver(successfulSaveListener);
			}
		}
		
	private void registerRecordingToggleReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBToggleRecording");
			toggleRecorder = new ToggleRecordingListener();
			context.registerReceiver(toggleRecorder, intentFilter);
		} else {
			context.unregisterReceiver(toggleRecorder);
		}
	}
	private void registerFileReadReceiver(boolean reg) {
		if (reg) {
			IntentFilter fileReadIntent = new IntentFilter("BYBFileReadIntent");
			readReceiver = new FileReadReceiver();
			context.registerReceiver(readReceiver, fileReadIntent);
		}else{
			context.unregisterReceiver(readReceiver);
		}
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- REGISTER RECEIVERS
	// -----------------------------------------------------------------------------------------------------------------------------
		public void registerReceivers() {
			//Log.d(TAG, "registerReceivers");
			registerRecordingToggleReceiver(true);
			registerFileReadReceiver(true);
			registerSuccessfulSaveReceiver(true);
		}

		public void unregisterReceivers() {
			//Log.d(TAG, "unregisterReceivers");
			registerRecordingToggleReceiver(false);
			registerFileReadReceiver(false);
			registerSuccessfulSaveReceiver(false);
		}
}
