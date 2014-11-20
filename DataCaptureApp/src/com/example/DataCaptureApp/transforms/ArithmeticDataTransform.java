package com.example.DataCaptureApp.transforms;

import android.util.Log;
import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;

/**
 * Created by Tom on 26/10/2014.
 */
public class ArithmeticDataTransform extends DataTransform
{
    private String mFieldOperand;
    private double mDoubleValue;
    private long mLongValue;
    private Operation mOperation;
    private boolean mAsLong;

    public enum Operation
    {
        Addition,
        Multiply,
        Subtract,
        Divide
    }

    public ArithmeticDataTransform(String fieldOperand, long value, Operation operation)
    {
        mFieldOperand = fieldOperand;
        mLongValue = value;
        mOperation = operation;
        mAsLong = true;
    }

    public ArithmeticDataTransform(String fieldOperand, double value, Operation operation)
    {
        mFieldOperand = fieldOperand;
        mDoubleValue = value;
        mOperation = operation;
        mAsLong = false;
    }

    @Override
    public Data transform(Data data)
    {
        if(mAsLong)
            data.set(mFieldOperand, calcAsLong(data));
        else
            data.set(mFieldOperand, calcAsDouble(data));
        return data;
    }

    private long calcAsLong(Data data)
    {
        long result = data.get(mFieldOperand);
        switch(mOperation)
        {
            case Addition:
                result += mLongValue;
                break;
            case Multiply:
                result *= mLongValue;
                break;
            case Subtract:
                result -= mLongValue;
                break;
            case Divide:
                result /= mLongValue;
                break;
        }
        return result;
    }

    private double calcAsDouble(Data data)
    {
        double result = data.get(mFieldOperand);
        switch(mOperation)
        {
            case Addition:
                result += mDoubleValue;
                break;
            case Multiply:
                result *= mDoubleValue;
                break;
            case Subtract:
                result -= mDoubleValue;
                break;
            case Divide:
                result /= mDoubleValue;
                break;
        }
        return result;
    }
}
