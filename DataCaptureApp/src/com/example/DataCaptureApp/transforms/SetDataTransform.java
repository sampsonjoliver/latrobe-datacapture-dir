package com.example.DataCaptureApp.transforms;

import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

/**
 * Created by Tom on 26/10/2014.
 */
public class SetDataTransform extends DataTransform
{
    private String mKey;
    private Object mValue;

    public SetDataTransform(String key, Object value)
    {
        mKey = key;
        mValue = value;
    }

    @Override
    public synchronized Data transform(Data data)
    {
        data.set(mKey, mValue);
        return data;
    }
}
