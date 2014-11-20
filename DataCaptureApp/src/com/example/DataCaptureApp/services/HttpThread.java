package com.example.DataCaptureApp.services;

import com.example.DataCaptureApp.utils.DataEventHandler;
import com.example.DataCaptureApp.data.Event;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpThread extends Thread
{
    private String mUrl;
    private String mData;
    private DataEventHandler mHandler;

    public HttpThread(String url, String data, DataEventHandler handler)
    {
        super();
        mUrl = url;
        mData = data;
        mHandler = handler;
    }

    @Override
    public void run()
    {
        InputStream in = null;
        String response = null;
        try
        {
            // Constuct URL and open connection
            URL url = new URL(mUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(RemoteConnectivityService.TIMEOUT);
            conn.setConnectTimeout(RemoteConnectivityService.TIMEOUT);
            conn.setRequestMethod("GET");
            if (mData != null)
            {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                OutputStream outStream = conn.getOutputStream();
                outStream.write(mData.getBytes("UTF-8"));
                outStream.close();
            }
            conn.connect();
            // Check response code
            int respCode = conn.getResponseCode();
            if(respCode != HttpURLConnection.HTTP_OK)
            {
                mHandler.onEvent(null, Event.FAILED, "Bad response code: " + respCode);
                return;
            }
            response = readResponse(conn.getInputStream(), RemoteConnectivityService.MAX_LEN);
            mHandler.onEvent(null, Event.OK, new String[] { response, mData });
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            mHandler.onEvent(null, Event.FAILED, "Invalid URL!");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            mHandler.onEvent(null, Event.FAILED, "Failed to establish HTTP connection!");
        }
        finally
        {
            try
            {
                if (in != null)
                    in.close();
            } catch (IOException e)
            {
            }
        }
    }

    private String readResponse(InputStream stream, int len) throws IOException
    {
        InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        String resp = new String(buffer).trim();
        return resp;
    }
}