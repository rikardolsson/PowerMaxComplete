package com.example.olsson.powermaxcomplete;

import android.net.ConnectivityManager;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;


/**
 * Created by Olsson on 2015-08-22.
 */
public class AlarmServiceInterface {
   // public static final String SERVER_URL = "http://85.224.244.58/index.php";
    public static final String SERVER_URL = "http://192.168.1.100/index.php";
    private static String logtag = "PowerMax";
    public static String StatusAlarm = "AlarmDisconnected";

    public static String EnableAlarm() {
        return executeHttpRequest("command=EnableAlarm");
    }

    public static String DisableAlarm() {
        return executeHttpRequest("command=DisableAlarm");
    }

    public static String StatusAlarm() {
        StatusAlarm = executeHttpRequest("command=StatusAlarm");
        return StatusAlarm;
    }

    public static String ReturnAlarmStatus()
    {
        return StatusAlarm;
    }

    private static String executeHttpRequest(String data) {
        String result = "";
        Log.d(logtag, "Running thread to handle Http request");
        try {
            URL url = new URL(SERVER_URL);
            URLConnection connection = url.openConnection();


            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            // Send the POST data
            DataOutputStream dataOut = new DataOutputStream(connection.getOutputStream());
            dataOut.writeBytes(data);
            dataOut.flush();
            dataOut.close();
            // get the response from the server and store it in result
            DataInputStream in = new DataInputStream(connection.getInputStream());
            int i;
            char c;
            while ((i = in.read()) != -1) {
                // converts integer to character
                c = (char) i;
                result += Character.toString(c);
               }

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            result = null;
        }
        Log.d(logtag, "Read data and we got: " + result);
        return result;
    }

}
