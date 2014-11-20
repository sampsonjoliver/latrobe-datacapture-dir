package com.example.DataCaptureApp.testing;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.example.DataCaptureApp.R;

/**
 * Created by Tom on 3/09/2014.
 */
public class ServiceTestActivity extends Activity implements IServiceListener
{
    public static final String START_ACTION = "com.example.DataCaptureApp.START";
    public static final String STOP_ACTION = "com.example.DataCaptureApp.STOP";
    public static final String TAG = "ServiceTestActivity";

    private EditText txtMin;
    private EditText txtMax;
    private Spinner spinnerFreq;
    private Button btnStart;
    private Button btnStop;
    private TextView txtResult;

    private MainService mMainService;
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "Receiving Binder");
            mMainService = ((MainService.LocalBinder)binder).getService();
            if(mMainService.isRunning()) {
                mMainService.setListener(ServiceTestActivity.this);
            }
            else
            {
                Log.d(TAG, "Service created but not running, discarding binding");
                mMainService = null;
            }
            updateButtons();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMainService = null;
            updateButtons();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "Creating");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.service_test);

        // Initialise spinner
        spinnerFreq = (Spinner)findViewById(R.id.spinnerFrequency);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.frequencies, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFreq.setAdapter(adapter);

        // Get references to UI elements
        txtMin = (EditText)findViewById(R.id.textMinNumber);
        txtMax = (EditText)findViewById(R.id.textMaxNumber);
        txtResult = (TextView)findViewById(R.id.textResult);
        btnStart = (Button)findViewById(R.id.buttonStartService);
        btnStop = (Button)findViewById(R.id.buttonStopService);

    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "Pausing");
        unbindServiceListener();
        //unbindMainService();
        super.onPause();
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "Resuming");
        super.onResume();
        bindServiceListener();
        //bindMainService(new Intent(this, MainService.class));
    }


    @Override
    public void onStart()
    {
        super.onStart();
        Log.d(TAG, "Starting");
        // Bind service_test service if it is running
        bindMainService(new Intent(this, MainService.class));
    }

    @Override
    public void onStop()
    {
        Log.d(TAG, "Stopping");
        unbindServiceListener();
        unbindMainService();
        super.onStop();
    }

    public void startService(View v)
    {
        Log.d(TAG, "Starting MainService");
        Intent startIntent = new Intent(this, MainService.class);
        startIntent.setAction(START_ACTION);
        try {
            int freq = Integer.parseInt(spinnerFreq.getSelectedItem().toString());
            int min = Integer.parseInt(txtMin.getText().toString());
            int max = Integer.parseInt(txtMax.getText().toString());
            if(max < min)
                throw new Exception("Max less than min!");
            startIntent.putExtra("freq", freq);
            startIntent.putExtra("min", min);
            startIntent.putExtra("max", max);
            startService(startIntent);
            bindMainService(startIntent);
        }
        catch(Exception e)
        {
            Toast.makeText(this, "Error with values!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, e.getMessage());
        }
    }

    private void bindMainService(Intent intent)
    {
        Log.d(TAG, "Binding MainService");
        bindService(intent, mServiceConn, Context.BIND_ABOVE_CLIENT);
    }

    private void unbindMainService()
    {
        if(mMainService != null)
        {
            Log.d(TAG, "Unbinding MainService");
            unbindService(mServiceConn);
            mMainService = null;
        }
    }

    private void bindServiceListener()
    {
        if(mMainService != null)
        {
            mMainService.setListener(this);
        }
    }

    private void unbindServiceListener()
    {
        if(mMainService != null)
        {
            mMainService.setListener(null);
        }
    }

    public void stopService(View v)
    {
        Log.d(TAG, "Stopping MainService");
        unbindMainService();
        stopService(new Intent(this, MainService.class));
        mMainService = null;
        updateButtons();
    }

    private void updateButtons()
    {
        if(mMainService != null)
        {
            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
        }
        else
        {
            btnStart.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.GONE);
        }
    }

    private void updateResult(final String data)
    {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                txtResult.setText(data);
            }
        });
    }

    public void onServiceData(Service service, String data)
    {
        updateResult(data);
    }
}