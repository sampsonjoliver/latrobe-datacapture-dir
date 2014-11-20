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
import android.widget.Toast;
import com.example.DataCaptureApp.services.BluetoothConnectivityService;
import com.example.DataCaptureApp.data.DataService;
import com.example.DataCaptureApp.data.FailedInitialisationException;
import com.example.DataCaptureApp.R;
import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.Event;
import com.example.DataCaptureApp.data.IEventListener;
import com.example.DataCaptureApp.data.IEventSource;

/**
 * Created by Tom on 5/09/2014.
 */
public class BluetoothActivity extends Activity implements IEventListener
{
    public static final String TAG = "BluetoothActivity";
    private TextView mStatus;
    private BluetoothConnectivityService mService;
    private boolean mBound = false;
    private Data mServiceConfig;
    private boolean mIsMaster;

    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mBound = true;
            mService = (BluetoothConnectivityService)((DataService.LocalBinder)binder).getService();
            Log.d(TAG, "Bluetooth Service Connected");
            mService.setEventListener(BluetoothActivity.this);
            try
            {
                mService.start(mServiceConfig);
            }
            catch(FailedInitialisationException e)
            {
                setStatus("Failed to start Bluetooth service!");
                mBound = false;
                unbindService(this);
                mService = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth);
        mStatus = (TextView)findViewById(R.id.labelStatus);
        mServiceConfig = new Data();
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    private void setStatus(final String status)
    {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                mStatus.setText(status);
            }
        });
    }

    public void onButtonMaster(View v)
    {
        Log.d(TAG, "Starting as Master");
        mIsMaster = true;
        mServiceConfig.set(BluetoothConnectivityService.CONFIG_ROLE, true);
        mServiceConfig.set(BluetoothConnectivityService.CONFIG_SLAVE_MAC, "00:90:64:44:57:90"); // LG P990 Bluetooth address!
        bindService(new Intent(this, BluetoothConnectivityService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    }

    public void onButtonSlave(View v)
    {
        Log.d(TAG, "Starting as Slave");
        mIsMaster = false;
        mServiceConfig.set(BluetoothConnectivityService.CONFIG_ROLE, false);
        bindService(new Intent(this, BluetoothConnectivityService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    }

    public void onButtonClose(View v)
    {
        Log.d(TAG, "Closing Connection");
        if(mBound)
        {
            mBound = false;
            mService.stop();
            unbindService(mServiceConn);
            mService = null;

        }
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        if (source == mService)
        {
            setStatus(source + ": " + event);
            if (mIsMaster && event == Event.STARTED)
            {
                // Connected and we are master, write config
                String str = "Service Ready! Latency=" + mService.getLatency() + " Offset=" + mService.getTimeOffset();
                Toast.makeText(this, str, Toast.LENGTH_LONG).show();
            }
            if(event == Event.STOPPING)
            {
                if(mBound)
                {
                    mBound = false;
                    unbindService(mServiceConn);
                    mService = null;
                }
            }
        }
    }
}