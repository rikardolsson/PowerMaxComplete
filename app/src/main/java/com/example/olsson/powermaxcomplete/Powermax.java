package com.example.olsson.powermaxcomplete;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

public class Powermax extends Activity {
    // Declared Constants
    private static String logtag = "PowerMax";
    ImageView imageAlarmStatus;
    private static TextView PowerMaxMessage;
    static Context applicationContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_powermax);

        applicationContext = getApplicationContext();

        PowerMaxMessage = (TextView)findViewById(R.id.message_power_max_backend);

        imageAlarmStatus =(ImageView)findViewById(R.id.imageViewAlarmStatus);
       // statusAlarm();
        checkInternetConnection();

        // Intent Message sent from Broadcast Receiver
        String strGCM = getIntent().getStringExtra("msg");


        // Get Email ID from Shared preferences
        SharedPreferences prefs = getSharedPreferences("UserDetails",
                Context.MODE_PRIVATE);

        String Powermaxstatus = prefs.getString("PowerMaxStatus", "");

        if (strGCM == null) {
            PowerMaxMessage.setText(Powermaxstatus);
        }
        else {
            PowerMaxMessage.setText(strGCM);
        }

        if (Powermaxstatus.equals("PowerMaxArmedAway")) {
            imageAlarmStatus.setImageResource(R.drawable.padlock_closed);
        }
        else {
            imageAlarmStatus.setImageResource(R.drawable.padlock_open);
        }

        final Button buttonEnableAlarm = (Button) findViewById(R.id.buttonEnableAlarm);
        buttonEnableAlarm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(logtag, "onClick() called - Enable Alarm");
                // Perform action on click
                enableAlarm();
            }
        });

        final Button buttonDisableAlarm = (Button) findViewById(R.id.buttonDisableAlarm);
        buttonDisableAlarm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(logtag, "onClick() called - Disable Alarm");
                // Perform action on click
                disableAlarm();
            }
        });

        final Button buttonStatusAlarm = (Button) findViewById(R.id.buttonStatusAlarm);
        buttonStatusAlarm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(logtag, "onClick() called - Status Alarm");
                // Perform action on click
                statusAlarm();
            }
        });

        final Button buttonExitAlarm = (Button) findViewById(R.id.buttonExitAlarm);
        buttonExitAlarm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(logtag, "onClick() called - Exit Alarm");
                finish();
            }
        });
     }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String enableAlarm() {
        (new EnableAlarmTask()).execute();
        return "ok";
    }

    public String disableAlarm() {
        (new DisableAlarmTask()).execute();
        return "ok";
    }

    public String statusAlarm() {
        (new StatusAlarmTask()).execute();
        return "ok";
    }

    static public String updateMessageDisplay() {
        (new UpdateMessageTask()).execute();
        return "ok";
    }

    public boolean checkInternetConnection() {
        // get Connectivity Manager object to check connection
        ConnectivityManager connect =(ConnectivityManager)getSystemService(getBaseContext().CONNECTIVITY_SERVICE);
        boolean result = false;
        // Check for network connections
        if ( connect.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTED ||

                connect.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connect.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connect.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED ) {
            Log.d(logtag, "Connected");
            result = true;
        }else if (
                connect.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.DISCONNECTED ||
                        connect.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.DISCONNECTED  ) {
            Log.d(logtag, "Not connected");
            result = false;
        } else {
            Log.d(logtag, "Not sure we should be here");
        }
        return result;
    }

    /**
     * Used to spawn a thread to enable alarm.
     */
    @SuppressWarnings("unchecked")
    private class EnableAlarmTask extends AsyncTask {

        /**
         * Let's make the http request and return the result as a String.
         */
        protected String doInBackground(Object... args) {
                return AlarmServiceInterface.EnableAlarm();
        }
    }

    /**
     * Used to spawn a thread to enable alarm.
     */
    @SuppressWarnings("unchecked")
    private class DisableAlarmTask extends AsyncTask {
        /**
         * Let's make the http request and return the result as a String.
         */
        protected String doInBackground(Object... args) {
            return AlarmServiceInterface.DisableAlarm();

        }
    }
    @SuppressWarnings("unchecked")
     private class StatusAlarmTask extends AsyncTask{
        @Override
        protected String doInBackground(Object... String) {
            String result = "";
            result =  AlarmServiceInterface.StatusAlarm();
            Log.d(logtag, "Read data and we got: " + result);
            return result;
        }
        @Override
        protected void onPostExecute(Object result) {
            String data = result.toString();
            PowerMaxMessage.clearComposingText();
            PowerMaxMessage.setText("PowerMax sever:\n\n" + data);
        }

    }


    @SuppressWarnings("unchecked")
    static private class UpdateMessageTask extends AsyncTask{
        @Override
        protected String doInBackground(Object... String) {
            String result = "";
            result =  GCMNotificationIntentService.ReturnMessageAlarm();
            return result;
        }
        @Override
        protected void onPostExecute(Object result) {
            String data = result.toString();
            PowerMaxMessage.clearComposingText();
            PowerMaxMessage.setText(data);
        }

    }

}
