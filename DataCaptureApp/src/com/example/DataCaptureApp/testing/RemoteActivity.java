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
import android.widget.EditText;
import android.widget.TextView;
import com.example.DataCaptureApp.*;
import com.example.DataCaptureApp.data.*;
import com.example.DataCaptureApp.services.RemoteConnectivityService;

/**
 * Created by Tom on 8/10/2014.
 */
public class RemoteActivity extends Activity implements IDataEventListener
{
    private static final String TAG = "RemoteActivity";

    private EditText mUrlText;
    private TextView mTextStatus;

    private RemoteConnectivityService mService;
    private Data mServiceConfig;

    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = (RemoteConnectivityService)((DataService.LocalBinder)binder).getService();
            Log.d(TAG, "Remote Service Connected");
            mService.setEventListener(RemoteActivity.this);
            mService.setDataListener(RemoteActivity.this);
            try
            {
                mService.start(mServiceConfig);
            }
            catch(FailedInitialisationException e)
            {
                setStatus("Failed to startSampling: " + e.getMessage());
                unbindService(this);
                return;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstance)
    {
        super.onCreate(savedInstance);
        setContentView(R.layout.remote);

        mUrlText = (EditText)findViewById(R.id.remoteUrl);
        mTextStatus = (TextView)findViewById(R.id.textStatus);
    }

    public void onInitialiseRemote(View v)
    {
        String url = mUrlText.getText().toString();
        mServiceConfig = new Data();
        mServiceConfig.set("url", url);
        mServiceConfig.set("idKey", "session");
        mServiceConfig.set("submitRoute", "session");
        mServiceConfig.set("handleType", "test");
        bindService(new Intent(this, RemoteConnectivityService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        if(source == mService)
        {
            setStatus(source + ": data received");
            String serverName = data.get("serverName");
            String version = data.get("version");
            Object[] handles = data.get("handles");
            String status = "Contacted " + serverName + " (" + version + ") [";
            for(Object obj : handles)
            {
                status += obj + " ";
            }
            status += ']';
            setStatus(status);
        }
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        if(source == mService)
        {
            setStatus(source + ": " + event + " [" + (arg == null ? "no arg" : arg.toString()) + "]");
        }
    }

    private void setStatus(final String status)
    {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                mTextStatus.append(status + '\n');
            }
        });
    }
}
