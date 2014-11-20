package com.example.DataCaptureApp.transforms;

import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

/**
 * Created by Tom on 26/10/2014.
 */
public class RemoveDataTransform extends DataTransform
{
    private String mKey;

    public RemoveDataTransform(String key)
    {
        mKey = key;
    }

    @Override
    public synchronized Data transform(Data data)
    {
        data.remove(mKey);
        return data;
    }
}
