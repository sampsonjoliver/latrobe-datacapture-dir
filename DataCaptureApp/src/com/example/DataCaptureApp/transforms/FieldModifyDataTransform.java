package com.example.DataCaptureApp.transforms;

import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

import java.util.Map;
import java.util.Set;

/**
 * Created by Tom on 26/10/2014.
 */
public class FieldModifyDataTransform extends DataTransform
{
    private String mAdd;
    private boolean mPrepend;
    private boolean mFixCase;
    private String[] mExemptFields;

    public FieldModifyDataTransform(String add, String[] exemptFields, boolean prepend, boolean fixCase)
    {
        mAdd = add;
        mPrepend = prepend;
        mFixCase = fixCase;
        mExemptFields = exemptFields;
    }

    @Override
    public synchronized Data transform(Data data)
    {
        Set<Data.Field> fields = data.getFields();
        data.clear();
        for(Data.Field field : fields)
        {
            String oldKey = mFixCase ? field.getKey().substring(0, 1).toUpperCase() + field.getKey().substring(1) : field.getKey();
            String newKey = mPrepend ? mAdd + oldKey : oldKey + mAdd;
            // Avoid modifying keys that are exempt
            for(String exempt : mExemptFields)
            {
                if(exempt.equals(field.getKey()))
                    newKey = field.getKey();
            }
            data.set(newKey, field.getValue());
        }
        return data;
    }
}
