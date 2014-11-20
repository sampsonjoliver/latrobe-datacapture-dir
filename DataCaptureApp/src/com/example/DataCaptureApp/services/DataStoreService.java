package com.example.DataCaptureApp.services;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.DataCaptureApp.services.DataDbContract.DataDb;
import com.example.DataCaptureApp.data.DataService;
import com.example.DataCaptureApp.utils.SerialisationUtils;
import com.example.DataCaptureApp.data.*;

/**
 * Created by Tom on 21/09/2014.
 */
public class DataStoreService extends DataService
{
    public static final String CONFIG_TIMESTAMP_FIELD = "timestamp";
    public static final String CONFIG_SESSION_FIELD = "session";
    private DataDbHelper mHelper;
    private SQLiteDatabase mDatabase;

    private String mTimestampField;
    private String mSessionField;

    @Override
    protected void doStart()
    {
        mHelper = new DataDbHelper(this);
        mDatabase = mHelper.getWritableDatabase();
        mTimestampField = mConfig.get(CONFIG_TIMESTAMP_FIELD);
        mSessionField = mConfig.get(CONFIG_SESSION_FIELD);
        changeState(State.STARTED);
    }

    @Override
    protected void doStop()
    {
        // Nothing
    }

    @Override
    protected boolean isValidConfig(Data config)
    {
        boolean timestampFieldExists = config.contains(CONFIG_TIMESTAMP_FIELD, String.class);
        boolean sessionFieldExists = config.contains(CONFIG_SESSION_FIELD, String.class);
        return timestampFieldExists && sessionFieldExists;
    }

    @Override
    public void onData(IDataSource source, Data data)
    {
        super.onData(source, data);
        persist(data); // Persist into database
        if(mDataListener != null)
            mDataListener.onData(this, data); // Pass on data object unchanged
    }

    public void persist(Data data)
    {
        long timestamp = data.get(mTimestampField);
        String session = data.get(mSessionField);
        byte[] bytes = SerialisationUtils.serialise(data);
        if(bytes == null)
            logd("Null data bytes!");
        ContentValues values = new ContentValues();
        values.put(DataDb.COLUMN_NAME_TIMESTAMP, timestamp);
        values.put(DataDb.COLUMN_NAME_DATA, bytes);
        long row = mDatabase.insert(DataDb.TABLE_NAME, null, values);
    }

    public Data[] retrieve(String sessionId, long startTimestamp, long endTimestamp)
    {
        String selection = DataDb.COLUMN_NAME_TIMESTAMP + " >= " + startTimestamp +
                " AND " + DataDb.COLUMN_NAME_TIMESTAMP + " <= " + endTimestamp +
                " AND " + DataDb.COLUMN_NAME_SESSION + " = '" + sessionId + "'";
        Cursor c = mDatabase.query(DataDb.TABLE_NAME, null, selection, null, null, null, null);
        c.moveToFirst();
        int count = c.getCount();
        Data[] dataArr = new Data[count];
        for(int i = 0; i < count; ++i)
        {
            byte[] bytes = c.getBlob(1);
            dataArr[i] = (Data)SerialisationUtils.deserialise(bytes);
            c.move(1);
        }
        return dataArr;
    }

    public Data[] retrieve(String sessionId, long endTimestamp)
    {
        return retrieve(sessionId, 0, endTimestamp);
    }

    public void delete(String sessionId, long startTimestamp, long endTimestamp)
    {
        String selection = DataDb.COLUMN_NAME_TIMESTAMP + " >= " + startTimestamp +
                " AND " + DataDb.COLUMN_NAME_TIMESTAMP + " <= " + endTimestamp +
                " AND " + DataDb.COLUMN_NAME_SESSION + " = '" + sessionId + "'";
        int rows = mDatabase.delete(DataDb.TABLE_NAME, selection, null);
    }

    public void delete(String sessionId, long endTimestamp)
    {
        delete(sessionId, 0, endTimestamp);
    }
}

