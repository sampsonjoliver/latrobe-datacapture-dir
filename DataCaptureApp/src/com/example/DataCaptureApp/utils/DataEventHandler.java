package com.example.DataCaptureApp.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.example.DataCaptureApp.data.*;

/**
 * Created by Tom on 6/09/2014.
 */
public class DataEventHandler extends Handler implements IDataSource, IEventSource, IDataListener, IEventListener
{
    public static final int FLAG_DATA = 0;
    public static final int FLAG_EVENT = 1;

    IDataListener mDataListener;
    IEventListener mEventListener;
    IDataSource mDataSource;
    IEventSource mEventSource;

    public DataEventHandler(IDataSource dataSource, IEventSource eventSource, IDataEventListener listener, Looper looper)
    {
        this(dataSource, eventSource, listener, listener, looper);
    }

    public DataEventHandler(IDataSource dataSource, IEventSource eventSource, IDataListener dataListener, IEventListener eventListener, Looper looper)
    {
        super(looper);
        mDataListener = dataListener;
        mEventListener = eventListener;
        mDataSource = dataSource;
        mEventSource = eventSource;
    }

    public DataEventHandler(IDataEventListener listener, Looper looper)
    {
        this(listener, listener, looper);
    }

    public DataEventHandler(IDataListener dataListener, IEventListener eventListener, Looper looper)
    {
        this(null, null, dataListener, eventListener, looper);
        mDataSource = this;
        mEventSource = this;
    }

    @Override
    public void handleMessage(Message msg)
    {
        if(msg.what == FLAG_DATA)
        {
            mDataListener.onData(mDataSource, (Data)msg.obj);
        }
        else if(msg.what == FLAG_EVENT)
        {
            mEventListener.onEvent(mEventSource, Event.values[msg.arg1], msg.obj);
        }
    }

    @Override
    public void setDataListener(IDataListener listener)
    {
        mDataListener = listener;
    }

    @Override
    public void setEventListener(IEventListener listener)
    {
        mEventListener = listener;
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        Message msg = obtainMessage(DataEventHandler.FLAG_DATA, data);
        msg.sendToTarget();
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        Message msg = obtainMessage(DataEventHandler.FLAG_EVENT);
        msg.arg1 = event.ordinal();
        msg.obj = arg;
        msg.sendToTarget();
    }
}
