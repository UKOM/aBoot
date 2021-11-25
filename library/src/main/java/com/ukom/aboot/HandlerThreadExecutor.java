package com.ukom.aboot;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.concurrent.Executor;

public class HandlerThreadExecutor implements Executor {

    private HandlerThread mWorkThread;
    private Handler mHandler;

    private volatile boolean isReleased;

    public HandlerThreadExecutor(){
        mWorkThread = new HandlerThread("HttpWorkThread");
        mWorkThread.start();
        mHandler = new Handler(mWorkThread.getLooper());
    }

    @Override
    public void execute(Runnable runnable) {
        if (isReleased) throw new IllegalStateException("Already released!");
        mHandler.post(runnable);
    }

    public void release(){
        isReleased = true;

        mWorkThread.quitSafely();
        mWorkThread = null;
    }
}
