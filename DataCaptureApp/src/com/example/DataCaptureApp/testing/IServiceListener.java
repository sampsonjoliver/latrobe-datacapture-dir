package com.example.DataCaptureApp.testing;

import android.app.Service;

/**
 * Created by Tom on 3/09/2014.
 */
public interface IServiceListener
{
    public void onServiceData(Service service,String data);
}
