package com.example.DataCaptureApp.transforms;

import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

import java.util.Map;
import java.util.Set;

/**
 * Created by Tom on 26/10/2014.
 */
public class PackDataTransform extends DataTransform
{
    private String mPackingField;
    private String[] mRetainFields;

    public PackDataTransform(String packingField, String[] retainFields)
    {
        mPackingField = packingField;
        mRetainFields = retainFields;
    }

    @Override
    public synchronized Data transform(Data data)
    {
        dataPack(data);
        return data;
    }

    private void dataPack(Data data)
    {
        // Retrieve entry set of the object
        Set<Data.Field> fields = data.getFields();
        // Clear original data
        data.clear();
        // Mirror entry set in a new data object
        Data packedData = new Data();
        for(Data.Field field : fields)
        {
            boolean doRetain = false;
            // Check if this field is to be retained
            for(String retain : mRetainFields)
            {
                if(retain.equals(field.getKey()))
                {
                    doRetain = true;
                    break;
                }
            }
            // If field is to be retained, restore to object, else pack
            if(doRetain)
                data.set(field.getKey(), field.getValue());
            else
                packedData.set(field.getKey(), field.getValue());

        }

        // Add packed data to original data
        data.set(mPackingField, packedData);
    }
}
