package com.example.DataCaptureApp.transforms;

import com.example.DataCaptureApp.utils.SerialisationUtils;
import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

/**
 * Created by Tom on 9/09/2014.
 */
public class DeserialiseDataTransform extends DataTransform
{
    @Override
    public Data transform(Data d)
    {
        Data deserialised = (Data)SerialisationUtils.deserialise((byte[])d.get("bytes"));
        return deserialised;
    }

    @Override
    public synchronized Data[] transform(Data... d)
    {
        for(int i = 0; i < d.length; ++i)
        {
            d[i] = transform(d[i]);
        }
        return d;
    }
}
