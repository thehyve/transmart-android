package nl.thehyve.transmartclient;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.MalformedURLException;
import java.net.URL;
import android.support.v4.app.Fragment;

import nl.thehyve.transmartclient.fragments.AddNewServerFragment;
import nl.thehyve.transmartclient.fragments.GraphFragment;
import nl.thehyve.transmartclient.fragments.ServerOverviewFragment;
import nl.thehyve.transmartclient.oauth.TokenGetterTask;
import nl.thehyve.transmartclient.oauth.TokenReceiver;
import nl.thehyve.transmartclient.rest.RestInteractionListener;
import nl.thehyve.transmartclient.rest.ServerResult;
import nl.thehyve.transmartclient.rest.TransmartServer;

/**
 * Created by Ward Weistra.
 * Copyright (c) 2015 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */

public class MainActivity extends AppCompatActivity implements
        ServerOverviewFragment.OnFragmentInteractionListener,
        GraphFragment.OnFragmentInteractionListener,
        TokenReceiver.TokenReceivedListener,
        RestInteractionListener {

    private static final String TAG = "MainActivity";
    private final IntentFilter intentFilter = new IntentFilter(TokenGetterTask.TOKEN_RECEIVED_INTENT);
    private final TokenReceiver tokenReceiver = new TokenReceiver();

    public static String CLIENT_ID = "android-client";
    public static String CLIENT_SECRET = "";

    public static TransmartServer transmartServer;
    private LocalBroadcastManager mBroadcastMgr;
    private android.support.v4.app.FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"--> onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();


        fragmentManager = getSupportFragmentManager();

//        When the activity is started from the OAuth return URL: Get the code out
        Intent intent = getIntent();
        Uri uri = intent.getData();
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        boolean oauthCodeUsed = settings.getBoolean("oauthCodeUsed", false);
        if (
                uri != null
                && uri.toString().startsWith("transmart://oauthresponse")
                && !oauthCodeUsed
                )
        {
//            TODO Show waiting sign: "Connecting to the tranSMART server"

            // Keep in mind that a new instance of the same application has been started
            Log.d(TAG,"Received uri");
            String code = uri.getQueryParameter("code");
            Log.d(TAG,"Received OAuth code: " + code);

            transmartServer = new TransmartServer();
            String currentServerUrl = settings.getString("currentServerUrl", "");
            Log.d(TAG, "Retrieved currentServerUrl from settings: " + currentServerUrl);
            transmartServer.setServerUrl(currentServerUrl);

            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("oauthCodeUsed", true);
            editor.apply();

            new TokenGetterTask(this.getApplicationContext(), transmartServer, CLIENT_ID, CLIENT_SECRET).execute(code);
        }

        if (savedInstanceState == null) {
            Log.d(TAG, "savedInstanceState is null");
            Fragment fragment = new AddNewServerFragment();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        }

        mBroadcastMgr = LocalBroadcastManager
                .getInstance(getApplicationContext());
        tokenReceiver.setTokenReceivedListener(this);
        mBroadcastMgr.registerReceiver(tokenReceiver, intentFilter);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_view);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG,"--> onStop called");
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG,"--> onSaveInstanceState called");
        // Always call the superclass so it can save the view hierarchy state
        savedInstanceState.putParcelable("transmartServer", transmartServer);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "--> onRestoreInstanceState called");
        transmartServer = savedInstanceState.getParcelable("transmartServer");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "--> onStart called");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "--> onRestart called");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "--> onResume called");
        super.onResume();

        if (transmartServer == null){
            Log.d(TAG,"transmartServer is not set.");
        } else {
            Log.d(TAG,"transmartServer is already set");
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "--> onPause called");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "--> onDestroy called");
        mBroadcastMgr.unregisterReceiver(tokenReceiver);
        super.onDestroy();
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

        if (id == R.id.action_about) {

            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Developed at The Hyve");

            PackageInfo pInfo = null;
            try {
                pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            assert pInfo != null;
            String versionName = pInfo.versionName;
            int versionCode = pInfo.versionCode;

            final TextView message = new TextView(this);
            message.setMovementMethod(LinkMovementMethod.getInstance());

            SpannableString s = new SpannableString(
                    "Version "+versionName+" (version code "+versionCode+")\n" +
                            "\n" +
                            "We provide open source solutions for bioinformatics. Find us at http://thehyve.nl\n" +
                            "\n" +
                            "This code is licensed under the GNU Lesser General Public License, " +
                            "version 3, or (at your option) any later version.\n" +
                            "\n" +
                            "Contribute at https://github.com/wardweistra/tranSMARTClient"
                    );
            Linkify.addLinks(s, Linkify.WEB_URLS);
            message.setText(s);
            alertDialog.setView(message,30,30,30,30);
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Cool", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            // Set the Icon for the Dialog
            alertDialog.setIcon(R.drawable.thehyve);
            alertDialog.show();

            return true;
        } else if (id == R.id.action_addNewServer) {
            Fragment fragment = new AddNewServerFragment();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment)
                    .addToBackStack("AddNewServerFragment")
                    .commit();
        }

        return super.onOptionsItemSelected(item);
    }

    // This is the method that is called when the submit button is clicked

    public void checkTransmartServerUrl(View view) {
        EditText serverUrlEditText = (EditText) findViewById(R.id.serverUrlField);
        String serverUrl = serverUrlEditText.getText().toString();

        if (serverUrl.equals("")) {
            Toast.makeText(this, "Please specify your server URL", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Scheme: " + Uri.parse(serverUrl).getScheme());
        if (Uri.parse(serverUrl).getScheme()==null){
            serverUrl = "https://" + serverUrl;

        }

        try {
            new URL(serverUrl);
        } catch (MalformedURLException e) {
            Toast toast = Toast.makeText(this, "Please specify the URL of your tranSMART server, starting with http:// or https://", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        if (serverUrl.substring(serverUrl.length()-1).equals("/")) {
            serverUrl = serverUrl.substring(0,serverUrl.length()-1);
            Log.d(TAG,"Removed trailing /: "+serverUrl);
        }

        connectToTranSMARTServer(serverUrl);
    }

    public void connectToTranSMARTServer(String serverUrl) {



        String query = serverUrl + "/oauth/authorize?"
                + "response_type=code"
                + "&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET
                + "&redirect_uri=" + "transmart://oauthresponse"
                ;

        // We need an Editor object to make preference changes.
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("currentServerUrl", serverUrl);
        editor.putBoolean("oauthCodeUsed", false);
        Log.d(TAG, "Saved currentServerUrl in to settings: " + serverUrl);

        Log.d(TAG, "Opening URL: " + query);

        // Commit the edits!
        editor.apply();

        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(query));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast toast = Toast.makeText(this, "There is no app installed to open the URL "+serverUrl, Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    // Methods for TokenReceiver

    public void onTokenReceived(ServerResult serverResult) {
        if (serverResult.getResponseCode() == 200) {
            String access_token;

            try {
                JSONObject jObject = new JSONObject(serverResult.getResult());
                access_token = jObject.getString("access_token");
                transmartServer.setAccess_token(access_token);
                String refresh_token = jObject.getString("refresh_token");
                transmartServer.setRefresh_token(refresh_token);
                Log.i(TAG,"access_token : " + access_token);
                Log.i(TAG,"refresh_token : " + refresh_token);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Fragment fragment = new ServerOverviewFragment();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment)
                    .addToBackStack("ServerOverviewFragment")
                    .commitAllowingStateLoss();

        } else {
            Toast toast = Toast.makeText(getBaseContext(), "Server responded with code "
                    + serverResult.getResponseCode() +": "
                    + serverResult.getResponseDescription(), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    // Methods for all REST calling fragments

    @Override
    public void authorizationLost() {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Authorization lost");
        final TextView message = new TextView(this);
        SpannableString s = new SpannableString(
                "The tranSMART server seems to have forgotten that you have given this app permission " +
                        "to access your data. Shall we try to reconnect?"
        );
        message.setText(s);
        alertDialog.setView(message, 30, 30, 30, 30);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Reconnect", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                connectToTranSMARTServer(transmartServer.getServerUrl());
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Try again", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Fragment fragment = new ServerOverviewFragment();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
            }
        });
        alertDialog.show();
    }

    @Override
    public void connectionLost() {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Connection lost");
        final TextView message = new TextView(this);
        SpannableString s = new SpannableString(
                "We seem to be unable to reach the tranSMART server. Please check your internet " +
                        "connection and try again."
        );
        message.setText(s);
        alertDialog.setView(message, 30, 30, 30, 30);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Try again", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Fragment fragment = new ServerOverviewFragment();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
    }

    // Methods for ServerOverviewFragment

    @Override
    public void onStudyClicked(String studyId) {
        Fragment fragment = GraphFragment.newInstance(studyId, transmartServer);
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment)
                .addToBackStack("GraphFragment")
                .commit();
    }

    // Methods for GraphFragment

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}