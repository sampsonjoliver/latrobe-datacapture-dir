package com.example.DataCaptureApp.utils;

import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.IDataListener;
import com.example.DataCaptureApp.data.IDataSource;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Tom on 27/10/2014.
 */
public class BroadcastDataSource implements IDataSource, IDataListener
{
    private List<IDataListener> mListeners;

    public BroadcastDataSource()
    {
        mListeners = new LinkedList<IDataListener>();
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        for(IDataListener listener : mListeners)
        {
            listener.onData(source, data);
        }
    }

    @Override
    public void setDataListener(IDataListener listener)
    {
        if(!mListeners.contains(listener))
            mListeners.add(listener);
    }

    public void removeDataListener(IDataListener listener)
    {
        mListeners.remove(listener);
    }
}
