package com.example.DataCaptureApp.data;

/**
 * Created by Tom on 6/09/2014.
 */
public interface IEventListener
{
    public void onEvent(IEventSource source, Event event, Object arg);
}
