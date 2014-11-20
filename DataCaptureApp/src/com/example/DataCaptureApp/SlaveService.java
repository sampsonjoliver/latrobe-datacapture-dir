package com.example.DataCaptureApp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.example.DataCaptureApp.data.*;
import com.example.DataCaptureApp.services.BluetoothConnectivityService;
import com.example.DataCaptureApp.services.SensorSampleService;
import com.example.DataCaptureApp.transforms.ArraySplitDataTransform;

/**
 * Created by Tom on 21/09/2014.
 */
public class SlaveService extends DataService
{
    private static final int NOTIF_ID = 2;
    private static final String NOTIF_TITLE = "Data Collection Slave";
    private static boolean mRunning = false;

    private Data mSensorConfig;
    private boolean mIsSampling = false;

    private NotificationManager mNotificationMgr;
    private Notification mNotification;


    private BluetoothConnectivityService mBluetoothService;
    private DataServiceConnection mBluetoothServiceConn = new DataServiceConnection(BluetoothConnectivityService.class);


    private SensorSampleService mSensorService;
    private DataServiceConnection mSensorServiceConn = new DataServiceConnection(SensorSampleService.class);

    @Override
    public boolean onUnbind(Intent intent)
    {
        logd("Unbinding");
        setDataListener(this);
        setEventListener(this);
        return true;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        logd("Created");

        mRunning = true;

        mNotificationMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        mNotification = buildNotification("Running...", R.drawable.ic_action_refresh_w);
    }

    @Override
    public void onDestroy()
    {
        mRunning = false;
    }

    @Override
    protected void doStart()
    {
        bindServices();
    }

    @Override
    protected void doStop()
    {
        if(mState == State.STARTED && mBluetoothService != null)
            mBluetoothService.write(Event.SLAVE_STOPPED);
        unbindServices();
        mRunning = false;
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        super.onData(source, data);
        if(source == mBluetoothService)
        {
            handleBluetoothData(data);
        }
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        if(source == this)
        {
            handleSelfEvent(event, arg);
        }
        if(event == Event.FAILED)
        {
            failed(arg.toString());
        }
        else if(event == Event.STOPPING)
        {
            if(mState != mState.STOPPING)
                stop();
        }
        else if(event == Event.SERVICE_AVAILABLE)
        {
            if(source == mBluetoothServiceConn)
            {
                handleBluetoothService((BluetoothConnectivityService)arg);
            }
            else if(source == mSensorServiceConn)
            {
                handleSensorService((SensorSampleService)arg);
            }
        }
        if(source == mBluetoothService)
        {
            handleBluetoothEvent(event);
        }
        else if(source == mSensorService)
        {
            handleSensorEvent(event);
        }
    }

    @Override
    public void setEventListener(IEventListener listener)
    {
        super.setEventListener(listener);
        if(listener == this && mRunning)
        {
            startForeground(NOTIF_ID, mNotification);
        }
        else
        {
            stopForeground(true);
        }
    }

    public static boolean isRunning()
    {
        return mRunning;
    }

    private void bindServices()
    {
        mBluetoothServiceConn.bind(this, this);
    }

    private void unbindServices()
    {
        if(mBluetoothService != null)
        {
            mBluetoothService.stop();
            mBluetoothServiceConn.unbind();
            mBluetoothService = null;
        }
        if(mSensorService != null)
        {
            mSensorService.stop();
            mSensorServiceConn.unbind();
            mSensorService = null;
        }
    }

    private void handleBluetoothService(BluetoothConnectivityService service)
    {
        mBluetoothService = service;
        mBluetoothService.setDataListener(this);
        mBluetoothService.setEventListener(this);
        Data config = new Data("role", false);
        try
        {
            mBluetoothService.start(config);
        } catch (FailedInitialisationException e)
        {
            e.printStackTrace();
            failed("Bluetooth Service failed to initialise!");
        }
    }

    public boolean isSampling()
    {
        return mIsSampling;
    }

    private void handleBluetoothEvent(Event event)
    {
        switch(event)
        {
            case ACTION_START:
                mSensorService.startSampling();
                mIsSampling = true;
                if(mEventListener != null)
                    mEventListener.onEvent(this, event, null);
                break;
            case ACTION_STOP:
                mSensorService.stopSampling();
                mIsSampling = false;
                if(mEventListener != null)
                    mEventListener.onEvent(this, event, null);
                break;
        }
    }

    private void handleBluetoothData(Data d)
    {
        if(mState == State.STARTING)
        {
            // Only data at this point should be the slave config
            mSensorConfig = d;
            // Handle connection of sensor service (as it depends on Bluetooth service existing)
            mSensorServiceConn.bind(this, this);
        }
        else
        {
            // What? Only data should be the sensor config...
            logd("Non-config data received!");
        }
    }

    private void handleSensorService(SensorSampleService service)
    {
        mSensorService = service;
        if(mBluetoothService == null)
        {
            failed("Bluetooth Service not available for starting Sensor Sample service");
            return;
        }
        mSensorService.setDataListener(mBluetoothService);
        mSensorService.setEventListener(this);
        if(mSensorConfig != null) // Only startSampling if config has been received from Bluetooth already!
        {
            try
            {
                mSensorService.start(mSensorConfig);
            }
            catch(FailedInitialisationException e)
            {
                failed("Sensor Service failed to initialise! " + e.getMessage());
            }
        }
    }

    private void handleSensorEvent(Event event)
    {
        switch(event)
        {
            case STARTED:
                mBluetoothService.write(Event.SLAVE_READY);
                prepareDataPipeline();
                changeState(State.STARTED);
                break;
        }
    }

    private void handleSelfEvent(Event event, Object arg)
    {
        String newText = null;
        int resId = R.drawable.ic_action_refresh_w;
        switch(event)
        {
            case STARTING:
                newText = "Starting...";
                break;
            case STARTED:
                newText = "Connected";
                break;
            case STOPPING:
                newText = "Stopped";
                break;
            case FAILED:
                String reason = arg == null ? "Unknown reason" : arg.toString();
                newText = "Failed: " + arg.toString();
                resId = R.drawable.ic_action_warning_w;
                break;
            case ACTION_START:
                newText = "Sensor sampling in progress";
                break;
            case ACTION_STOP:
                newText = "Sensor sampling paused";
                break;
        }
        if(newText != null)
        {
            mNotification = buildNotification(newText, resId);
            mNotificationMgr.notify(NOTIF_ID, mNotification);
        }
    }

    private Notification buildNotification(String newText, int iconResId)
    {
        Intent notifIntent = new Intent(this, SlaveActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notifIntent, 0);

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(NOTIF_TITLE)
                .setContentText(newText)
                .setSmallIcon(iconResId)
                .setContentIntent(contentIntent);

        return builder.getNotification();
    }

    private void prepareDataPipeline()
    {
        String[] sensorKeys = mSensorConfig.get("sensorKeys");
        String[] splitKeys = { "X", "Y", "Z", "W" };
        DataTransform[] transforms = new DataTransform[sensorKeys.length];
        for(int i = 0; i < sensorKeys.length; ++i)
        {
            transforms[i] = new ArraySplitDataTransform(sensorKeys[i], splitKeys, true);
            if(i > 0)
                transforms[i-1].setDataListener(transforms[i]);
        }
        mSensorService.setDataListener(transforms[0]);
        transforms[transforms.length-1].setDataListener(mBluetoothService);
    }
}
