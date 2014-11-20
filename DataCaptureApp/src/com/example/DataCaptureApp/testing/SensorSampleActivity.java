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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.example.DataCaptureApp.data.DataService;
import com.example.DataCaptureApp.data.FailedInitialisationException;
import com.example.DataCaptureApp.R;
import com.example.DataCaptureApp.services.SensorSampleService;
import com.example.DataCaptureApp.data.*;

/**
 * Created by Tom on 17/09/2014.
 */
public class SensorSampleActivity extends Activity implements IDataListener, IEventListener
{
    public static final String TAG = "SensorSampleActivity";
    public static final int SOURCE_SERVICE = 0;

    private TextView mTextStatus;
    private EditText mTextRate;
    private EditText mTextEpsilon;
    private Button mStart;
    private Button mStop;
    private TextView mTextResults;

    private boolean mRunning = false;
    private SensorSampleService mService;
    private Data mServiceConfig;

    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = (SensorSampleService)((DataService.LocalBinder)binder).getService();
            Log.d(TAG, "Sensor Service Connected");
            mService.setEventListener(SensorSampleActivity.this);
            mService.setDataListener(SensorSampleActivity.this);
            try
            {
                mService.start(mServiceConfig);
            }
            catch(FailedInitialisationException e)
            {
                setResults("Failed to startSampling Sensor Service!");
                unbindService(this);
                return;
            }
            mService.startSampling();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mService.stopSampling();
            mService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_sample);

        mTextStatus = (TextView)findViewById(R.id.textStatus);
        mStart = (Button)findViewById(R.id.buttonStartSensorService);
        mStop = (Button)findViewById(R.id.buttonStopSensorService);
        mTextResults = (TextView)findViewById(R.id.textSensorData);
        mTextRate = (EditText)findViewById(R.id.textRate);
        mTextEpsilon = (EditText)findViewById(R.id.textEpsilonFraction);

        mServiceConfig = new Data();
        String[] sensorKeys = new String[] { "rotationVector", "accelerometer"};
        int[] sensorTypes = new int[] { Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_ACCELEROMETER};
        mServiceConfig.set("sensorKeys", sensorKeys);
        mServiceConfig.set("sensorTypes", sensorTypes);
    }

    @Override
    public void onPause()
    {
        if(mRunning)
            unbind();
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(mRunning)
            bind();
    }

    public void startSampling(View v)
    {
        Log.d(TAG, "Start Sampling");
        int hz = Integer.parseInt(mTextRate.getText().toString());
        double epsilonFraction = Double.parseDouble(mTextEpsilon.getText().toString());
        mServiceConfig.set("sampleRate", hz);
        mRunning = true;
        bind();
    }

    public void stopSampling(View v)
    {
        Log.d(TAG, "Stop Sampling");
        mRunning = false;
        unbind();
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        setStatus("" + source + " : " + event);
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        Log.d(TAG, "Results for: " + data.get("timestamp"));
        String results = "Got results for timetstamp:  " + data.get("timestamp");
        String[] keys = mServiceConfig.get("sensorKeys");
        for(String key : keys)
        {
            results += "\n contains '" + key + "'";
        }
        results += "\n aggregators: " + (Integer) data.get("aggregators");
        setResults(results);
    }

    private void setStatus(final String status)
    {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                mTextStatus.setText(status);
            }
        });
    }

    private void setResults(final String status)
    {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                mTextResults.setText(status);
            }
        });
    }

    private void unbind()
    {
        if(mService != null)
        {
            mService.stopSampling();
            unbindService(mServiceConn);
            mService = null;
        }
    }

    private void bind()
    {
        bindService(new Intent(this, SensorSampleService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    }
}