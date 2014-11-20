package com.example.DataCaptureApp.transforms;

import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

/**
 * Created by Tom on 27/10/2014.
 */
public class ArrayCollectDataTransform extends DataTransform
{
    private int mCount;
    private int mIndex = 0;
    private Data[] mData;
    private String mArrayField;

    public ArrayCollectDataTransform(int count, String arrayField)
    {
        if(count < 1)
            throw new IllegalArgumentException("Count must be at least 1!");
        mCount = count;
        mData = new Data[mCount];
        mArrayField = arrayField;
        mIndex = 0;
    }

    @Override
    public Data transform(Data data)
    {
        mData[mIndex] = data;
        ++mIndex;
        if(mIndex == mCount)
        {
            Data arrayData = new Data();
            arrayData.set(mArrayField, mData);
            mData = new Data[mCount];
            mIndex = 0;
            return arrayData;
        }
        return null;
    }

    public void clear()
    {
        for(int i = 0; i < mIndex; ++i)
        {
            mData[i] = null;
        }
        mIndex = 0;
    }

    public boolean isEmpty()
    {
        return mIndex == 0;
    }

    public void flush()
    {
        Data arrayData = new Data();
        arrayData.set(mArrayField, mData);
        mData = new Data[mCount];
        mIndex = 0;
        if(mDataListener != null)
            mDataListener.onData(this, arrayData);
    }
}
