package com.example.DataCaptureApp.transforms;

import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

/**
 * Created by Tom on 12/09/2014.
 */
public class FieldRenameDataTransform extends DataTransform
{
    private String[] mFromFields;
    private String[] mToFields;

    public FieldRenameDataTransform(String[] fromFields, String[] toFields)
    {
        mFromFields = fromFields;
        mToFields = toFields;
    }

    public FieldRenameDataTransform(FieldRenameDataTransform other)
    {
        mFromFields = other.mFromFields;
        mToFields = other.mToFields;
    }

    @Override
    public synchronized Data transform(Data data)
    {
        for(int i = 0; i < mFromFields.length; ++i)
        {
            data.set(mToFields[i], data.get(mFromFields[i]));
            data.remove(mFromFields[i]);
        }
        return data;
    }
}
