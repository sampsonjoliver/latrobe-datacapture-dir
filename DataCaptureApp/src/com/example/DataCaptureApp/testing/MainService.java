package com.example.DataCaptureApp.testing;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.example.DataCaptureApp.R;


/**
 * Created by Tom on 3/09/2014.
 */
public class MainService extends Service implements IServiceListener
{
    public static final String START_ACTION = "com.example.DataCaptureApp.START";
    private static final String TAG = "MainService";
    private static final int NOTIFICATION_ID = 1;

    private IServiceListener mListener;
    private boolean mRunning = false;

    private RandomService mRandomService;
    private ServiceConnection mRandomServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mRandomService = ((RandomService.LocalBinder)binder).getService();
            mRandomService.setListener(MainService.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRandomService = null;
        }
    };

    private NotificationManager mNotificationMgr;
    private Notification mNotification;

    public class LocalBinder extends Binder
    {
        public MainService getService()
        {
            return MainService.this;
        }
    }

    private LocalBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "Bound");
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "Created");

        mNotificationMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        int icon = R.drawable.icon;
        CharSequence text = "Random Numbers";
        long when = System.currentTimeMillis();

        // Initialise notification
        mNotification = new Notification(icon, text, when);
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
        mNotification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
    }

    /*
     * Implement this method so service runs indefinitely until stopped!
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // Start RandomService and bind
        Log.d(TAG, "onStartCommand");
        // Start commands only valid from MainActivity with a bundle
        if(intent != null || intent.getExtras() == null)
        {
            mRunning = true;
            Intent startIntent = new Intent(this, RandomService.class);
            startIntent.setAction(START_ACTION);
            startIntent.putExtras(intent.getExtras());
            bindRandomService(startIntent);
        }
        else
        {
            Log.d(TAG, "Null intent, stopping self");
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "Destroying");
        // Clean up RandomService
        unbindRandomService();
        stopService(new Intent(this, RandomService.class));
        mRandomService = null;
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.d(TAG, "Unbinding");
        setListener(this);
        return true;
    }

    public void onServiceData(Service service, String data)
    {
        if(service == this)
        {
            // Update notification
            updateNotification(data);
        }
        else if(mListener != null)
        {
            // Data is from RandomService, pass on to listener
            mListener.onServiceData(this, data);
        }
    }

    private void bindRandomService(Intent intent)
    {
        bindService(intent, mRandomServiceConn, Context.BIND_AUTO_CREATE);
    }

    private void unbindRandomService()
    {
        if(mRandomService != null)
        {
            unbindService(mRandomServiceConn);
        }
    }

    private void updateNotification(String newText)
    {
        Context context = getApplicationContext();
        CharSequence title = "Random Numbers!";
        CharSequence text = newText;

        Intent notifIntent = new Intent(this, ServiceTestActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notifIntent, 0);

        // Initialise notification
        mNotification.setLatestEventInfo(context, title, text, contentIntent);
        mNotificationMgr.notify(NOTIFICATION_ID, mNotification);
    }

    /*
     * API methods
     */

    public void setListener(IServiceListener listener)
    {
        mListener = listener;
        if(listener == this)
        {
            updateNotification("Starting...");
            startForeground(NOTIFICATION_ID, mNotification);
        }
        else
        {
            stopForeground(true);
        }
    }

    public boolean isRunning()
    {
        return mRunning;
    }
}
