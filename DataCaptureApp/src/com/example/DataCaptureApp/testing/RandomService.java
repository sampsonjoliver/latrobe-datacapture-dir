package com.example.DataCaptureApp.testing;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Tom on 3/09/2014.
 */
public class RandomService extends Service {
    private static final String TAG = "RandomService";

    private IServiceListener mListener;

    private Timer mTimer;

    public class LocalBinder extends Binder
    {
        public RandomService getService()
        {
            return RandomService.this;
        }
    }

    private LocalBinder mBinder = new LocalBinder();


    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "Bound");
        // Intent can only be from MainService - unpackage configuration
        final int freq = intent.getExtras().getInt("freq");
        final int min = intent.getExtras().getInt("min");
        final int max = intent.getExtras().getInt("max");

        // Create thread to generate numbers
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                int rand = min + (int)(Math.random()*(max-min));
                if(mListener != null)
                {
                    mListener.onServiceData(RandomService.this, "Random: " + rand);
                }
            }
        }, 0, freq*1000);
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "Created");
    }

    /*
     * Implement this method so service runs indefinitely until stopped!
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "Destroying");
        // Clean up Timer
        if(mTimer != null)
        {
            mTimer.cancel();
        }
        super.onDestroy();
    }


    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.d(TAG, "Unbinding");
        return true;
    }

    /*
     * API methods
     */

    public void setListener(IServiceListener listener)
    {
        mListener = listener;
    }
}
