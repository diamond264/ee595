package com.example.ee595android;

import java.io.Serializable;
import android.util.Log;



public class HTMLvalue {

    private static String URL;
    private static String Time;

    private static final Singleton<HTMLvalue> htmlValue = new Singleton<HTMLvalue>() {
        @Override
        protected HTMLvalue create() {
            HTMLvalue value = new HTMLvalue();
            return value;
        }
    };

    public HTMLvalue getInstance() {
        return htmlValue.get();
    }

    public static void setURL(String input) {
        Log.d("SERIAL", input);
        htmlValue.get().URL = input;
//        URL = input;
    }

    public static void setTime(String input) {
        Log.d("SERIAL", input);
        htmlValue.get().Time = input;
//        Time = input;
    }

    public static String getURL(){
        return htmlValue.get().URL;
    }

    public static String getTime(){
        return htmlValue.get().Time;
    }

}
