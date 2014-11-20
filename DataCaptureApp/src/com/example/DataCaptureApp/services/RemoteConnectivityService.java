package com.example.DataCaptureApp.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.example.DataCaptureApp.utils.DataEventHandler;
import com.example.DataCaptureApp.data.DataService;
import com.example.DataCaptureApp.data.FailedInitialisationException;
import com.example.DataCaptureApp.data.*;

/**
 * Created by Tom on 8/10/2014.
 */
public class RemoteConnectivityService extends DataService
{
    public static final int TIMEOUT = 15000;
    public static final int MAX_LEN = 1024;
    public static final String QUERY_PATH = "api/proclaim";
    public static final String SUBMIT_PATH = "api/session";
    public static final String KEY_SERVER_NAME = "serverName";
    public static final String KEY_SERVER_VERSION = "version";
    public static final String KEY_SERVER_HANDLES = "handles";
    public static final String KEY_RESPONSE_STATUS = "status";
    public static final String KEY_RESPONSE_MESSAGE = "message";
    public static final String KEY_RESPONSE_DATA = "data";

    public static final String CONFIG_URL = "url";
    public static final String CONFIG_ID_KEY = "idKey";
    public static final String CONFIG_HANDLE_TYPE = "handleType";

    private String mUrl;
    private String mIdKey;
    private String mHandleType;

    @Override
    public boolean isValidConfig(Data config)
    {
        boolean urlExists = config.contains(CONFIG_URL, String.class);
        boolean idKeyExists = config.contains(CONFIG_ID_KEY, String.class);
        boolean handleTypeExists = config.contains(CONFIG_HANDLE_TYPE, String.class);
        if(!(urlExists && idKeyExists && handleTypeExists))
            return false;
        return true;
    }

    @Override
    protected void doStart() throws FailedInitialisationException
    {
        // Check network connectivity
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected())
        {
            throw new FailedInitialisationException("Network connectivity not available!");
        }
        // Assign url
        mUrl = mConfig.get(CONFIG_URL);
        mIdKey = mConfig.get(CONFIG_ID_KEY);
        mHandleType = mConfig.get(CONFIG_HANDLE_TYPE);
        testConnection();
    }

    @Override
    protected void doStop()
    {
        // Nothing
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        super.onData(source, data);
        if(source == this)
        {
            // Service should not receive data from HTTP thread...
        }
        else
        {
            // Handle submitting data to remote
            submitData(data);
        }
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object obj)
    {
        if(source == this && mState != State.STOPPING && mState != State.FAILED)
        {
            if(event == Event.FAILED)
            {
                logd("HttpThread failed: " + (obj == null ? "Unknown" : obj.toString()));
                // Handle fail event from a HttpThread (obj is error message)
                failed((String)obj);
            }
            else if(event == Event.OK)
            {
                String[] arr = (String[])obj;
                String respJson = arr[0];
                logd("HttpThread ok [" + respJson + "]");
                Data response = Data.fromJson(respJson);
                if(arr[1] != null)
                {
                    Data submitData = Data.fromJson(arr[1]);
                    response.set(KEY_RESPONSE_DATA, submitData);
                }
                handleResponse(response);
            }
        }
    }

    private void testConnection()
    {
        String url = mUrl + '/' + QUERY_PATH;
        logd("Testing connection: " + url);
        DataEventHandler threadHandler = new DataEventHandler(this, this, this, this.getMainLooper());
        HttpThread thread = new HttpThread(url, null, threadHandler);
        thread.start();
        // Wait for callback to onEvent with response or error
    }

    private void handleResponse(Data resp)
    {
        if(mState == State.STARTING)
        {
            // Check connection test response
            String serverName = resp.get(KEY_SERVER_NAME);
            String version = resp.get(KEY_SERVER_VERSION).toString();
            Object[] handles = resp.get(KEY_SERVER_HANDLES);
            if(serverName == null || version == null || handles == null)
            {
                failed("Remote connection cannot be established!");
            }
            else
            {
                String debug = "";
                for (Object handle : handles)
                    debug += handle + " ";
                logd("ServerName: " + serverName + " | Verson: " + version + " | Handles: " + debug);
                boolean matched = false;
                for (Object handle : handles)
                {
                    if (handle.toString().equals(mHandleType))
                    {
                        changeState(State.STARTED);
                        matched = true;
                        break;
                    }
                }
                if (!matched)
                {
                    failed("Remote server cannot handle specified handle type! (" + mHandleType + ")");
                }
            }
        }
        else if(mState == State.STARTED)
        {
            // Check status and message
            String status = resp.get(KEY_RESPONSE_STATUS);
            String message = resp.get(KEY_RESPONSE_MESSAGE);
            mDataListener.onData(this, resp);
        }
    }

    private void submitData(Data data)
    {
        // To complete
        String json = data.toJson();
        String url = mUrl + '/' + SUBMIT_PATH + '/' + data.get(mIdKey);
        DataEventHandler threadHandler = new DataEventHandler(this, this, this, this.getMainLooper());
        HttpThread thread = new HttpThread(url, json, threadHandler);
        thread.start();
        // Wait for callback to onData with response, or onEvent with error
    }


}
