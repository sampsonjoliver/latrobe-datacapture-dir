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
import com.example.DataCaptureApp.R;
import com.example.DataCaptureApp.data.DataService;
import com.example.DataCaptureApp.services.DataStoreService;
import com.example.DataCaptureApp.data.FailedInitialisationException;
import com.example.DataCaptureApp.data.*;

/**
 * Created by Tom on 21/09/2014.
 */
public class DataStoreActivity extends Activity implements IDataEventListener
{
    public static final String TAG = "DataStoreActivity";
    public static final int SOURCE_SERVICE = 0;
    public static final String SESSION_FIELD = "session";
    public static final String SESSION = "test_session";
    private EditText mStartTimestamp;
    private EditText mEndTimestamp;
    private TextView mResults;

    private DataStoreService mService;
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = (DataStoreService)((DataService.LocalBinder)binder).getService();
            Log.d(TAG, "Data Store Service Connected");
            mService.setEventListener(DataStoreActivity.this);
            mService.setDataListener(DataStoreActivity.this);
            try
            {
                Data data = new Data();
                data.set(DataStoreService.CONFIG_SESSION_FIELD, SESSION_FIELD);
                mService.start(data);
            }
            catch(FailedInitialisationException e) {}
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_store);

        mStartTimestamp = (EditText)findViewById(R.id.textStartTimestamp);
        mEndTimestamp = (EditText)findViewById(R.id.textEndTimestamp);
        mResults = (TextView)findViewById(R.id.textResults);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        bindService(new Intent(this, DataStoreService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if(mService != null)
        {
            unbindService(mServiceConn);
            mService = null;
        }
    }


    @Override
    public void onData(IDataSource source, Data data)
    {
        Log.d(TAG, "Data: " + source);
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        Log.d(TAG, "Event: " + source + " " + event);
    }

    public void addData(View v)
    {
        if(mService != null)
        {
            // Construct Data from UI
            Data d = new Data();
            long timestamp = Long.parseLong(mStartTimestamp.getText().toString());
            int value = Integer.parseInt(mEndTimestamp.getText().toString());
            d.set("timestamp", timestamp);
            d.set("value", value);
            d.set(SESSION_FIELD, SESSION);
            // Persist
            mService.persist(d);
        }
    }

    public void deleteData(View v)
    {
        if(mService != null)
        {
            // Retrieve parameters from UI
            long startTimestamp = Long.parseLong(mStartTimestamp.getText().toString());
            long endTimestamp = Long.parseLong(mEndTimestamp.getText().toString());
            // Delete
            mService.delete(SESSION, startTimestamp, endTimestamp);
        }
    }

    public void retrieveData(View v)
    {
        if(mService != null)
        {
            // Retrieve parameters from UI
            long startTimestamp = Long.parseLong(mStartTimestamp.getText().toString());
            long endTimestamp = Long.parseLong(mEndTimestamp.getText().toString());
            // Retrieve
            Data[] data = mService.retrieve(SESSION, startTimestamp, endTimestamp);
            // Display in results
            String str = "";
            for(Data d : data)
            {
                str += "[" + d.get("timestamp") + " " + d.get("value") + "] \n";
            }
            setResults(str);
        }
    }

    private void setResults(final String results)
    {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                mResults.setText(results);
            }
        });
    }
}