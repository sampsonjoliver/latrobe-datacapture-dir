package com.example.DataCaptureApp.testing;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.example.DataCaptureApp.*;
import com.example.DataCaptureApp.data.*;
import com.example.DataCaptureApp.services.BluetoothConnectivityService;
import com.example.DataCaptureApp.services.DataStoreService;
import com.example.DataCaptureApp.services.RemoteConnectivityService;
import com.example.DataCaptureApp.services.SensorSampleService;

import java.util.Date;

/**
 * Created by Tom on 23/09/2014.
 */
public class MasterTestActivity extends Activity implements IDataEventListener, IEventSource
{
    private TextView mStatus;
    private TextView mResults;
    private TextView mTimestamps;

    private Data mConfig;
    private Data mCurrentData;
    private MasterService mService;
    public ServiceConnection mServiceConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder)
        {
            mService = (MasterService)((DataService.LocalBinder)binder).getService();
            mService.setDataListener(MasterTestActivity.this);
            mService.setEventListener(MasterTestActivity.this);
            try
            {
                mService.start(mConfig);
            }
            catch(FailedInitialisationException e)
            {
                setStatus("Failed to startSampling Master Service!");
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
        setContentView(R.layout.test_master);

        mStatus = (TextView)findViewById(R.id.textStatus);
        mResults = (TextView)findViewById(R.id.textResults);
        mTimestamps = (TextView)findViewById(R.id.textTimestamps);

        // Define config
        mConfig = new Data();
        // Bluetooth config
        mConfig.set(BluetoothConnectivityService.CONFIG_ROLE, true);
            //mConfig.set(BluetoothConnectivityService.CONFIG_SLAVE_MAC, "00:90:64:44:57:90"); // LG P990 Bluetooth address!
        mConfig.set(BluetoothConnectivityService.CONFIG_SLAVE_MAC, "E8:99:C4:2F:4D:79"); // HTC One Bluetooth Address!
        // Remote config
        mConfig.set(RemoteConnectivityService.CONFIG_URL, "http://www.tomwwright.com/dir");
        mConfig.set(RemoteConnectivityService.CONFIG_ID_KEY, MasterService.KEY_SESSION);
        mConfig.set(RemoteConnectivityService.CONFIG_HANDLE_TYPE, MasterService.HANDLE_TYPE);
        // Sensor config
        String[] sensorKeys = new String[] { "rotData", "accData", "gyroData", "magData"};
        int[] sensorTypes = new int[] { Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD};
        mConfig.set(SensorSampleService.CONFIG_SAMPLE_RATE, 5);
        mConfig.set(SensorSampleService.CONFIG_SENSOR_KEYS, sensorKeys);
        mConfig.set(SensorSampleService.CONFIG_SENSOR_TYPES, sensorTypes);
        mConfig.set(SensorSampleService.CONFIG_TIMESTAMP_FIELD, MasterService.KEY_TIMESTAMP);
        // Data Store config
        mConfig.set(DataStoreService.CONFIG_TIMESTAMP_FIELD, MasterService.KEY_TIMESTAMP);
        mConfig.set(DataStoreService.CONFIG_SESSION_FIELD, MasterService.KEY_SESSION);
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        if(source == mService)
        {
            mCurrentData = data;
            setResults("Data [knee=" + data.get(MasterService.KEY_ORIENTATION_DIFF));
        }
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        Log.d("Activity", source + ": " + event);
        if(event == Event.FAILED)
        {
            Log.d("Activity", "Reason: " + arg.toString());
        }
        setStatus(source + ": " + event);
    }

    public void onButtonStartMaster(View v)
    {
        startService(new Intent(this, MasterService.class));
        bindService(new Intent(this, MasterService.class), mServiceConn, Context.BIND_ABOVE_CLIENT);
    }

    public void onButtonStartSampling(View v)
    {
        Log.d(getClass().getName(), "Starting sampling on slave!");
        mService.onEvent(this, Event.ACTION_START, null);
    }

    public void onButtonLogTimestamp(View v)
    {
        Long timestamp = (mCurrentData == null) ? null : (Long)mCurrentData.get(MasterService.KEY_TIMESTAMP);
        if(timestamp != null)
        {
            String timestampStr = new Date((long)timestamp).toString();
            appendTimestamps(timestampStr);
        }
    }

    public void setStatus(final String status)
    {
        this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                mStatus.setText(status);
            }
        });
    }

    public void appendResults(final String result)
    {
        this.runOnUiThread(new Runnable() {
            public void run()
            {
                mResults.append(result + '\n');
            }
        });
    }

    public void appendTimestamps(final String timestamp)
    {
        this.runOnUiThread(new Runnable() {
            public void run()
            {
                mTimestamps.append('\n' + timestamp);
            }
        });
    }

    public void setResults(final String result)
    {
        this.runOnUiThread(new Runnable() {
            public void run()
            {
                mResults.setText(result);
            }
        });
    }

    @Override
    public void setEventListener(IEventListener listener)
    {
        // Unused
    }
}