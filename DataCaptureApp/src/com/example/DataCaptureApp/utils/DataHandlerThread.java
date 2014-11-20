package com.example.DataCaptureApp.utils;

import android.os.HandlerThread;
import android.os.Message;
import com.example.DataCaptureApp.data.*;

/**
 * Created by Tom on 28/10/2014.
 */
public class DataHandlerThread implements IDataListener, IDataSource, IEventListener, IEventSource
{
    public static final String THREAD_NAME = "dataThread";
    private HandlerThread mThread;
    private DataEventHandler mHandler;

    public DataHandlerThread(IDataSource dataSource, IEventSource eventSource, IDataListener dataListener, IEventListener eventListener)
    {
        mThread = new HandlerThread(THREAD_NAME);
        mHandler = new DataEventHandler(dataSource, eventSource, dataListener, eventListener, mThread.getLooper());
    }

    public void start()
    {
        mThread.start();
    }

    public void stop()
    {
        mThread.quit();
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        Message msg = mHandler.obtainMessage(DataEventHandler.FLAG_DATA, data);
        msg.sendToTarget();
    }

    @Override
    public void setDataListener(IDataListener listener)
    {
        mHandler.setDataListener(listener);
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        Message msg = mHandler.obtainMessage(DataEventHandler.FLAG_EVENT);
        msg.arg1 = event.ordinal();
        msg.obj = arg;
        msg.sendToTarget();
    }

    @Override
    public void setEventListener(IEventListener listener)
    {
        mHandler.setEventListener(listener);
    }
}
