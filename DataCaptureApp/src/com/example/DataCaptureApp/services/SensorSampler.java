package com.example.DataCaptureApp.services;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.IDataListener;
import com.example.DataCaptureApp.data.IDataSource;

/**
 * Created by Tom on 12/09/2014.
 */
public class SensorSampler implements SensorEventListener, IDataSource
{
    public static final String KEY_SAMPLE_RATE = "sampleRate";
    public static final String KEY_TIMESTAMPE = "timestamp";
    private IDataListener mDataListener;
    private Sensor mSensor;
    private String mKey;
    private int mSampleRate;

    public SensorSampler(IDataListener listener, String key, int sampleRate, Sensor sensor)
    {
        mDataListener = listener;
        mKey = key;
        mSampleRate = sampleRate;
        mSensor = sensor;
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        Data d = processSensorEvent(event);
        mDataListener.onData(this, d);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // Do nothing
    }

    public Data processSensorEvent(SensorEvent event)
    {
        Data d = new Data();
        //d.set("timestamp", event.timestamp);
        d.set("timestamp", System.currentTimeMillis());
        Object[] values = new Object[event.values.length];
        // Box up the floats
        for(int i = 0; i < values.length; ++i)
        {
            values[i] = event.values[i];
        }
        d.set(mKey, values);
        return d;
    }

    public Sensor getSensor()
    {
        return mSensor;
    }

    public int getSampleRate()
    {
        return mSampleRate;
    }

    public String getKey()
    {
        return mKey;
    }

    @Override
    public void setDataListener(IDataListener listener)
    {
        mDataListener = listener;
    }
}
