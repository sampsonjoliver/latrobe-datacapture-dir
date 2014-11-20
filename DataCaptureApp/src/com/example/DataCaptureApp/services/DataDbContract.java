package com.example.DataCaptureApp.services;

import android.provider.BaseColumns;

/**
 * Created by Tom on 21/09/2014.
 */
public class DataDbContract
{
    public DataDbContract() {}

    public static abstract class DataDb implements BaseColumns
    {
        public static final String TABLE_NAME = "Data";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_SESSION = "session";
        public static final String COLUMN_NAME_DATA = "data";
        private static final String TIMESTAMP_TYPE = " INTEGER";
        private static final String SESSION_TYPE = " TEXT";
        private static final String DATA_TYPE = " BLOB";
    }

    public static final String CREATE_DATA_TABLE =
            "CREATE TABLE " + DataDb.TABLE_NAME + " (" +
                    DataDb.COLUMN_NAME_TIMESTAMP + DataDb.TIMESTAMP_TYPE + " PRIMARY KEY, " +
                    DataDb.COLUMN_NAME_SESSION + DataDb.SESSION_TYPE + ", " +
                    DataDb.COLUMN_NAME_DATA + DataDb.DATA_TYPE + " )";

    public static final String DELETE_DATA_TABLE =
            "DROP TABLE IF EXISTS " + DataDb.TABLE_NAME;
}
