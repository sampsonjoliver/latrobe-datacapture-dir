package com.example.DataCaptureApp.services;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import com.example.DataCaptureApp.utils.ByteUtils;
import com.example.DataCaptureApp.data.DataService;
import com.example.DataCaptureApp.data.FailedInitialisationException;
import com.example.DataCaptureApp.utils.SerialisationUtils;
import com.example.DataCaptureApp.data.*;
import com.example.DataCaptureApp.transforms.DeserialiseDataTransform;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Tom on 6/09/2014.
 */
public class BluetoothConnectivityService extends DataService
{
    public static final String CONFIG_ROLE = "role";
    public static final String CONFIG_SLAVE_MAC = "slaveMac";

    public static final String SERVICE_NAME = "BluetoothConnectivityService";
    public static final String UUID_STRING = "96c2cf70-359b-11e4-8c21-0800200c9a66";
    public static final int TIME_SYNC_TRIPS = 8;

    private BluetoothAdapter mAdapter;
    private BluetoothThread mBluetoothThread;
    private DeserialiseDataTransform mDataTransform = new DeserialiseDataTransform();
    private boolean mIsMaster;
    private boolean mThreadStopExpected = false;

    private long mLatency;
    private long mTimeOffset;
    private int mTimeSyncs = 0;
    private long[] mMasterTimestamps = new long[TIME_SYNC_TRIPS+1];
    private long[] mSlaveTimestamps = new long[TIME_SYNC_TRIPS];

    @Override
    protected boolean isValidConfig(Data config)
    {
        if (config == null)
            return false;
        if (!config.contains(CONFIG_ROLE))
            return false;
        return true;
    }

    @Override
    protected void doStart() throws FailedInitialisationException
    {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null || !mAdapter.isEnabled())
        {
            mAdapter = null;
            throw new FailedInitialisationException("Bluetooth unavailable!");
        }
        mIsMaster = mConfig.get(CONFIG_ROLE);
        if (mIsMaster)
        {
            String slaveMac = mConfig.get(CONFIG_SLAVE_MAC);
            if (slaveMac == null)
                throw new FailedInitialisationException("Missing '" + CONFIG_SLAVE_MAC + "' string!");
            BluetoothDevice slaveDevice = getDeviceFromMac(slaveMac);
            if (slaveDevice == null)
            {
                throw new FailedInitialisationException("Specified Bluetooth device is not paired!");
            }
            // Create client socket
            UUID uuid = UUID.fromString(UUID_STRING);
            try
            {
                BluetoothSocket socket = slaveDevice.createRfcommSocketToServiceRecord(uuid);
                mBluetoothThread = new BluetoothThread(socket, this);
                mBluetoothThread.start();
            } catch (IOException e)
            {
                e.printStackTrace();
                throw new FailedInitialisationException("Failed to connect bluetooth socket! " + e.getMessage());
            }
        } else
        {
            // Create server socket
            UUID uuid = UUID.fromString(UUID_STRING);
            try
            {
                BluetoothServerSocket socket = mAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, uuid);
                mBluetoothThread = new BluetoothThread(socket, this);
                mBluetoothThread.start();
            } catch (IOException e)
            {
                e.printStackTrace();
                throw new FailedInitialisationException("Failed to listen on bluetooth server socket! " + e.getMessage());
            }
        }
    }

    @Override
    protected void doStop()
    {
        if(mBluetoothThread != null)
        {
            // Going to kill BluetoothThread on purpose, write CLOSING event
            mBluetoothThread.write(Event.CLOSING.ordinal());
            mBluetoothThread.cancel();
            mBluetoothThread = null;
        }
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        super.onData(source, data);
        if(source == mBluetoothThread) // Data from thread goes up to the listener or is timesyncing
        {
            handleThreadData(data);
        }
        else // Other data goes down through bluetooth thread
        {
            write(data);
        }
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        boolean propagateEvent = false;
        if(source == mBluetoothThread)
        {
            propagateEvent = handleThreadEvent(event, arg);
        }
        if(propagateEvent)
        {
            mEventListener.onEvent(this, event, arg);
        }
    }

    private void handleThreadData(Data data)
    {
        // Propagate data to the data listener
        if(mState == State.STARTED && mDataListener != null)
        {
            data = mDataTransform.transform(data);
            if(data != null && mDataListener != null)
                mDataListener.onData(this, data);
        }
        else if(mIsMaster && mState == State.STARTING) // Handle time syncing
        {
            byte[] byteData = data.get("bytes");
            mSlaveTimestamps[mTimeSyncs] = ByteUtils.bytesToLong(byteData, 0);
            ++mTimeSyncs;
            if(mTimeSyncs >= TIME_SYNC_TRIPS)
            {
                // Save last timestamp
                mMasterTimestamps[mTimeSyncs] = System.currentTimeMillis();
                // Calculate delta (round trip time for all
                // where t2 = t1 (assuming no time lost during slave processing
                long minDelta = Long.MAX_VALUE;
                int minDeltaIndex = 0;
                for(int i = 0; i < TIME_SYNC_TRIPS; ++i)
                {
                    long delta = mMasterTimestamps[i+1] - mMasterTimestamps[i];
                    if(delta < minDelta)
                    {
                        minDelta = delta;
                        mLatency = minDelta / 2;
                        minDeltaIndex = i;
                    }
                }
                // Use data points for smallest delta to calculate theta - time offset
                int i = minDeltaIndex;
                mTimeOffset = ((mSlaveTimestamps[i] - mMasterTimestamps[i]) + (mSlaveTimestamps[i] - mMasterTimestamps[i+1])) / 2;
                mBluetoothThread.write(Event.READY.ordinal());
                changeState(State.STARTED);

            }
            else
            {
                long time = System.currentTimeMillis();
                mMasterTimestamps[mTimeSyncs] = time;
                mBluetoothThread.write(Event.TIMESYNC.ordinal());
            }
        }

    }

    /**
     *
     * @param event
     * @param arg
     * @return Returns true if event should be propagated to the listener of the service
     */
    private boolean handleThreadEvent(Event event, Object arg)
    {
        switch (event)
        {
            case CONNECTED: // Thread connection establised, initiate timesyncing if master
                if(mIsMaster)
                {
                    logd("Timesyncing");
                    long time = System.currentTimeMillis();
                    mMasterTimestamps[0] = time;
                    mBluetoothThread.write(Event.TIMESYNC.ordinal());
                }
                break;
            case FAILED: // Thread has failed to connect, arg should contain reason
                failed(arg instanceof String ? (String)arg : "Unknown reason for bluetooth thread failure!");
                break;
            case STOPPING: // Thread has closed, flag determines normal or not!
                if(mThreadStopExpected)
                {
                    stop();
                }
                else
                {
                    // Unexpected, this is a fail
                    failed("Bluetooth connection closed unexpectedly");
                }
                break;
            case CLOSING: // Other end of connection is being closed normally, be cool
                mThreadStopExpected = true;
                break;
            case READY:
                changeState(State.STARTED);
                break;
            case TIMESYNC: // Other end of connection wants to timesync, provide!
                byte[] time = ByteUtils.longToBytes(System.currentTimeMillis());
                mBluetoothThread.write(time);
                break;
            default: // Event not related to service-thread interactions, propagate!
                return true;
        }
        return false; // Event must of hit a branch that is NOT default, therefore related to service-thread stuff, don't propagate
    }

    public void write(Data d)
    {
        if(mState == State.STARTED && mBluetoothThread != null)
        {
            byte[] bytes = SerialisationUtils.serialise(d);
            if(bytes == null || bytes.length > BluetoothThread.BYTE_BUFFER_SIZE)
            {
                logd("Null bytes, or bytes exceeding the buffer limit!!");
                return;
            }
            mBluetoothThread.write(bytes);
        }
    }

    public void write(Event e)
    {
        if(mState == State.STARTED && mBluetoothThread != null)
        {
            mBluetoothThread.write((byte)e.ordinal());
        }
    }

    public long getTimeOffset()
    {
        return mTimeOffset;
    }

    public long getLatency()
    {
        return mLatency;
    }

    private BluetoothDevice getDeviceFromMac(String mac)
    {
        Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
        for(BluetoothDevice device : devices)
        {
            if(mac.equals(device.getAddress()))
            {
                return device;
            }
        }
        return null;
    }
}
