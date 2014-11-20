package com.example.DataCaptureApp.services;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.example.DataCaptureApp.utils.DataEventHandler;
import com.example.DataCaptureApp.data.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Tom on 6/09/2014.
 */
public class BluetoothThread extends Thread implements IDataSource, IEventSource
{
    public static final int BYTE_MAX_VALUE = 255;
    public static final int BYTE_BUFFER_SIZE = 2048;
    public static final String TAG = "BluetoothThread";
    private BluetoothServerSocket mServerSocket;
    private BluetoothSocket mSocket;
    private IDataEventListener mListener;
    private DataEventHandler mHandler;

    private InputStream mInputStream;
    private OutputStream mOutputStream;

    public BluetoothThread(BluetoothSocket socket, IDataEventListener listener) {
        mSocket = socket;
        mListener = listener;
        mHandler = new DataEventHandler(this, this, mListener, Looper.getMainLooper());
    }

    public BluetoothThread(BluetoothServerSocket serverSocket, IDataEventListener listener)
    {
        mServerSocket = serverSocket;
        mListener = listener;
        mHandler = new DataEventHandler(this, this, mListener, Looper.getMainLooper());
    }

    @Override
    public void run()
    {
        boolean success = false;
        try {
            success = connect();
            // At this point, mSocket should be correctly attached, retrieve streams
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
        } catch (Exception e) {
            success = false;
        }
        if(success)
        {
            mHandler.onEvent(this, Event.CONNECTED, null);
            readInputStream();
        }
        else
        {
            mHandler.onEvent(this, Event.FAILED, "Failed to connect bluetooth socket!");
        }
        Log.d(TAG, "Terminating");
        if(mListener != null)
        {
            mHandler.onEvent(this, Event.STOPPING, null);
        }
    }

    public void cancel()
    {
        try {
            if(mServerSocket != null) {
                mServerSocket.close();
                mServerSocket = null;
            }
            if(mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        } catch (IOException e) {
        }
    }

    private boolean connect() throws IOException
    {
        Log.d(TAG, "Connecting");
        Message msg;
        if(mServerSocket != null)
        {
            // Begin listening
            mSocket = mServerSocket.accept();
        }
        else if (mSocket != null)
        {
            // Attempt connection
            mSocket.connect();
        }
        return (mSocket != null);
    }


    public boolean write(int oneByte)
    {
        if(mOutputStream != null)
        {
            try {
                mOutputStream.write(1);
                mOutputStream.write(oneByte);
                mOutputStream.flush();
                return true;
            } catch(IOException e)
            {
            }
        }
        return false;
    }

    public boolean write(byte[] data)
    {
        if(mOutputStream != null)
        {
            try {
                int len = data.length;
                while(len > BYTE_MAX_VALUE)
                {
                    mOutputStream.write(BYTE_MAX_VALUE);
                    len -= BYTE_MAX_VALUE;
                }
                mOutputStream.write(len);
                mOutputStream.write(data);
                mOutputStream.flush();
                return true;
            } catch(IOException e)
            {
            }
        }
        return false;
    }

    private void readInputStream()
    {
        Log.d(TAG, "Reading");
        byte[] buffer = new byte[BYTE_BUFFER_SIZE];
        int bytes = 0;
        while(true)
        {
            try {
                // Read from the InputStream
                int numBytes = 0;
                int readByte = 0;
                do
                {
                    readByte = mInputStream.read();
                    numBytes += readByte;
                } while(readByte == BYTE_MAX_VALUE);

                bytes = mInputStream.read(buffer, 0, numBytes);
                // Handle bytes
                handleBytes(bytes, buffer);
            } catch (IOException e) {
                break;
            }
        }
    }

    private void handleBytes(int bytes, byte[] buffer)
    {
        if(bytes == 1)
        {
            mHandler.onEvent(this, Event.values[buffer[0]], null);
        }
        else {
            Data data = new Data();
            byte[] dataBytes = new byte[bytes];
            for(int i = 0; i < bytes; ++i)
                dataBytes[i] = buffer[i];
            data.set("bytes", dataBytes);
            mHandler.onData(this, data);
        }
    }

    @Override
    public void setDataListener(IDataListener listener)
    {
        // Unused
    }

    @Override
    public void setEventListener(IEventListener listener)
    {
        // Unused
    }
}
