package com.example.DataCaptureApp.data;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Tom on 23/09/2014.
 */
public abstract class DataService extends Service implements IDataEventListener, IDataSource, IEventSource
{
    private static final boolean DEBUG_DATA = false;
    protected LocalBinder mBinder = new LocalBinder();

    protected IDataListener mDataListener;
    protected IEventListener mEventListener;

    protected Data mConfig;
    protected State mState = State.UNINITIALISED;

    public class LocalBinder extends Binder
    {
        public DataService getService() { return DataService.this; }
    }

    public enum State
    {
        UNINITIALISED   ,
        STARTING (Event.STARTING),
        STARTED (Event.STARTED),
        STOPPING(Event.STOPPING),
        STOPPED(Event.STOPPED),
        FAILED (Event.FAILED);

        private Event event;
        private State()
        {
            this(null);
        }
        private State(Event event)
        {
            this.event = event;
        }

        public Event event()
        {
            return event;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public void onDestroy()
    {
        logd("Destroying");
        if(mEventListener != null)
            mEventListener.onEvent(this, Event.DESTROYED, null);
    }

    public void start(Data config) throws FailedInitialisationException
    {
        try
        {
            if (!isValidConfig(config))
                throw new FailedInitialisationException("Invalid configuration supplied!");
            mConfig = config;
            changeState(State.STARTING, null);
            doStart();
        }
        catch(FailedInitialisationException e)
        {
            failed(e.getMessage());
            throw e;
        }
    }

    protected abstract void doStart() throws FailedInitialisationException;

    public void stop()
    {
        if(mState != State.STOPPING && mState != State.STOPPED)
        {
            changeState(State.STOPPING, null);
            doStop();
            changeState(State.STOPPED, null);
            stopSelf();
        }
    }

    protected abstract void doStop();

    public void failed(String reason)
    {
        if(mState != State.FAILED)
        {
            changeState(State.FAILED, reason);
            stop();
        }
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        if(DEBUG_DATA)
        {
            logd("Data received: " + data);
        }
        // Do nothing
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        // Do nothing
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

    protected boolean isValidConfig(Data config)
    {
        // By default ignore config
        return true;
    }

    protected void logd(String msg)
    {
        Log.d(this.getClass().getName(), msg);
    }

    public State getState()
    {
        return mState;
    }

    protected void changeState(State state, String msg)
    {
        mState = state;
        logd("State: " + state);
        if(mEventListener != null)
        {
            mEventListener.onEvent(this, state.event(), msg);
        }
    }

    protected void changeState(State state)
    {
        changeState(state, null);
    }
}
