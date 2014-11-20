package com.example.DataCaptureApp.transforms;

import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

/**
 * Created by Tom on 16/09/2014.
 */
public class AggregatorDataTransform extends DataTransform
{
    private Data mAggregated;
    private String[] mKeys;

    public AggregatorDataTransform(String[] keys)
    {
        mKeys = keys;
        mAggregated = new Data();
    }

    @Override
    public synchronized Data transform(Data data)
    {
        boolean complete = true;
        for(String key : mKeys)
        {
            if(!mAggregated.contains(key))
            {
                Object obj = data.get(key);
                if (obj != null)
                {
                    mAggregated.set(key, obj);
                }
                else
                {
                    complete = false;
                }
            }
        }
        return (complete) ? mAggregated : null;
    }
}
