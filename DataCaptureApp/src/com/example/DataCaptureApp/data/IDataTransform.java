package com.example.DataCaptureApp.data;

/**
 * Created by Tom on 6/09/2014.
 */
public interface IDataTransform
{
    public Data transform(Data dp);
    public Data[] transform(Data... dps);
}
