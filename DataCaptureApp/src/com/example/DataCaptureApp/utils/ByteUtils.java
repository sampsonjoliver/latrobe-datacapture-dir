package com.example.DataCaptureApp.utils;

import java.nio.ByteBuffer;

/**
 * Created by Tom on 7/09/2014.
 */
public class ByteUtils
{
    private static final int LONG_BYTES = Long.SIZE / 8;
    private static final int FLOAT_BYTES = Float.SIZE / 8;
    private static final ByteBuffer LONG_BUFFER = ByteBuffer.allocate(LONG_BYTES);
    private static final ByteBuffer FLOAT_BUFFER = ByteBuffer.allocate(FLOAT_BYTES);

    public static long bytesToLong(byte[] bytes, int start)
    {
        LONG_BUFFER.position(0);
        LONG_BUFFER.put(bytes, start, LONG_BYTES);
        LONG_BUFFER.flip();
        return LONG_BUFFER.getLong(0);
    }

    public static byte[] longToBytes(long l)
    {
        LONG_BUFFER.putLong(0, l);
        return LONG_BUFFER.array();
    }

    public static float bytesToFloat(byte[] bytes, int start)
    {
        FLOAT_BUFFER.position(0);
        FLOAT_BUFFER.put(bytes, start, FLOAT_BYTES);
        FLOAT_BUFFER.flip();
        return FLOAT_BUFFER.getFloat(0);
    }

    public static byte[] floatToBytes(float f)
    {
        FLOAT_BUFFER.putFloat(0, f);
        return FLOAT_BUFFER.array();
    }

    public static void main(String[] args)
    {
        System.out.println("Float Size: " + Float.SIZE);
        System.out.println("Float Buffer Size: " + FLOAT_BUFFER.array().length);
        System.out.println("Test 1: Float -> Byte[] -> Float");
        float f = -4.5624f;
        System.out.println("Float: " + f);
        byte[] b = floatToBytes(f);
        System.out.print("Bytes: ");
        for(byte b2 : b)
        {
            System.out.print(b2 + " ");
        }
        System.out.println();
        f = bytesToFloat(b, 0);
        System.out.println("Float: " + f);

        System.out.println("\n\nTest 2: Long -> Byte[] -> Long");
        long l = -734454554;
        System.out.println("Long: " + l);
        b = longToBytes(l);
        System.out.print("Bytes ");
        for(byte b2 : b)
        {
            System.out.print(b + " ");
        }
        System.out.println();
        l = bytesToLong(b, 0);
        System.out.println("Long: " + l);
        b = longToBytes(l);
        System.out.print("Bytes ");
        for(byte b2 : b)
        {
            System.out.print(b + " ");
        }
        System.out.println();
        l = bytesToLong(b, 0);
        System.out.println("Long: " + l);
    }
}