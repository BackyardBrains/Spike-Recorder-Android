package com.backyardbrains.audio;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Environment;

public class RecordingSaver {

	private File mFileToRecordTo;
	private BufferedOutputStream bufferedStream;

	public RecordingSaver(String filename) {
		initializeAndCreateFile(filename);
		
	}

	private void initializeAndCreateFile(String filename) {
		mFileToRecordTo = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filename);

		if (mFileToRecordTo == null) {
			throw new IllegalStateException("File to record to is null");
		}

		try {
			mFileToRecordTo.createNewFile();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot create file: " + mFileToRecordTo.toString());
		}
		
		try {
			bufferedStream = new BufferedOutputStream(new FileOutputStream(mFileToRecordTo));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Cannot open file for writing", e);
		}
		DataOutputStream dataOutputStreamInstance = new DataOutputStream(bufferedStream);

	}
}
