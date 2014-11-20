package com.example.DataCaptureApp.testing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.example.DataCaptureApp.R;

/**
 * Created by Tom on 3/09/2014.
 */
public class TestActivity extends Activity
{
    public static final String TAG = "MainActivity";

    private String[] mActivityNames = new String[] { "ServiceTest", "Bluetooth", "SensorSample",
            "DataStore", "RemoteConnectivity", "Master", "Slave"};
    private Class[] mActivityClasses = new Class[] { ServiceTestActivity.class, BluetoothActivity.class, SensorSampleActivity.class,
            DataStoreActivity.class, RemoteActivity.class, MasterTestActivity.class, SlaveTestActivity.class };
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "Creating");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Initialise activity list
        ListView activityList = (ListView)findViewById(R.id.activityList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mActivityNames);
        activityList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                startActivity(new Intent(TestActivity.this, mActivityClasses[position]));
            }
        });
        activityList.setAdapter(adapter);
    }
}