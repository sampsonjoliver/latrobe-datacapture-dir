package com.example.DataCaptureApp.data;

/**
 * Created by Tom on 6/09/2014.
 */
public class DataTransform implements IDataTransform, IDataSource, IDataListener
{
    protected IDataListener mDataListener;
    @Override
    public synchronized Data transform(Data dp)
    {
        return dp;
    }

    @Override
    public synchronized Data[] transform(Data... dps)
    {
        for(Data d : dps)
            d = transform(d);
        return dps;
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        data = transform(data);
        if(data != null)
            mDataListener.onData(source, data); // Use local source to appear transparent
    }

    @Override
    public void setDataListener(IDataListener listener)
    {
        mDataListener = listener;
    }

    public static void pipeline(DataTransform... transforms)
    {
        DataTransform prev = null;
        for(DataTransform current : transforms)
        {
            if(prev != null)
                prev.setDataListener(current);
            prev = current;
        }
    }
}
