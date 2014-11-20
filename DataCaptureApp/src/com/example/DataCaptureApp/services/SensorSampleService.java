package com.example.DataCaptureApp.services;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import com.example.DataCaptureApp.data.DataService;
import com.example.DataCaptureApp.data.FailedInitialisationException;
import com.example.DataCaptureApp.data.*;
import com.example.DataCaptureApp.transforms.AggregatorDataTransform;
import com.example.DataCaptureApp.transforms.IntervalAggregatorDataTransform;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Tom on 12/09/2014.
 */
public class SensorSampleService extends DataService
{
    public static final String CONFIG_SAMPLE_RATE = "sampleRate";
    public static final String CONFIG_SENSOR_KEYS = "sensorKeys";
    public static final String CONFIG_SENSOR_TYPES = "sensorTypes";
    public static final String CONFIG_TIMESTAMP_FIELD = "timestamp";

    private String mTimestampField;
    private SensorManager mSensorManager;
    private SensorSampler[] mSamplers;
    private int mSampleFrequency;
    private long mSampleTimeslice;
    private long mStartTime;
    private IntervalAggregatorDataTransform mIntervalAggregator;
    private List<AggregatorDataTransform> mAggregators = new LinkedList<AggregatorDataTransform>();
    private List<Integer> mAggregatorTimeslices = new LinkedList<Integer>();
    private String[] mAggregatorKeys;

    @Override
    protected boolean isValidConfig(Data config)
    {
        boolean sampleRate = config.contains(CONFIG_SAMPLE_RATE, Integer.class);
        boolean sensorKeys = config.contains(CONFIG_SENSOR_KEYS, Object[].class);
        boolean sensorTypes = config.contains(CONFIG_SENSOR_TYPES, Object[].class);
        boolean timestampField = config.contains(CONFIG_TIMESTAMP_FIELD, String.class);
        if(!(sampleRate && sensorKeys && sensorTypes && timestampField))
            return false;
        String[] keys = config.get(CONFIG_SENSOR_KEYS);
        Integer[] types = config.get(CONFIG_SENSOR_TYPES);
        if(keys.length != types.length)
            return false;
        return true;
    }

    @Override
    protected void doStart() throws FailedInitialisationException
    {
        // Retrieve timestamp field
        mTimestampField = mConfig.get(CONFIG_TIMESTAMP_FIELD);
        // Retrieve sensor manager
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        // Determine sampling rate and timeslice time
        mSampleFrequency = mConfig.get(CONFIG_SAMPLE_RATE);
        mSampleTimeslice = 1000 / mSampleFrequency; // 1000ms divide by sample Hz
        // Create SensorSamplers and aggregator key list
        createSensorSamplers(mConfig);
        mIntervalAggregator = new IntervalAggregatorDataTransform(mTimestampField, (int)mSampleTimeslice, mStartTime, mAggregatorKeys);
        changeState(State.STARTED);
    }

    @Override
    protected void doStop()
    {
        stopSampling();
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        super.onData(source, data);
        if(source instanceof SensorSampler)
        {
            Data aggregated = mIntervalAggregator.transform(data);
            if(aggregated != null && mDataListener != null)
                mDataListener.onData(this, aggregated);
            //handleSensorData(dp);
        }
    }

    public void startSampling()
    {
        logd("Starting samplers");
        // Register SensorSamplers
        mStartTime = System.currentTimeMillis();
        mIntervalAggregator.setTimeOffset(mStartTime);
        mIntervalAggregator.clear();
        logd("Start Time: " + mStartTime);
        for(SensorSampler sampler : mSamplers)
        {
            mSensorManager.registerListener(sampler, sampler.getSensor(), calculateRate(sampler.getSampleRate()));
        }
    }

    public void stopSampling()
    {
        logd("Stopping samplers");
        // Unregister SensorSamplers
        if(mSamplers != null)
        {
            for (SensorSampler sampler : mSamplers)
            {
                mSensorManager.unregisterListener(sampler);
            }
        }
    }

    /*
    private void handleSensorData(Data d)
    {
        long sampleTimestamp = d.get("timestamp");
        long elapsedTime = sampleTimestamp - mStartTime;
        int sampleTimeslice = (int)(elapsedTime / mSampleTimeslice);
        int index = 0;
        for(int timeslice : mAggregatorTimeslices)
        {
            if(timeslice == sampleTimeslice)
            {
                // Correct timeslice found
                AggregatorDataTransform aggregator = mAggregators.get(index);
                Data aggregatedData = aggregator.transform(d);
                if(aggregatedData != null)
                {
                    // Notify listener with complete
                    aggregatedData.set("timestamp", mStartTime + (timeslice * mSampleTimeslice));
                    mDataListener.onData(this, aggregatedData);
                    // Remove completed aggregator and timeslice
                    mAggregatorTimeslices.remove(index);
                    mAggregators.remove(index);
                }
                return;

            }
            else if (timeslice > sampleTimeslice)
            {
                break; // This is the correct index to add at
            }
            ++index;
        }
        // Add new aggregator
        mAggregatorTimeslices.add(index, sampleTimeslice);
        AggregatorDataTransform aggregator = new AggregatorDataTransform(mAggregatorKeys);
        aggregator.transform(d); // Aggregate this sample
        mAggregators.add(index, aggregator);
    }
    */

    private void createSensorSamplers(Data config) throws FailedInitialisationException
    {
        mAggregatorKeys = config.get(CONFIG_SENSOR_KEYS);
        Integer[] sensorTypes = config.get(CONFIG_SENSOR_TYPES);
        mSamplers = new SensorSampler[mAggregatorKeys.length];
        for(int i = 0; i < mAggregatorKeys.length; ++i)
        {
            Sensor sensor = mSensorManager.getDefaultSensor(sensorTypes[i]);
            if(sensor == null)
            {
                throw new FailedInitialisationException("No sensor available for sensor type '" + sensorTypes[i] + "'");
            }
            mSamplers[i] = new SensorSampler(this, mAggregatorKeys[i], mSampleFrequency, sensor);
        }
    }

    private int calculateRate(int rateHz)
    {
        return 1000000 / rateHz;
    }
}
