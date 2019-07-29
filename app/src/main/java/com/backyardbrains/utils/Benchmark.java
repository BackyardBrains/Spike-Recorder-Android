package com.backyardbrains.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.tspoon.benchit.Benchit;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class Benchmark {

    private int warmUpCounter = 0;
    private int sessionCounter = 0;
    private int perSessionCounter = 0;
    private boolean benchmarkStarted;

    private String name;
    private int warmUp;
    private int sessions;
    private int measuresPerSession;
    private boolean logBySession;
    private OnBenchmarkListener listener;

    public interface OnBenchmarkListener {
        void onEnd();
    }

    public Benchmark(@NonNull String name) {
        this.name = name;
    }

    public Benchmark warmUp(int warmUp) {
        this.warmUp = warmUp;

        return this;
    }

    public Benchmark sessions(int sessions) {
        this.sessions = sessions - 1;

        return this;
    }

    public Benchmark measuresPerSession(int measuresPerSession) {
        this.measuresPerSession = measuresPerSession - 1;

        return this;
    }

    public Benchmark logBySession(boolean logBySession) {
        this.logBySession = logBySession;

        return this;
    }

    public Benchmark listener(@Nullable OnBenchmarkListener listener) {
        this.listener = listener;

        return this;
    }

    public void start() {
        if (warmUpCounter == warmUp) {
            Benchit.begin(name);
            benchmarkStarted = true;
        } else {
            warmUpCounter++;
        }
    }

    public void end() {
        if (benchmarkStarted) {
            if (perSessionCounter == measuresPerSession) {
                Benchit.end(name);
                if (logBySession) Benchit.analyze(name).log();
                perSessionCounter = 0;

                if (sessionCounter == sessions) {
                    Benchit.analyze(name).log();
                    if (listener != null) listener.onEnd();
                }

                sessionCounter++;
            } else {
                Benchit.end(name);
                perSessionCounter++;
            }
            System.gc();
        }
    }
}
