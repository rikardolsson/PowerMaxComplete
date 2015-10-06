package com.example.olsson.powermaxcomplete;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.*;

import org.apache.http.Header;

public class MainActivity extends Activity {
    ProgressDialog prgDialog;
    RequestParams params = new RequestParams();
    GoogleCloudMessaging gcmObj;
    Context applicationContext;
    String regId = "";
   // String token = "";
    private static String logtag = "PowerMax";

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    AsyncHttpClient client = new AsyncHttpClient();


    AsyncTask<Void, Void, String> createRegIdTask;

    public static final String REG_ID = "regId";
    public static final String EMAIL_ID = "eMailId";
    public static final String POWERMAX_STATUS = "PowerMaxStatus";
    EditText emailET;

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applicationContext = getApplicationContext();
        emailET = (EditText) findViewById(R.id.email);

        prgDialog = new ProgressDialog(this);
        // Set Progress Dialog Text
        prgDialog.setMessage("Please wait...");
        // Set Cancelable as False
        prgDialog.setCancelable(false);

        checkInternetConnection();

        SharedPreferences prefs = getSharedPreferences("UserDetails",
                Context.MODE_PRIVATE);
        String registrationId = "";
        //String registrationId = prefs.getString(REG_ID, "");

        //When Email ID is set in Sharedpref, User will be taken to HomeActivity
        if (!TextUtils.isEmpty(registrationId)) {
            Intent i = new Intent(applicationContext, HomeActivity.class);
            i.putExtra("regId", registrationId);
            startActivity(i);
            finish();
        }
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

    // When Register Me button is clicked
    public void RegisterUser(View view) {
        String emailID = emailET.getText().toString();

        if (!TextUtils.isEmpty(emailID) && Utility.validate(emailID)) {

            // Check if Google Play Service is installed in Device
            // Play services is needed to handle GCM stuffs
            if (checkPlayServices()) {

            //    Intent intent = new Intent(this, RegistrationIntentService.class);
              //  startService(intent);
                // Register Device in GCM Server
                registerInBackground(emailID);
                // Start IntentService to register this application with GCM.

            }
        }
        // When Email is invalid
        else {
            Toast.makeText(applicationContext, "Please enter valid email",
                    Toast.LENGTH_LONG).show();
        }

    }

    // AsyncTask to register Device in GCM Server
    private void registerInBackground(final String emailID) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";

                try {
                 //   InstanceID instanceID = InstanceID.getInstance(applicationContext);
                  //  regId = instanceID.getInstance(applicationContext).getToken("597727937607","GCM");
                    // [START get_token]
//                    InstanceID instanceID = InstanceID.getInstance(applicationContext);
//                    regId= instanceID.getToken("597727937607",GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

                    if (gcmObj == null) {
                        gcmObj = GoogleCloudMessaging
                                .getInstance(applicationContext);
                    }
                    regId = gcmObj
                            .register(ApplicationConstants.GOOGLE_PROJ_ID);


                    msg = "Registration ID :" + regId;

                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }

                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                if (!TextUtils.isEmpty(regId)) {
                    // Store RegId created by GCM Server in SharedPref
                    storeRegIdinSharedPref(applicationContext, regId, emailID);
                    Toast.makeText(
                            applicationContext,
                            "Registered with GCM Server successfully.\n\n"
                                    + msg, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(
                            applicationContext,
                            "Reg ID Creation Failed.\n\nEither you haven't enabled Internet or GCM server is busy right now. Make sure you enabled Internet and try registering again after some time."
                                    + msg, Toast.LENGTH_LONG).show();
                }
            }
        }.execute(null, null, null);
    }

    // Store  RegId and Email entered by User in SharedPref
    private void storeRegIdinSharedPref(Context context, String regId,
                                        String emailID) {
        SharedPreferences prefs = getSharedPreferences("UserDetails",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(REG_ID, regId);
        editor.putString(EMAIL_ID, emailID);

        storeRegIdinServer();
        statusAlarmMain();

        try {
            Thread.sleep(1000);                 //1000 milliseconds is one second.
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        editor.putString(POWERMAX_STATUS, AlarmServiceInterface.ReturnAlarmStatus());
        //editor.putString(POWERMAX_STATUS, "PowerMaxArmedAway");
        editor.commit();

        Intent i = new Intent(applicationContext,
                Powermax.class);
        startActivity(i);
        finish();
    }

    // Share RegID with GCM Server Application (Php)
    private void storeRegIdinServer() {
        prgDialog.show();
        //regId = RegistrationIntentService.returnToken();
        params.put("regId", regId);
        // Make RESTful webservice call using AsyncHttpClient object
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(ApplicationConstants.APP_SERVER_URL, params,
                new AsyncHttpResponseHandler() {
                    // When the response returned by REST has Http
                    // response code '200'
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        // Hide Progress Dialog
                        prgDialog.hide();
                        if (prgDialog != null) {
                            prgDialog.dismiss();
                        }
                        Toast.makeText(applicationContext,
                                "Reg Id shared successfully with Web App ",
                                Toast.LENGTH_LONG).show();
/*
                        Intent i = new Intent(applicationContext,
                                HomeActivity.class);

                        i.putExtra("regId", regId);
                        startActivity(i);
                        finish();
*/
                    }

                    // When the response returned by REST has Http
                    // response code other than '200' such as '404',
                    // '500' or '403' etc
                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        // Hide Progress Dialog
                        prgDialog.hide();
                        if (prgDialog != null) {
                            prgDialog.dismiss();
                        }
                        // When Http response code is '404'
                        if (statusCode == 404) {
                            Toast.makeText(applicationContext,
                                    "Requested resource not found",
                                    Toast.LENGTH_LONG).show();
                        }
                        // When Http response code is '500'
                        else if (statusCode == 500) {
                            Toast.makeText(applicationContext,
                                    "Something went wrong at server end",
                                    Toast.LENGTH_LONG).show();
                        }
                        // When Http response code other than 404, 500
                        else {
                            Toast.makeText(
                                    applicationContext,
                                    "Unexpected Error occcured! [Most common Error: Device might "
                                            + "not be connected to Internet or remote server is not up and running], check for other errors as well",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Check if Google Playservices is installed in Device or not
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        // When Play services not found in device
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                // Show Error dialog to install Play services
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(
                        applicationContext,
                        "This device doesn't support Play services, App will not work normally",
                        Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        } else {
            Toast.makeText(
                    applicationContext,
                    "This device supports Play services, App will work normally",
                    Toast.LENGTH_LONG).show();
        }
        return true;
    }

    // When Application is resumed, check for Play services support to make sure app will be running normally
    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    public String statusAlarmMain() {
        (new StatusAlarmTask()).execute();
        return "ok";
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
         }

    }


}