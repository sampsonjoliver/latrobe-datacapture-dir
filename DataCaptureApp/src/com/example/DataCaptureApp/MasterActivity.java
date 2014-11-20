package com.example.DataCaptureApp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.DataCaptureApp.data.*;
import com.example.DataCaptureApp.services.BluetoothConnectivityService;
import com.example.DataCaptureApp.services.DataStoreService;
import com.example.DataCaptureApp.services.RemoteConnectivityService;
import com.example.DataCaptureApp.services.SensorSampleService;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Tom on 5/11/2014.
 */
public class MasterActivity extends Activity implements IEventListener, IEventSource, IDataListener
{
    private Button mStart;
    private Button mStop;
    private Button mSampling;
    private View mProgress;
    private LinearLayout mAngleLayout;
    private TextView mAngle;
    private ImageView mImage;
    private TextView mStatus;

    private State mState = State.START;
    private boolean mReconnecting = false;

    private MediaPlayer mSound;
    private Vibrator mVibrator;

    private String mEventLabel;
    private boolean mEventIsLte; // Is event <= ?
    private int mEventAngle;

    private DataServiceConnection mConnection = new DataServiceConnection(MasterService.class);
    private MasterService mService;

    private Data mConfig;

    private enum State
    {
        START,
        LOAD,
        READY,
        SAMPLE,
        FAIL,
        WARN
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getActionBar();
        setContentView(R.layout.master);

        mStart = (Button) findViewById(R.id.buttonStart);
        mStop = (Button) findViewById(R.id.buttonStop);
        mSampling = (Button) findViewById(R.id.buttonSampling);
        mProgress = findViewById(R.id.progressSlave);
        mImage = (ImageView) findViewById(R.id.imageSlave);
        mStatus = (TextView) findViewById(R.id.textStatus);
        mAngleLayout = (LinearLayout) findViewById(R.id.layoutAngle);
        mAngle = (TextView) findViewById(R.id.textAngle);

        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        setStatus("");
        setState(State.START);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        checkState();
        mSound = MediaPlayer.create(this, R.raw.beep);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mConnection.unbind();
        mService = null;
        mSound.stop();
        mSound.release();
        mSound = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.master_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_configure:
                Intent configureIntent = new Intent(this, ConfigActivity.class);
                startActivity(configureIntent);
                return true;
            case R.id.action_bluetooth:
                Intent bluetoothIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(bluetoothIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        if (source == mConnection && event == Event.SERVICE_AVAILABLE)
        {
            mService = (MasterService) mConnection.getService();
            mService.setEventListener(this);
            mService.setDataListener(this);
            if(mReconnecting)
            {
                // Update UI to match
                checkState();
                mReconnecting = false;
            }
            else
            {
                try
                {
                    mService.start(mConfig);
                } catch (FailedInitialisationException e)
                {
                    mConnection.unbind();
                    setStatus("Service failed to start: " + e.getMessage());
                }
            }
        } else if (source == mService)
        {
            String newStatus = null;
            switch (event)
            {
                case STARTING:
                    newStatus = "Connecting...";
                    break;
                case STARTED:
                    newStatus = "Connected to client";
                    setState(State.READY);
                    break;
                case FAILED:
                    String reason = arg != null ? arg.toString() : null;
                    newStatus = "Service failed!\n(" + reason + ")";
                    disconnectService();
                    setState(State.FAIL);
                    break;
                case STOPPING:
                    newStatus = "Connection closed";
                    disconnectService();
                    checkState();
                    break;
                default:
                    break;
            }
            if (newStatus != null)
                setStatus(newStatus);
        }
    }

    @Override
    public void setEventListener(IEventListener listener)
    {
        // Unused
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        float angle = data.get(MasterService.KEY_ORIENTATION_DIFF);
        int intAngle = Math.round(angle);
        setAngle(String.valueOf(intAngle));
        if(!mSound.isPlaying())
        {
            boolean isEvent = mEventIsLte ? intAngle <= mEventAngle : intAngle >= mEventAngle;
            if(isEvent)
            {
                mSound.start();
                mVibrator.vibrate(200);
            }
        }
    }

    public void onButtonStart(View v)
    {
        // Bind MasterService
        if(mService == null)
        {
            Data config = createConfig();
            if(config == null)
            {
                setStatus("Configuration not complete!");
                setState(State.FAIL);
                return;
            }
            mConfig = config;
            Data eventData = mConfig.get(MasterService.CONFIG_EVENT_DATA);
            mEventAngle = eventData.get(MasterService.KEY_EVENT_VALUE);
            mEventLabel = eventData.get(MasterService.KEY_EVENT_LABEL);
            mEventIsLte = eventData.get(MasterService.KEY_EVENT_TYPE).equals("lte");
            // Start the service if not running
            if(!MasterService.isRunning())
            {
                mConnection.start(this);
            }
            mConnection.bind(this, this);
            setStatus("Binding service...");
            setState(State.LOAD);
        }
    }

    public void onButtonStop(View v)
    {
        if (mService != null)
        {
            mService.onEvent(this, Event.ACTION_STOP, null);
            disconnectService();
            checkState();
        }
    }

    public void onButtonStartSampling(View v)
    {
        if(mService != null && mService.getState() == DataService.State.STARTED)
        {
            mService.onEvent(this, Event.ACTION_START, null);
            setStatus("Sampling");
            setState(State.SAMPLE);
        }
    }

    private void checkState()
    {
        if(MasterService.isRunning())
        {
            if(mService == null)
            {
                mReconnecting = true;
                mConnection.bind(this, this);
                setStatus("Binding service...");
                setState(State.LOAD);
            }
            else
            {
                // Just need to change activity to match service state
                switch(mService.getState())
                {
                    case STARTING:
                        setStatus("Service starting...");
                        setState(State.LOAD);
                        break;
                    case STARTED:
                        if(mService.isSampling())
                        {
                            setStatus("Sampling");
                            setState(State.SAMPLE);
                        }
                        else
                        {
                            setStatus("Not sampling");
                            setState(State.READY);
                        }
                        break;
                    case FAILED:
                        setStatus("Service has failed!");
                        setState(State.FAIL);
                        break;
                }
            }
        }
        else
        {
            mConfig = createConfig();
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled())
            {
                setStatus("Bluetooth unavailable!");
                setState(State.WARN);
            } else if (mConfig == null)
            {
                setStatus("Please complete configuration!");
                setState(State.WARN);
            } else
            {
                setStatus("Ready");
                setState(State.START);
            }
        }
    }

    private void setStatus(final String status)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                mStatus.setText(status);
            }
        });
    }

    private void setAngle(final String angle)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                mAngle.setText(angle);
            }
        });
    }

    private void setState(State newState)
    {
        switch(newState)
        {
            case START:
                mImage.setVisibility(View.VISIBLE);
                mImage.setImageResource(R.drawable.ic_action_accept);
                mProgress.setVisibility(View.GONE);
                mAngleLayout.setVisibility(View.GONE);
                mStart.setVisibility(View.VISIBLE);
                mStop.setVisibility(View.GONE);
                mSampling.setVisibility(View.GONE);
                break;
            case LOAD:
                mImage.setVisibility(View.GONE);
                mProgress.setVisibility(View.VISIBLE);
                mAngleLayout.setVisibility(View.GONE);
                mStart.setVisibility(View.GONE);
                mStop.setVisibility(View.VISIBLE);
                mSampling.setVisibility(View.GONE);
                break;
            case READY:
                mImage.setVisibility(View.VISIBLE);
                mImage.setImageResource(R.drawable.ic_action_refresh);
                mProgress.setVisibility(View.GONE);
                mAngleLayout.setVisibility(View.GONE);
                mStart.setVisibility(View.GONE);
                mStop.setVisibility(View.VISIBLE);
                mSampling.setVisibility(View.VISIBLE);
                break;
            case SAMPLE:
                mImage.setVisibility(View.VISIBLE);
                mImage.setImageResource(R.drawable.ic_action_refresh);
                mProgress.setVisibility(View.GONE);
                mAngleLayout.setVisibility(View.VISIBLE);
                mStart.setVisibility(View.GONE);
                mStop.setVisibility(View.VISIBLE);
                mSampling.setVisibility(View.GONE);
                break;
            case FAIL:
                mImage.setVisibility(View.VISIBLE);
                mImage.setImageResource(R.drawable.ic_action_warning);
                mProgress.setVisibility(View.GONE);
                mAngleLayout.setVisibility(View.GONE);
                mStart.setVisibility(View.VISIBLE);
                mStop.setVisibility(View.GONE);
                mSampling.setVisibility(View.GONE);
                break;
            case WARN:
                mImage.setVisibility(View.VISIBLE);
                mImage.setImageResource(R.drawable.ic_action_settings);
                mProgress.setVisibility(View.GONE);
                mAngleLayout.setVisibility(View.GONE);
                mStart.setVisibility(View.GONE);
                mStop.setVisibility(View.GONE);
                mSampling.setVisibility(View.GONE);
                break;
        }
        mState = newState;
    }

    private void disconnectService()
    {
        if(mService != null)
        {
            mService.stop();
            mConnection.unbind();
            mService = null;
        }
    }

    private Data createConfig()
    {
        Data config = new Data();
        SharedPreferences prefs = getSharedPreferences(ConfigActivity.PREF_NAME, MODE_PRIVATE);
        // Session configuration
        String userId = prefs.getString(getString(R.string.pref_user), "");
        String eventType = prefs.getString(getString(R.string.pref_event_type), null);
        int eventThreshold = Integer.parseInt(prefs.getString(getString(R.string.pref_event_angle), "-1"));
        if(userId.length() <= 0 || eventType == null || eventThreshold < 0 || eventThreshold > 180)
            return null;
        String sessionId = UUID.randomUUID().toString(); // Generate random UUID
        config.set(MasterService.CONFIG_USER_ID, userId);
        config.set(MasterService.CONFIG_SESSION_ID, sessionId);
        Data eventData = new Data();
        eventData.set(MasterService.KEY_EVENT_FIELD, MasterService.KEY_ORIENTATION_DIFF);
        eventData.set(MasterService.KEY_EVENT_LABEL, "Angle Difference");
        eventData.set(MasterService.KEY_EVENT_TYPE, eventType);
        eventData.set(MasterService.KEY_EVENT_VALUE, new Integer(eventThreshold));
        eventData.set(MasterService.KEY_EVENT_SEVERITY, "9");
        config.set(MasterService.CONFIG_EVENT_DATA, eventData);
        // Bluetooth configuration
        String slaveMac = prefs.getString(getString(R.string.pref_bluetooth), "");
        if(slaveMac == null || slaveMac.length() <= 0)
            return null;
        config.set(BluetoothConnectivityService.CONFIG_SLAVE_MAC, slaveMac);
        config.set(BluetoothConnectivityService.CONFIG_ROLE, true); // Define as master
        // Remote configuration
        String url = prefs.getString(getString(R.string.pref_remote_url), "");
        if(url == null || url.length() <= 0)
            return null;
        config.set(RemoteConnectivityService.CONFIG_URL, url);
        config.set(RemoteConnectivityService.CONFIG_ID_KEY, MasterService.KEY_SESSION);
        config.set(RemoteConnectivityService.CONFIG_HANDLE_TYPE, MasterService.HANDLE_TYPE);
        int collectCount = Integer.parseInt(prefs.getString(getString(R.string.pref_remote_packet_size), "-1"));
        if(collectCount < 1)
            collectCount = 50; // Default value
        config.set(MasterService.CONFIG_ARRAY_COLLECT_COUNT, collectCount);
        // Sensor configuration
        int sampleRate = Integer.parseInt(prefs.getString(getString(R.string.pref_samplerate), "-1"));
        if(sampleRate == -1)
            return null;
        ArrayList<String> sensorKeys = new ArrayList<String>();
        ArrayList<Integer> sensorTypes = new ArrayList<Integer>();
        sensorKeys.add(MasterService.KEY_SENSOR_ROTATION);
        sensorTypes.add(Sensor.TYPE_ROTATION_VECTOR);
        config.set(SensorSampleService.CONFIG_SAMPLE_RATE, sampleRate);
        config.set(SensorSampleService.CONFIG_TIMESTAMP_FIELD, MasterService.KEY_TIMESTAMP);
        boolean sampleMag = prefs.getBoolean(getString(R.string.pref_sampling_mag), false);
        if(sampleMag)
        {
            sensorKeys.add(MasterService.KEY_SENSOR_MAGNETOMETER);
            sensorTypes.add(Sensor.TYPE_MAGNETIC_FIELD);
        }
        boolean sampleAcc = prefs.getBoolean(getString(R.string.pref_sampling_acc), false);
        if(sampleAcc)
        {
            sensorKeys.add(MasterService.KEY_SENSOR_ACCELEROMETER);
            sensorTypes.add(Sensor.TYPE_ACCELEROMETER);
        }
        boolean sampleGyro = prefs.getBoolean(getString(R.string.pref_sampling_gyro), false);
        if(sampleGyro)
        {
            sensorKeys.add(MasterService.KEY_SENSOR_GYROSCOPE);
            sensorTypes.add(Sensor.TYPE_GYROSCOPE);
        }
        String[] sensorKeysArr = new String[sensorKeys.size()];
        Integer[] sensorTypesArr = new Integer[sensorTypes.size()];
        sensorKeys.toArray(sensorKeysArr);
        sensorTypes.toArray(sensorTypesArr);
        config.set(SensorSampleService.CONFIG_SENSOR_KEYS, sensorKeysArr);
        config.set(SensorSampleService.CONFIG_SENSOR_TYPES, sensorTypesArr);
        // Data Store configuration
        config.set(DataStoreService.CONFIG_TIMESTAMP_FIELD, MasterService.KEY_TIMESTAMP);
        config.set(DataStoreService.CONFIG_SESSION_FIELD, MasterService.KEY_SESSION);
        return config;
    }
}