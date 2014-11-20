package com.example.DataCaptureApp.transforms;

import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

/**
 * Created by Tom on 28/10/2014.
 */
public class FieldCopyDataTransform extends DataTransform
{
    private String mFromField;
    private String mToField;

    public FieldCopyDataTransform(String from, String to)
    {
        mFromField = from;
        mToField = to;
    }

    public FieldCopyDataTransform(FieldCopyDataTransform other)
    {
        mFromField = other.mFromField;
        mToField = other.mToField;
    }

    @Override
    public Data transform(Data data)
    {
        Object val = data.get(mFromField);
        if(val != null)
            data.set(mToField, val);
        return data;
    }
}
