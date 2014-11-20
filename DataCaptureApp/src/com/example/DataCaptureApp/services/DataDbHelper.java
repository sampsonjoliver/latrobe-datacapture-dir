package com.example.DataCaptureApp.services;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Tom on 21/09/2014.
 */
public class DataDbHelper extends SQLiteOpenHelper
{
    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "Data.db";

    public DataDbHelper(Context context)
    {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(DataDbContract.CREATE_DATA_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL(DataDbContract.DELETE_DATA_TABLE);
        onCreate(db);
    }
}
