package com.example.DataCaptureApp.utils;

import java.io.*;

/**
 * Created by Tom on 9/09/2014.
 */
public class SerialisationUtils
{
    private static ByteArrayOutputStream bOutput;
    private static ByteArrayInputStream bInput;
    private static ObjectOutput oOutput;
    private static ObjectInput oInput;
    public static byte[] serialise(Object obj)
    {
        try
        {
            bOutput = new ByteArrayOutputStream();
            oOutput = new ObjectOutputStream(bOutput);
            oOutput.writeObject(obj);
            return bOutput.toByteArray();
        }
        catch(Exception e) {}
        finally
        {
            try
            {
                oOutput.close();
            } catch (IOException e) {}
            try
            {
                bOutput.close();
            }
            catch (IOException e) {}
        }
        return null;
    }

    public static Object deserialise(byte[] bytes)
    {
        try
        {
            bInput = new ByteArrayInputStream(bytes);
            oInput = new ObjectInputStream(bInput);
            return oInput.readObject();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally
        {
            try {
                oInput.close();
            } catch (IOException e) {
            }
            try {
                bInput.close();
            } catch (IOException e) {
            }
        }
        return null;
    }
}
