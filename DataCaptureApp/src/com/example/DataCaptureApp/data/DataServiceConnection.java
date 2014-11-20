package com.example.DataCaptureApp.data;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Tom on 26/10/2014.
 */
public class DataServiceConnection implements ServiceConnection, IEventSource
{
    private Context mContext;
    private DataService mService;
    private IEventListener mServiceListener;
    private Class mDataServiceClass;
    private boolean mIsBound = false;

    public DataServiceConnection(Class dataServiceClass)
    {
        mDataServiceClass = dataServiceClass;
        if(!DataService.class.isAssignableFrom(mDataServiceClass))
        {
            throw new IllegalArgumentException("Given class must be subclass of DataService class!");
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder)
    {
        mService = ((DataService.LocalBinder)binder).getService();
        mServiceListener.onEvent(this, Event.SERVICE_AVAILABLE, mService);
    }

    @Override
    public void onServiceDisconnected(ComponentName name)
    {
        // Handling local services, this callback should not be called!
        Log.d("DataServiceConnection", "onServiceDisconnected callback!");
    }

    public void bind(Context context, IEventListener serviceListener)
    {
        if(!mIsBound)
        {
            mServiceListener = serviceListener;
            context.bindService(new Intent(context, mDataServiceClass), this, Context.BIND_AUTO_CREATE);
            mContext = context;
            mIsBound = true;
        }
        else
        {
            Log.d("DataServiceConnection", "Attempted binding while bound!");
        }
    }

    public void unbind()
    {
        if(mIsBound)
        {
            // Unbind
            mContext.unbindService(this);
            mContext = null;
            mServiceListener = null;
            mIsBound = false;
        }
        else
        {
            Log.d("DataServiceConnection", "Attempted unbinding while unbound!");
        }
    }

    public void start(Context context)
    {
        context.startService(new Intent(context, mDataServiceClass));
    }

    public void stop(Context context)
    {
        mContext.stopService(new Intent(context, mDataServiceClass));
    }

    @Override
    public void setEventListener(IEventListener listener)
    {
        mServiceListener = listener;
    }

    public boolean isBound()
    {
        return mIsBound;
    }

    public DataService getService()
    {
        return mService;
    }
}
