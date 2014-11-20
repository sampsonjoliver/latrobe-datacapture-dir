package com.example.DataCaptureApp.testing;

import com.example.DataCaptureApp.data.Data;
import com.example.DataCaptureApp.data.DataTransform;
import com.example.DataCaptureApp.data.IDataTransform;

/**
 * Created by Tom on 6/09/2014.
 */
public class DataTester
{
    public static void main(String[] args)
    {
        Data dp = new Data();

        log("Test 1 - Setting String\n-----------------");
        String strKey = "string";
        String strValue = "Test String!";
        log("Setting '" + strKey + "' to '" + strValue + "'");
        dp.set(strKey, strValue);
        log();

        log("Test 2 - Retrieving String\n-----------------");
        log("Retrieving '" + strKey + "'");
        String retrieved = dp.get(strKey);
        log(strKey + " = " + retrieved);
        log();

        log("Test 3 - Setting Integer\n-----------------");
        String intKey = "int";
        int intValue = 100;
        log("Setting '" + intKey + "' to '" + intValue + "'");
        dp.set(intKey, intValue);
        log();

        log("Test 4 - Retrieving Integer\n-----------------");
        log("Retrieving '" + intKey + "'");
        int retInt = dp.get(intKey);
        log(intKey + " = " + retInt);
        log();

        log("Test 5 - Setting DataTransform\n------------------");
        String dptKey = "dpt";
        DataTransform dptValue = new DataTransform();
        log("Setting '" + dptKey + "' to DataTransform object");
        dp.set(dptKey, dptValue);
        log();

        log("Test 6 - Retrieving DataTransform as IDataTransform\n-----------------------");
        IDataTransform idpt = dp.get(dptKey);
        log("Is IDataTransform? " + (idpt instanceof IDataTransform));
        log("Is DataTransform? " + (idpt instanceof DataTransform));
        log();

        log("Test 7 - Deleting String\n----------------------");
        log("Deleting '" + strKey + "'");
        retrieved = dp.remove(strKey);
        log("'" + strKey + "' was '" + retrieved + "'");
        log("'" + strKey + "' is '" + dp.get(strKey) + "'");

        log("Test 8 - Data to JSON String\n----------------------");
        Data test = new Data();
        test.set("number", 23.53);
        test.set("integer", 10000);
        test.set("boolean", true);
        test.set("intArray", new Integer[] {1,2,3,4});
        test.set("intArray2", new int[] {1,2,3,4});
        test.set("stringArray", new String[] {"Hello", "World", "Testing!"});
        Data test2 = new Data();
        test2.set("int", 30);
        test2.set("bool", true);
        test.set("data", test2);
        String json = test.toJson();
        log("JSON: " + json);

        log("Test 9 - JSON String to Data\n----------------------------");
        Data data = Data.fromJson(json);
        log("Data: created");

        log();
        log("TESTING COMPLETE");
        log();
    }

    public static void log()
    {
        log("");
    }
    public static void log(String msg)
    {
        System.out.println(msg);
    }
}
