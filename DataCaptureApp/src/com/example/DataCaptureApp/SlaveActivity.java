package com.example.DataCaptureApp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.DataCaptureApp.data.*;

/**
 * Created by Tom on 5/11/2014.
 */
public class SlaveActivity extends Activity implements IEventListener
{
    private Button mStart;
    private Button mStop;
    private View mProgress;
    private ImageView mImage;
    private TextView mStatus;

    private State mState = State.START;

    private boolean mReconnecting = false;

    private DataServiceConnection mConnection = new DataServiceConnection(SlaveService.class);
    private SlaveService mService;

    private enum State
    {
        START,
        LOAD,
        RUN,
        FAIL,
        WARN
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slave);

        mStart = (Button)findViewById(R.id.buttonStart);
        mStop = (Button)findViewById(R.id.buttonStop);
        mProgress = findViewById(R.id.progressSlave);
        mImage = (ImageView)findViewById(R.id.imageSlave);
        mStatus = (TextView)findViewById(R.id.textStatus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.slave_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_bluetooth:
                Intent bluetoothIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(bluetoothIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        checkState();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mConnection.unbind();
        mService = null;
    }

    @Override
    public void onEvent(IEventSource source, Event event, Object arg)
    {
        if(source == mConnection && event == Event.SERVICE_AVAILABLE)
        {
            mService = (SlaveService)mConnection.getService();
            mService.setEventListener(this);
            if(!mReconnecting)
            {
                // Start up the service
                try
                {
                    mService.start(null);
                } catch (FailedInitialisationException e)
                {
                    mConnection.unbind();
                    setStatus("Service failed to start: " + e.getMessage());
                    setState(State.FAIL);
                }
            }
            else
            {
                // Update UI to reflect service state
                checkState();
                mReconnecting = false;
            }
        }
        else if(source == mService)
        {
            String newStatus = null;
            switch(event)
            {
                case STARTING:
                    newStatus = "Awaiting connection...";
                    break;
                case STARTED:
                    newStatus = "Connected to master";
                    setState(State.RUN);
                    break;
                case STOPPING:
                    newStatus = "Connection closed";
                    disconnectService();
                    setState(State.START);
                    break;
                case FAILED:
                    String reason = arg != null ? arg.toString() : null;
                    newStatus = "Service failed (" + reason + ")";
                    disconnectService();
                    setState(State.FAIL);
                    break;
                case ACTION_START:
                    newStatus = "Sampling";
                    break;
                case ACTION_STOP:
                    newStatus = "Sampling stopped";
                    break;
                default:
                    break;
            }
            if(newStatus != null)
                setStatus(newStatus);
        }
    }

    public void onButtonStart(View v)
    {
        // Bind SlaveService
        if(mService == null)
        {
            if(!SlaveService.isRunning())
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
        if(mService != null)
        {
            disconnectService();
            checkState();
            mService = null;
        }
    }

    private void checkState()
    {
        // Bind to service and resume if it is running
        if(SlaveService.isRunning())
        {
            if (mService == null)
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
                        setStatus(mService.isSampling() ? "Sampling" : "Not sampling");
                        setState(State.RUN);
                        break;
                    case FAILED:
                        setStatus("Service has failed");
                        setState(State.FAIL);
                        break;
                }
            }
        }
        else
        {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled())
            {
                setStatus("Bluetooth unavailable");
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

    private void setState(State newState)
    {
        switch(newState)
        {
            case START:
                mImage.setVisibility(View.VISIBLE);
                mImage.setImageResource(R.drawable.ic_action_accept);
                mProgress.setVisibility(View.GONE);
                mStart.setVisibility(View.VISIBLE);
                mStop.setVisibility(View.GONE);
                break;
            case LOAD:
                mImage.setVisibility(View.GONE);
                mProgress.setVisibility(View.VISIBLE);
                mStart.setVisibility(View.GONE);
                mStop.setVisibility(View.VISIBLE);
                break;
            case RUN:
                mImage.setVisibility(View.VISIBLE);
                mImage.setImageResource(R.drawable.ic_action_refresh);
                mProgress.setVisibility(View.GONE);
                mStart.setVisibility(View.GONE);
                mStop.setVisibility(View.VISIBLE);
                break;
            case FAIL:
                mImage.setVisibility(View.VISIBLE);
                mImage.setImageResource(R.drawable.ic_action_warning);
                mProgress.setVisibility(View.GONE);
                mStart.setVisibility(View.VISIBLE);
                mStop.setVisibility(View.GONE);
                break;
            case WARN:
                mImage.setVisibility(View.VISIBLE);
                mImage.setImageResource(R.drawable.ic_action_settings);
                mProgress.setVisibility(View.GONE);
                mStart.setVisibility(View.GONE);
                mStop.setVisibility(View.GONE);
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
}