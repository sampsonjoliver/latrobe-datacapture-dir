package com.example.DataCaptureApp.transforms;

import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

/**
 * Created by Tom on 21/09/2014.
 */
public class ArraySplitDataTransform extends DataTransform
{
    private String mSourceKey;
    private String[] mNewKeys;
    private boolean mAppend;


    public ArraySplitDataTransform(String sourceKey, String[] newKeys, boolean append)
    {
        mSourceKey = sourceKey;
        mNewKeys = newKeys;
        mAppend = append;
    }

    @Override
    public synchronized Data transform(Data d)
    {
        Object[] compositeKey = d.get(mSourceKey);
        if(compositeKey != null)
        {
            d.remove(mSourceKey);
            int max = compositeKey.length > mNewKeys.length ? mNewKeys.length : compositeKey.length;
            for (int i = 0; i < max; ++i)
            {
                d.set(mAppend ? mSourceKey + mNewKeys[i] : mNewKeys[i], compositeKey[i]);
            }
        }
        return d;
    }
}
