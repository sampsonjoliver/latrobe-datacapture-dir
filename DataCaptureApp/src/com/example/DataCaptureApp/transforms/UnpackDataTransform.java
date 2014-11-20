package com.example.DataCaptureApp.transforms;

import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

import java.util.Map;
import java.util.Set;

/**
 * Created by Tom on 26/10/2014.
 */
public class UnpackDataTransform extends DataTransform
{
    private String mUnpackField;

    public UnpackDataTransform(String unpackField)
    {
        mUnpackField = unpackField;
    }

    @Override
    public synchronized Data transform(Data data)
    {
        // Attempt to retrieve packed data
        Data packedData = data.get(mUnpackField);
        if(packedData != null)
        {
            data.remove(mUnpackField);
            Set<Data.Field> fields = packedData.getFields();
            for (Data.Field field : fields)
            {
                data.set(field.getKey(), field.getValue());
            }
        }
        return data;
    }
}
