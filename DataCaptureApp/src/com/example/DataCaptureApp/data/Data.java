package com.example.DataCaptureApp.data;

import com.example.DataCaptureApp.utils.JSONReader;

import java.io.Serializable;
import java.util.*;

/**
 * Created by Tom on 6/09/2014.
 */
public class Data implements Serializable
{
    private Map<String, Object> mMap = new HashMap<String, Object>();

    public Data() {}

    public <T> Data(String key, T value)
    {
        set(key, value);
    }

    public <T> T get(String key)
    {
        return (T)mMap.get(key);
    }

    public <T> void set(String key, T value)
    {
        mMap.put(key, value);
    }

    public <T> T remove(String key)
    {
        return (T)mMap.remove(key);
    }

    public boolean contains(String key)
    {
        return mMap.containsKey(key);
    }

    public boolean contains(String key, Class c)
    {
        Object obj = mMap.get(key);
        return c.isInstance(obj);
    }

    public Set<Field> getFields()
    {
        Set<Map.Entry<String, Object>> entries = mMap.entrySet();
        Set<Field> fields = new HashSet<Field>();
        for(Map.Entry<String, Object> entry : entries)
        {
            fields.add(new Field(entry.getKey(), entry.getValue()));
        }
        return fields;
    }

    public void clear()
    {
        mMap.clear();
    }

    // Note: Primitive arrays not transformed correctly!
    public String toJson()
    {
        StringBuilder json = new StringBuilder();
        json.append('{');
        Set<Map.Entry<String, Object>> entries = mMap.entrySet();
        boolean isFirst = true;
        for(Map.Entry<String, Object> entry : entries)
        {
            if(entry.getValue() != null)
            {
                // Comma separator (applied on second and subsequent iterations
                if (!isFirst)
                    json.append(",");
                isFirst = false;
                // Add key
                json.append("\"" + entry.getKey() + "\":");
                // Add value
                json.append(toJson(entry.getValue()));
            }
        }
        json.append('}');
        return json.toString();
    }

    private String toJson(Object obj)
    {
        String json = "";
        // Determine value string
        if(obj instanceof Number || obj instanceof Boolean)
        {
            json += obj.toString();
        }
        else if(obj instanceof Object[])
        {
            Object[] array = (Object[])obj;
            json += '[';
            boolean isFirst = true;
            for(Object element : array)
            {
                if(element != null)
                {
                    if (!isFirst)
                        json += ',';
                    isFirst = false;
                    json += toJson(element);
                }
            }
            json += ']';
        }
        else if(obj instanceof Data)
        {
            json += ((Data) obj).toJson();
        }
        else
        {
            json += "\"" + obj.toString() + "\"";
        }
        return json;
    }

    // Note: Arrays always stored as object arrays!
    public static Data fromJson(String json)
    {
        JSONReader reader = new JSONReader();
        Object obj = reader.read(json);
        if(!(obj instanceof Map))
            return null;
        Data data = (Data)getObject(obj);
        return data;
    }

    private static Object getObject(Object jsonObj)
    {
        if(jsonObj instanceof Map)
        {
            Data data = new Data();
            Map map = (Map) jsonObj;
            Set<Map.Entry> entries = map.entrySet();
            for (Map.Entry entry : entries)
            {
                String key = entry.getKey().toString();
                Object value = getObject(entry.getValue());
                data.set(key, value);
            }
            return data;
        }
        else if(jsonObj instanceof Collection)
        {
            Object[] objArr = ((Collection) jsonObj).toArray();
            for(int i = 0; i < objArr.length; ++i)
            {
                objArr[i] = getObject(objArr[i]);
            }
            return objArr;
        }
        return jsonObj;
    }

    public class Field
    {
        private String mKey;
        private Object mValue;

        public Field(String key, Object value)
        {
            mKey = key;
            mValue = value;
        }

        public String getKey()
        {
            return mKey;
        }

        public Object getValue()
        {
            return mValue;
        }
    }
}
