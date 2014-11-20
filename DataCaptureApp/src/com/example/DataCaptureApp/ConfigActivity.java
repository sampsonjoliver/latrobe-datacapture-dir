package com.example.DataCaptureApp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Tom on 5/11/2014.
 */
public class ConfigActivity extends PreferenceActivity
{
    public static final String PREF_NAME = "data_collection_prefs";

    private ListPreference mBluetoothPref;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Set correct shared preferences file
        PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(PREF_NAME);

        // Create preferences screen
        addPreferencesFromResource(R.xml.config);

        mBluetoothPref = (ListPreference)findPreference(getString(R.string.pref_bluetooth));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.config_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_adv_configure:
                Intent configureIntent = new Intent(this, AdvConfigActivity.class);
                finish();
                startActivity(configureIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = new HashSet<BluetoothDevice>();
        if(adapter != null)
        {
            devices = adapter.getBondedDevices();
        }
        String[] names = new String[devices.size()];
        String[] macs = new String[devices.size()];
        int count = 0;
        for (BluetoothDevice device : devices)
        {
            names[count] = device.getName();
            macs[count] = device.getAddress();
            ++count;
        }
        mBluetoothPref.setEntries(names);
        mBluetoothPref.setEntryValues(macs);
    }
}
