package com.example.DataCaptureApp.transforms;

import com.example.DataCaptureApp.utils.Quaternion;
import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

/**
 * Created by Tom on 26/10/2014.
 */
public class QuaternionDifferenceDataTransform extends DataTransform
{
    private String mMagnitudeField;
    // 0 = X, 1 = Y, 2 = Z, 3 = W
    private String[] mDifferenceFields;
    private String[] mPrevFields;
    private String[] mNewFields;

    public QuaternionDifferenceDataTransform(String[] prevFields, String[] newFields, String[] differenceFields, String magnitudeField)
    {
        if(prevFields.length != 4 || newFields.length != 4 || differenceFields.length != 4)
            throw new IllegalArgumentException("Field arrays for quaternions must be 4 wide!");
        mMagnitudeField = magnitudeField;
        mDifferenceFields = differenceFields;
        mPrevFields = prevFields;
        mNewFields = newFields;
    }

    @Override
    public synchronized Data transform (Data data)
    {
        // Perform domain calculations
        return performKneeCalculations(data);
    }

    private Data performKneeCalculations(Data data)
    {
        // Extract prev quaternion
        float[] pQuaternion;
        Float pW = data.get(mPrevFields[3]);
        if (pW == null || pW == 0)
        {
            pQuaternion = new float[3];
            pQuaternion[0] = data.get(mPrevFields[0]);
            pQuaternion[1] = data.get(mPrevFields[1]);
            pQuaternion[2] = data.get(mPrevFields[2]);
        }
        else
        {
            pQuaternion = new float[4];
            pQuaternion[0] = data.get(mPrevFields[3]);
            pQuaternion[1] = data.get(mPrevFields[0]);
            pQuaternion[2] = data.get(mPrevFields[1]);
            pQuaternion[3] = data.get(mPrevFields[2]);
        }

        // Extract new quaternion
        float[] nQuaternion;
        Float nW = data.get(mNewFields[3]);
        if(nW == null || nW == 0)
        {
            nQuaternion = new float[3];
            nQuaternion[0] = data.get(mNewFields[0]);
            nQuaternion[1] = data.get(mNewFields[1]);
            nQuaternion[2] = data.get(mNewFields[2]);
        }
        else
        {
            nQuaternion = new float[4];
            nQuaternion[0] = data.get(mNewFields[3]);
            nQuaternion[1] = data.get(mNewFields[0]);
            nQuaternion[2] = data.get(mNewFields[1]);
            nQuaternion[3] = data.get(mNewFields[2]);
        }
        // Prepare difference quarternion array
        float[] dQuaternion = new float[4];
        // Perform calculations
        Quaternion.getDifferenceQuaternion(dQuaternion, nQuaternion, pQuaternion);
        float magnitude = Quaternion.getQuaternionMagnitude(dQuaternion);
        magnitude = (float)Math.toDegrees(magnitude);
        // Clamp magnitude between 0 and 180
        if(magnitude > 180)
        {
            magnitude = 360 - magnitude;
        }
        // Save difference quaternion
        data.set(mDifferenceFields[0], dQuaternion[3]);
        data.set(mDifferenceFields[1], dQuaternion[0]);
        data.set(mDifferenceFields[2], dQuaternion[1]);
        data.set(mDifferenceFields[3], dQuaternion[2]);
        // Save magnitude
        data.set(mMagnitudeField, magnitude);
        return data;
    }
}
