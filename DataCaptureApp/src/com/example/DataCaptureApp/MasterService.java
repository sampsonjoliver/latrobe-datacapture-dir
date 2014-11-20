package com.example.DataCaptureApp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.HandlerThread;
import com.example.DataCaptureApp.data.*;
import com.example.DataCaptureApp.services.BluetoothConnectivityService;
import com.example.DataCaptureApp.services.DataStoreService;
import com.example.DataCaptureApp.services.RemoteConnectivityService;
import com.example.DataCaptureApp.services.SensorSampleService;
import com.example.DataCaptureApp.transforms.*;
import com.example.DataCaptureApp.utils.BroadcastDataSource;
import com.example.DataCaptureApp.utils.DataEventHandler;

/**
 * Created by Tom on 21/09/2014.
 */
public class MasterService extends DataService
{
    private static final String NOTIF_TITLE = "Data Capture Master";
    private static final int NOTIF_ID = 5;
    public static final String KEY_SLAVE_PACK = "slave";
    public static final String KEY_MASTER_PACK = "master";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_SESSION = "session";
    public static final String KEY_USER = "user";
    public static final String KEY_ARRAY_COLLECT = "datapoint";
    public static final String KEY_TIMESTAMP_COPY = KEY_TIMESTAMP + "Copy";
    public static final String KEY_ORIENTATION_DIFF = "calcOrientationDistance";
    public static final String KEY_SENSOR_ROTATION = "rotData";
    public static final String KEY_SENSOR_MAGNETOMETER = "magData";
    public static final String KEY_SENSOR_ACCELEROMETER = "accData";
    public static final String KEY_SENSOR_GYROSCOPE = "gyroData";
    public static final String KEY_EVENT_FIELD = "dataField";
    public static final String KEY_EVENT_LABEL = "label";
    public static final String KEY_EVENT_TYPE = "constraintType";
    public static final String KEY_EVENT_VALUE = "constraintValue";
    public static final String KEY_EVENT_SEVERITY = "severity";

    public static final String CONFIG_ARRAY_COLLECT_COUNT = "arrayCollectCount";
    public static final String CONFIG_USER_ID = KEY_USER;
    public static final String CONFIG_SESSION_ID = "sessionId";
    public static final String CONFIG_EVENT_DATA = "event";

    public static final String HANDLE_TYPE = "physio";
    public static final String THREAD_NAME = "dataThread";

    private static boolean mRunning = false;

    private DataStoreService mDataStoreService;
    private DataServiceConnection mDataStoreServiceConn = new DataServiceConnection(DataStoreService.class);

    private BluetoothConnectivityService mBluetoothService;
    private DataServiceConnection mBluetoothServiceConn = new DataServiceConnection(BluetoothConnectivityService.class);

    private SensorSampleService mSensorService;
    private DataServiceConnection mSensorServiceConn = new DataServiceConnection(SensorSampleService.class);

    private RemoteConnectivityService mRemoteService;
    private DataServiceConnection mRemoteServiceConn = new DataServiceConnection(RemoteConnectivityService.class);

    private IntervalAggregatorDataTransform mAggregator;
    private BroadcastDataSource mBroadcastSource = new BroadcastDataSource();
    private ArrayCollectDataTransform mArrayCollector;
    private long mStartTime;
    private long mFinishTime;
    private String mSessionId;
    private String mUserId;
    private int mArrayCollectCount;
    private boolean mIsSampling = false;
    private Data mEventData;


    private HandlerThread mThread;
    private DataEventHandler mSlaveToThreadHandler;
    private DataEventHandler mMasterToThreadHandler;
    private DataEventHandler mPersistFromThreadHandler;
    private DataEventHandler mRemoteFromThreadHandler;

    private NotificationManager mNotificationMgr;
    private Notification mNotification;



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
        // Log config data
        logd("Configuration: " + mConfig.toJson());
        mArrayCollectCount = mConfig.get(CONFIG_ARRAY_COLLECT_COUNT);
        mUserId = mConfig.get(CONFIG_USER_ID);
        mSessionId = mConfig.get(CONFIG_SESSION_ID);
        mEventData = mConfig.get(CONFIG_EVENT_DATA);
        mThread = new HandlerThread(THREAD_NAME);
        mThread.start();
    }

    @Override
    public void stop()
    {
        mRunning = false;
        super.stop();
    }

    @Override
    protected void doStop()
    {
        mRunning = false;
        // Flush remaining data in array collector (if remote is active)
        if(mRemoteService != null && mArrayCollector != null && mRemoteService.getState() == State.STARTED)
        {
            if (mArrayCollector != null && !mArrayCollector.isEmpty())
                mArrayCollector.flush();
            if(mDataStoreService != null)
            {
                mDataStoreService.delete(mSessionId, System.currentTimeMillis());
            }
        }
        // Stop all active services
        if(mDataStoreService != null)
            mDataStoreService.stop();
        if(mBluetoothService != null)
            mBluetoothService.stop();
        if(mSensorService != null)
            mSensorService.stop();
        if(mRemoteService != null)
            mRemoteService.stop();
        // Stop the helper thread
        if(mThread != null && mThread.isAlive())
            mThread.quit();
        // Unbind all services
        unbindServices();
    }

    @Override
    protected boolean isValidConfig(Data config)
    {
        boolean containsArrayCount = config.contains(CONFIG_ARRAY_COLLECT_COUNT, Integer.class);
        boolean containsUserId = config.contains(CONFIG_USER_ID, String.class);
        boolean containsSessionId = config.contains(CONFIG_SESSION_ID, String.class);
        boolean containsEventData = config.contains(CONFIG_EVENT_DATA, Data.class);
        if(!containsArrayCount || !containsUserId || !containsSessionId || !containsEventData)
            return false;
        int arrayCount = config.get(CONFIG_ARRAY_COLLECT_COUNT);
        if(arrayCount < 1)
            return false;
        return true;
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        super.onData(source, data);
        if(source == mPersistFromThreadHandler)
        {
            if(mDataListener != null)
                mDataListener.onData(this, data);
        }
        else if(source == mRemoteFromThreadHandler)
        {
            logd("Data from RemoteService");
            Data originalData = data.get(RemoteConnectivityService.KEY_RESPONSE_DATA);
            Object[] datapoints = originalData.get(KEY_ARRAY_COLLECT);
            long minTime = Long.MIN_VALUE;
            long maxTime = Long.MAX_VALUE;
            for(Object datapoint : datapoints)
            {
                long timestamp = ((Data)datapoint).get(KEY_TIMESTAMP);
                if(timestamp < minTime)
                    minTime = timestamp;
                if(timestamp > maxTime)
                    maxTime = timestamp;
            }
            mDataStoreService.delete(mSessionId, minTime, maxTime);
        }
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        if(source == this)
            handleSelfEvent(event, arg);
        else if(source == mBluetoothService)
            handleBluetoothEvent(event, arg);
        else if(source == mEventListener)
            handleListenerEvent(event, arg);
        else if(source == mRemoteService || source == mSensorService || source == mDataStoreService)
        {
            switch(event)
            {
                case FAILED:
                    failed((arg == null) ? "Unknown" : arg.toString());
                    break;
                case STOPPING:
                    break;
                case STARTED:
                    if(isAllStarted())
                        changeState(State.STARTED);
            }
        }
        else if(source == mBluetoothServiceConn)
            handleBluetoothService((BluetoothConnectivityService)arg);
        else if(source == mSensorServiceConn)
            handleSensorService((SensorSampleService) arg);
        else if(source == mDataStoreServiceConn)
            handleDataStoreService((DataStoreService) arg);
        else if(source == mRemoteServiceConn)
            handleRemoteService((RemoteConnectivityService) arg);
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

    private void startSampling()
    {
        if(mState == State.STARTED)
        {
            mStartTime = System.currentTimeMillis();
            prepareDataPipeline();
            mSensorService.startSampling();
            mBluetoothService.write(Event.ACTION_START);
            mIsSampling = true;
        }
    }

    private void stopSampling()
    {
        if(mState == State.STARTED && mIsSampling)
        {
            mFinishTime = System.currentTimeMillis();
            mSensorService.stopSampling();
            mBluetoothService.write(Event.ACTION_STOP);
            mIsSampling = false;
        }
    }

    public boolean isSampling()
    {
        return mIsSampling;
    }

    public static boolean isRunning()
    {
        return mRunning;
    }

    private void bindServices()
    {
        mDataStoreServiceConn.bind(this, this);
        mBluetoothServiceConn.bind(this, this);
        mSensorServiceConn.bind(this, this);
        mRemoteServiceConn.bind(this, this);
    }

    private void unbindServices()
    {
        mDataStoreServiceConn.unbind();
        mBluetoothServiceConn.unbind();
        mSensorServiceConn.unbind();
        mRemoteServiceConn.unbind();
    }

    private void handleDataStoreService(DataStoreService service)
    {
        mDataStoreService = service;
        mDataStoreService.setDataListener(MasterService.this);
        mDataStoreService.setEventListener(MasterService.this);
        try
        {
            mDataStoreService.start(mConfig);
        }
        catch(FailedInitialisationException e)
        {
            failed("Failed to start DataStoreService! " + e.getMessage());
        }
    }

    private void handleSensorService(SensorSampleService service)
    {
        mSensorService = service;
        mSensorService.setDataListener(MasterService.this);
        mSensorService.setEventListener(MasterService.this);
        try
        {
            mSensorService.start(mConfig);
        } catch (FailedInitialisationException e)
        {
            failed("SensorService failed to initialise: " + e.getMessage());
        }
    }

    private void handleBluetoothService(BluetoothConnectivityService service)
    {
        mBluetoothService = service;
        mBluetoothService.setDataListener(MasterService.this);
        mBluetoothService.setEventListener(MasterService.this);
        try
        {
            mBluetoothService.start(mConfig);
        }
        catch(FailedInitialisationException e)
        {
            failed("BluetoothService failed to initialise: " + e.getMessage());
        }
    }

    private void handleRemoteService(RemoteConnectivityService service)
    {
        mRemoteService = service;
        mRemoteService.setDataListener(this);
        mRemoteService.setEventListener(this);
        try
        {
            mRemoteService.start(mConfig);
        }
        catch(FailedInitialisationException e)
        {
            failed("RemoteConnectivityService failed to initialise: " + e.getMessage());
        }
    }

    private void handleBluetoothEvent(Event event, Object arg)
    {
        switch(event)
        {
            case FAILED:
                // Bluetooth connection has closed, service has failed
                failed(arg == null ? "Unknown Bluetooth error!" : arg.toString());
                break;
            case STARTED:
                // Bluetooth connection established, send slave config
                mBluetoothService.write(mConfig);
                break;
            case STOPPING:
                // Bluetooth connection stopped
                if(mState != State.STOPPING)
                    stop();
                break;
            case SLAVE_READY:
                // Slave service has started correctly
                if(isAllStarted())
                    changeState(State.STARTED);
                break;
            case SLAVE_STOPPED:
                // Slave service is stopping correctly
                logd("Slave service stopping!");
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
                newText = "Failed: " + (arg == null ? "Unknown reason" : arg.toString());
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

    private void handleListenerEvent(Event event, Object arg)
    {
        switch(event)
        {
            case ACTION_START:
                startSampling();
                break;
            case ACTION_STOP:
                stopSampling();
                break;
        }
    }

    private Notification buildNotification(String newText, int iconResId)
    {
        Intent notifIntent = new Intent(this, MasterActivity.class);
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
        // Slave pipeline from bluetooth to aggregation
        ArithmeticDataTransform timestampCorrector = new ArithmeticDataTransform(KEY_TIMESTAMP, mBluetoothService.getTimeOffset(), ArithmeticDataTransform.Operation.Subtract);
        FieldCopyDataTransform slaveTimestampCopy = new FieldCopyDataTransform(KEY_TIMESTAMP, KEY_TIMESTAMP_COPY);
        String[] timestampCopyField = { KEY_TIMESTAMP_COPY };
        FieldModifyDataTransform slavePrepend = new FieldModifyDataTransform(KEY_SLAVE_PACK, timestampCopyField, true, true);
        String[] timestampField = { KEY_TIMESTAMP };
        PackDataTransform slavePack = new PackDataTransform(KEY_SLAVE_PACK, timestampCopyField);
        FieldRenameDataTransform slaveTimestampRestore = new FieldRenameDataTransform(timestampCopyField, timestampField);

        // Create array splitters for master sensor data
        String[] sensorKeys = mConfig.get(SensorSampleService.CONFIG_SENSOR_KEYS);
        String[] splitKeys = { "X", "Y", "Z", "W" };
        DataTransform[] arraySplits = new DataTransform[sensorKeys.length];
        for(int i = 0; i < sensorKeys.length; ++i)
            arraySplits[i] = new ArraySplitDataTransform(sensorKeys[i], splitKeys, true);
        // Master pipeline from array splits to aggregation
        FieldCopyDataTransform masterTimestampCopy = new FieldCopyDataTransform(slaveTimestampCopy);
        FieldModifyDataTransform masterPrepend = new FieldModifyDataTransform(KEY_MASTER_PACK, timestampCopyField, true, true);
        PackDataTransform masterPack = new PackDataTransform(KEY_MASTER_PACK, timestampCopyField);
        FieldRenameDataTransform masterTimestampRestore = new FieldRenameDataTransform(slaveTimestampRestore);

        // Aggregator
        String[] aggregatorKeys = {KEY_SLAVE_PACK, KEY_MASTER_PACK};
        int intervalTime = 1000 / (Integer)mConfig.get(SensorSampleService.CONFIG_SAMPLE_RATE); // 1000ms divided by the sampleRate = intervalTime
        mAggregator = new IntervalAggregatorDataTransform(KEY_TIMESTAMP, intervalTime, mStartTime, aggregatorKeys);

        // Pipeline after aggregation until after data persistence
        UnpackDataTransform slaveUnpack = new UnpackDataTransform(KEY_SLAVE_PACK);
        UnpackDataTransform masterUnpack = new UnpackDataTransform(KEY_MASTER_PACK);
        SetDataTransform setSessionId = new SetDataTransform(KEY_SESSION, mSessionId);
        String[] masterFields = { "masterRotDataX", "masterRotDataY", "masterRotDataZ", "masterRotDataW"};
        String[] slaveFields = { "slaveRotDataX", "slaveRotDataY", "slaveRotDataZ", "slaveRotDataW" };
        String[] differenceFields = { "calcDiffOrientationX", "calcDiffOrientationY", "calcDiffOrientationZ", "calcDiffOrientationW"};

        QuaternionDifferenceDataTransform kneeAngle = new QuaternionDifferenceDataTransform(masterFields, slaveFields, differenceFields, KEY_ORIENTATION_DIFF);
        RemoveDataTransform removeSessionId = new RemoveDataTransform(KEY_SESSION);
        SetDataTransform setSessionId2 = new SetDataTransform(KEY_SESSION, mSessionId);
        mArrayCollector = new ArrayCollectDataTransform(mArrayCollectCount, KEY_ARRAY_COLLECT);
        SetDataTransform setUserId = new SetDataTransform(KEY_USER, mUserId);
        Data[] events = new Data[] { mEventData };
        SetDataTransform setEventData = new SetDataTransform(CONFIG_EVENT_DATA, events);
        // Create the broadcast data source
        mBroadcastSource = new BroadcastDataSource();

        // Thread and handler to handle data as soon as possible (using handler as 'source' - because why not)
        mSlaveToThreadHandler = new DataEventHandler(null, mThread.getLooper());
        mMasterToThreadHandler = new DataEventHandler(null, mThread.getLooper());
        // Handler to allow data to return to UI thread at completion of pipeline
        mPersistFromThreadHandler = new DataEventHandler(null, getMainLooper());
        mRemoteFromThreadHandler = new DataEventHandler(null, getMainLooper());

        // Set the pipeline - slave until aggregator
        mBluetoothService.setDataListener(mSlaveToThreadHandler);
        mSlaveToThreadHandler.setDataListener(timestampCorrector);
        DataTransform.pipeline(timestampCorrector, slaveTimestampCopy, slavePrepend, slavePack, slaveTimestampRestore, mAggregator);

        // Master until aggregator
        mSensorService.setDataListener(mMasterToThreadHandler);
        mMasterToThreadHandler.setDataListener(arraySplits[0]);
        DataTransform.pipeline(arraySplits);
        DataTransform.pipeline(arraySplits[arraySplits.length-1], masterTimestampCopy, masterPrepend, masterPack, masterTimestampRestore, mAggregator);

        // Aggregator to broadcast to master (via handler attached back to main thread)
        DataTransform.pipeline(mAggregator, slaveUnpack, masterUnpack, setSessionId, kneeAngle);
        kneeAngle.setDataListener(mDataStoreService);
        mDataStoreService.setDataListener(mBroadcastSource);
        mBroadcastSource.setDataListener(mPersistFromThreadHandler);
        // Broadcast to master via remoteService
        mBroadcastSource.setDataListener(removeSessionId);
        DataTransform.pipeline(removeSessionId, mArrayCollector, setSessionId2, setUserId, setEventData);
        setEventData.setDataListener(mRemoteService);
        mRemoteService.setDataListener(mRemoteFromThreadHandler);

        mPersistFromThreadHandler.setDataListener(this);
        mRemoteFromThreadHandler.setDataListener(this);
    }

    private boolean isAllStarted()
    {
        boolean sensorStarted = mSensorService != null && mSensorService.getState() == State.STARTED;
        boolean remoteStarted = mRemoteService != null && mRemoteService.getState() == State.STARTED;
        boolean dataStoreStarted = mDataStoreService != null && mDataStoreService.getState() == State.STARTED;
        boolean bluetoothStarted = mBluetoothService != null && mBluetoothService.getState() == State.STARTED;
        return sensorStarted && remoteStarted && dataStoreStarted && bluetoothStarted;
    }
}
