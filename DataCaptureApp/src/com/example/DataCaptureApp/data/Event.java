package com.example.DataCaptureApp.data;

/**
 * Created by Tom on 22/09/2014.
 */
public enum Event
{
    DESTROYED,
    READY,
    STARTING,
    STARTED,
    STOPPING,
    STOPPED,
    FAILED,
    TIMESYNC,
    CLOSING,
    CONNECTED,
    SERVICE_AVAILABLE,
    ACTION_START,
    ACTION_STOP,
    SLAVE_READY,
    SLAVE_STOPPED,
    MASTER_STOPPING,
    OK;

    public static final Event[] values = values();
}
