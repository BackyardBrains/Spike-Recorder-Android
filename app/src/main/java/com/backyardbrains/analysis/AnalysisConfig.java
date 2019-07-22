package com.backyardbrains.analysis;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AnalysisConfig implements Parcelable {

    public static final Creator<AnalysisConfig> CREATOR = new Creator<AnalysisConfig>() {
        @Override public AnalysisConfig createFromParcel(Parcel in) {
            return new AnalysisConfig(in);
        }

        @Override public AnalysisConfig[] newArray(int size) {
            return new AnalysisConfig[size];
        }
    };

    private final String filePath;
    private final @AnalysisType int analysisType;

    public AnalysisConfig(@NonNull String filePath, @AnalysisType int analysisType) {
        this.filePath = filePath;
        this.analysisType = analysisType;
    }

    protected AnalysisConfig(Parcel in) {
        filePath = in.readString();
        analysisType = in.readInt();
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(filePath);
        dest.writeInt(analysisType);
    }

    public String getFilePath() {
        return filePath;
    }

    public int getAnalysisType() {
        return analysisType;
    }
}
