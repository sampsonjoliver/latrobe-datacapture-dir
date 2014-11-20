package com.example.DataCaptureApp.transforms;

import android.util.Log;
import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Tom on 26/10/2014.
 */
public class IntervalAggregatorDataTransform extends DataTransform
{
    private String mIntervalKey;
    private int mIntervalTime;
    private String[] mAggregatorKeys;

    private long mTimeOffset;

    private List<AggregatorDataTransform> mAggregators = new LinkedList<AggregatorDataTransform>();
    private List<Integer> mAggregatorIntervals = new LinkedList<Integer>();


    public IntervalAggregatorDataTransform(String intervalKey, int intervalTime, long timeOffset, String[] aggregatorKeys)
    {
        mIntervalKey = intervalKey;
        mIntervalTime = intervalTime;
        mAggregatorKeys = aggregatorKeys;
        mTimeOffset = timeOffset;
    }

    public void setTimeOffset(long newOffset)
    {
        mTimeOffset = newOffset;
    }

    public void clear()
    {
        mAggregators.clear();
        mAggregatorIntervals.clear();
    }

    @Override
    public synchronized Data transform(Data data)
    {
        Data aggregated = aggregate(data);
        return aggregated;
    }

    private synchronized Data aggregate(Data data)
    {
        long sampleTimestamp = data.get(mIntervalKey);
        long elapsedTime = sampleTimestamp - mTimeOffset;
        int sampleTimeslice = (int)(elapsedTime / mIntervalTime);
        int index = 0;
        AggregatorDataTransform aggregator = null;
        for(int timeslice : mAggregatorIntervals)
        {
            if(timeslice == sampleTimeslice)
            {
                // Correct timeslice found
                aggregator = mAggregators.get(index);
                break;
            }
            else if (timeslice > sampleTimeslice)
            {
                break; // This is the correct index to add at
            }
            ++index;
        }
        if(aggregator == null)
        {
            // Add new aggregator
            mAggregatorIntervals.add(index, sampleTimeslice);
            aggregator = new AggregatorDataTransform(mAggregatorKeys);
            mAggregators.add(index, aggregator);
        }
        // Aggregate this sample
        Data aggregatedData = aggregator.transform(data);
        if(aggregatedData != null)
        {
            // Notify listener with complete
            aggregatedData.set(mIntervalKey, mTimeOffset + (sampleTimeslice * mIntervalTime));
            // Remove completed aggregator and timeslice
            mAggregatorIntervals.remove(index);
            mAggregators.remove(index);
        }
        return aggregatedData;
    }
}
