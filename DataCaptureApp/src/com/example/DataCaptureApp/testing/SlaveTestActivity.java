package com.example.DataCaptureApp.testing;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.example.DataCaptureApp.*;
import com.example.DataCaptureApp.data.*;
import com.example.DataCaptureApp.services.BluetoothConnectivityService;

/**
 * Created by Tom on 22/09/2014.
 */
public class SlaveTestActivity extends Activity implements IDataEventListener
{
    private static final String TAG = "SlaveActivity";
    private static final int SOURCE_SERVICE = 0;
    private TextView mStatus;

    private Data mConfig;


    private SlaveService mService;
    private ServiceConnection mServiceConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder)
        {
            mService = (SlaveService)((DataService.LocalBinder)binder).getService();
            mService.setEventListener(SlaveTestActivity.this);
            mService.setDataListener(SlaveTestActivity.this);
            try {
                mService.start(mConfig);
            } catch (FailedInitialisationException e) {
                e.printStackTrace();
                mService = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mService = null;
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_slave);

        mStatus = (TextView)findViewById(R.id.textStatus);

        mConfig = new Data();
        mConfig.set(BluetoothConnectivityService.CONFIG_ROLE, false);
    }

    public void onStartSlave(View v)
    {
        startService(new Intent(this, SlaveService.class));
        bindService(new Intent(this, SlaveService.class), mServiceConn, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);
    }

    public void onStopSlave(View v)
    {
        if(mService != null)
        {
            mService.stop();
            unbindService(mServiceConn);
            stopService(new Intent(this, SlaveService.class));
            mService = null;
        }
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        if(source == mService)
        {
            String status = data.get("status");
            Log.d(TAG, status);
            setStatus(status);
        }
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        setStatus(source + ": " + event);
    }

    public void setStatus(final String status)
    {
        this.runOnUiThread(new Runnable() {
            public void run()
            {
                mStatus.setText(status);
            }
        });
    }
}