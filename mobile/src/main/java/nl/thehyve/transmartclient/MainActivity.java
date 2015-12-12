package nl.thehyve.transmartclient;

import android.animation.ValueAnimator;
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
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
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
import android.view.SubMenu;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

    public static List<TransmartServer> transmartServers = new ArrayList<>();
    public static TransmartServer transmartServer;
    private CoordinatorLayout coordinatorLayout;
    private NavigationView mNavigationView;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private LocalBroadcastManager mBroadcastMgr;
    private android.support.v4.app.FragmentManager fragmentManager;

    Integer about_item, add_server_item;

    private final static String serversFileName = "servers.txt";
    Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "--> onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        drawer = (DrawerLayout) findViewById(R.id.main_view);
        toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                hideKeyboard();
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        refreshNavigationMenu();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);

        mNavigationView.setNavigationItemSelectedListener(new OnTransmartNavigationItemSelectedListener(drawer));

        fragmentManager = getSupportFragmentManager();

//        When the activity is started from the OAuth return URL: Get the code out
        Intent intent = getIntent();
        Uri uri = intent.getData();
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        boolean oauthCodeUsed = settings.getBoolean("oauthCodeUsed", false);
        boolean receivedURI = false;

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
            String currentServerLabel = settings.getString("currentServerLabel", "");
            Log.d(TAG, "Retrieved currentServerUrl from settings: " + currentServerUrl);
            Log.d(TAG, "Retrieved currentServerLabel from settings: " + currentServerLabel);
            transmartServer.setServerUrl(currentServerUrl);
            transmartServer.setServerLabel(currentServerLabel);

            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("oauthCodeUsed", true);
            editor.apply();

            new TokenGetterTask(this.getApplicationContext(), transmartServer, CLIENT_ID, CLIENT_SECRET).execute(code);
            receivedURI = true;
        }

        if (savedInstanceState == null) {
            Log.d(TAG, "savedInstanceState is null");

            readTransmartServersFromFile();

            if (!receivedURI) {
                navigateToBeginState();
            }
        }

        mBroadcastMgr = LocalBroadcastManager
                .getInstance(getApplicationContext());
        tokenReceiver.setTokenReceivedListener(this);
        mBroadcastMgr.registerReceiver(tokenReceiver, intentFilter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_view);
        Log.d(TAG,"In onOptionsItemSelected");
        switch (item.getItemId()) {
            case android.R.id.home:
                Log.d(TAG,"In android.R.id.home");
                if (drawer.getDrawerLockMode(GravityCompat.START) == DrawerLayout.LOCK_MODE_UNLOCKED) {
                    Log.d(TAG,"UNLOCKED");
                    drawer.openDrawer(GravityCompat.START);
                } else {
                    Log.d(TAG,"Not UNLOCKED");
                    onBackPressed();
                }
        }

        return false;
    }

    private class OnTransmartNavigationItemSelectedListener implements NavigationView.OnNavigationItemSelectedListener {

        DrawerLayout drawer;

        public OnTransmartNavigationItemSelectedListener(DrawerLayout drawer) {
            this.drawer = drawer;
        }

        // This method will trigger on item Click of navigation menu
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
            Integer menuItemId = menuItem.getItemId();
            Log.d(TAG,"Clicked on menuItem ID: "+ menuItemId);

            if (!menuItemId.equals(about_item)) {
                menuItem.setChecked(true);

                //Closing drawer on item click
                drawer.closeDrawers();
            }

            //Check to see which item was being clicked and perform appropriate action
            if (menuItemId.equals(about_item)) {
                Log.d(TAG,"Clicked about_item: "+ about_item);
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle(R.string.info_title);

                PackageInfo pInfo = null;
                try {
                    pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                assert pInfo != null;
                String versionName = pInfo.versionName;
                int versionCode = pInfo.versionCode;

                final TextView message = new TextView(MainActivity.this);
                message.setMovementMethod(LinkMovementMethod.getInstance());

                SpannableString s = new SpannableString(
                        "Version " + versionName + " (version code " + versionCode + ")\n" +
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
                alertDialog.setView(message, 30, 30, 30, 30);
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Cool", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                // Set the Icon for the Dialog
                alertDialog.setIcon(R.drawable.thehyve);
                alertDialog.show();

                return true;
            } else if (menuItemId.equals(add_server_item)) {
                Log.d(TAG, "Clicked add_server_item: " + add_server_item);

                Fragment fragment = new AddNewServerFragment();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment)
                        .addToBackStack("AddNewServerFragment")
                        .commit();
                return true;
            } else {

                for (TransmartServer transmartServerItem : transmartServers){

                    if (menuItemId.equals(transmartServerItem.getMenuItemID())) {
                        Log.d(TAG,"Clicked transmartServerItem ID: "+ transmartServerItem.getMenuItemID());
                        transmartServer = transmartServerItem;
                        Fragment fragment = new ServerOverviewFragment();
                        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment)
                                .addToBackStack("ServerOverviewFragment")
                                .commit();
                        return true;
                    }
                }

                return true;
            }

        }
    }

    private void refreshNavigationMenu() {


        NavigationView mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        final Menu menu = mNavigationView.getMenu();

        menu.clear();

        Integer order = 0;
        Integer group = 1;

        SubMenu serverMenu = menu.addSubMenu(Menu.NONE, order, order, "Servers");
        order += 1;

        Log.d(TAG,"Number of servers connected for menu: "+transmartServers.size());
        for (TransmartServer menuTransmartServer : transmartServers) {
            Log.d(TAG,"Added server label: "+menuTransmartServer.getServerLabel()+". ("+menuTransmartServer+")");
            Integer menuItemID = serverMenu.add(group, order, order, menuTransmartServer.getServerLabel())
                    .setIcon(R.drawable.ic_action_accounts)
                    .getItemId();
            menuTransmartServer.setMenuItemID(menuItemID);
            order += 1;
        }
        add_server_item = serverMenu.add(group,order,order,"Add server").setIcon(R.drawable.ic_action_new_account).getItemId();
        order += 1;
        Log.d(TAG, "Added add_server_item: " + add_server_item);

        serverMenu.setGroupCheckable(group, true, true);

        about_item = menu.add(Menu.NONE,order,order,"About").setIcon(R.drawable.ic_action_about).getItemId();
        Log.d(TAG, "Added about_item: " + about_item);
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
        Log.d(TAG, "--> onStop called");
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "--> onSaveInstanceState called");
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
        mBroadcastMgr.unregisterReceiver(tokenReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "--> onDestroy called");
        mBroadcastMgr.unregisterReceiver(tokenReceiver);
        writeTransmartServersToFile();
        super.onDestroy();
    }

    // This is the method that is called when the submit button is clicked

    public void checkTransmartServerUrl(View view) {
        EditText serverUrlEditText = (EditText) findViewById(R.id.serverUrlField);
        EditText serverLabelField  = (EditText) findViewById(R.id.serverLabelField);

        String serverUrl   = serverUrlEditText.getText().toString();
        String serverLabel = serverLabelField.getText().toString();
        Log.d(TAG,"Checking serverLabel: "+serverLabel);

        if (serverUrl.equals("")) {
            TextInputLayout inputServerUrl = (TextInputLayout) findViewById(R.id.input_server_url);
            inputServerUrl.setError(getString(R.string.no_server_url_specified));
            return;
        }

        Log.d(TAG, "Scheme: " + Uri.parse(serverUrl).getScheme());
        if (Uri.parse(serverUrl).getScheme()==null){
            serverUrl = "https://" + serverUrl;
        }

        try {
            URL url = new URL(serverUrl);
            if (serverLabel.equals("")) {
                serverLabel = url.getPath();
                Log.d(TAG, "Set serverLabel to "+serverLabel);
            }
        } catch (MalformedURLException e) {
            TextInputLayout inputServerUrl = (TextInputLayout) findViewById(R.id.input_server_url);
            inputServerUrl.setError(getString(R.string.malformed_url));
            return;
        }

        if (serverUrl.substring(serverUrl.length()-1).equals("/")) {
            serverUrl = serverUrl.substring(0,serverUrl.length()-1);
            Log.d(TAG,"Removed trailing /: "+serverUrl);
        }

        connectToTranSMARTServer(serverUrl, serverLabel);
    }

    public void connectToTranSMARTServer(String serverUrl, String serverLabel) {
        Log.d(TAG,"Connecting to transmart server: "+ serverLabel);

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
        editor.putString("currentServerLabel", serverLabel);
        editor.putBoolean("oauthCodeUsed", false);
        Log.d(TAG, "Saved currentServerUrl in to settings: " + serverUrl);
        Log.d(TAG, "Saved currentServerLabel in to settings: " + serverLabel);

        Log.d(TAG, "Opening URL: " + query);

        // Commit the edits!
        editor.apply();
        hideKeyboard();

        writeTransmartServersToFile();

        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(query));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            String message = String.format(getString(R.string.no_app_for_url), serverUrl);
            TextInputLayout inputServerUrl = (TextInputLayout) findViewById(R.id.input_server_url);
            inputServerUrl.setError(message);
        }

    }

    // Methods for TokenReceiver

    public void onTokenReceived(ServerResult serverResult) {
        if (serverResult.getResponseCode() == 200) {
            String access_token;

            Log.d(TAG,"Received token, going to add it to server: "+ transmartServer.getServerLabel());

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

            transmartServers.add(transmartServer);
            refreshNavigationMenu();
            NavigationView mNavigationView = (NavigationView) findViewById(R.id.nav_view);
            final Menu menu = mNavigationView.getMenu();

            Log.d(TAG, "Setting menu item " + transmartServer.getMenuItemID() + " to checked");
            mNavigationView.setCheckedItem(transmartServer.getMenuItemID());

            Fragment fragment = new ServerOverviewFragment();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment)
                    .addToBackStack("ServerOverviewFragment")
                    .commitAllowingStateLoss();

        } else {

            String message = String.format(getString(R.string.server_responded_with),
                    serverResult.getResponseCode(),
                    serverResult.getResponseDescription());
            Log.d(TAG, message);
            Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            }).show();

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
                connectToTranSMARTServer(transmartServer.getServerUrl(), transmartServer.getServerLabel());
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

    @Override
    public void removeServer() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder
                .setTitle(R.string.sure_remove_server)
                .setMessage(R.string.sure_remove_server_text)
                .setPositiveButton(R.string.sure_remove_server_positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        transmartServers.remove(transmartServer);
                        refreshNavigationMenu();
                        navigateToBeginState();
                    }
                })
                .setNegativeButton(R.string.sure_remove_server_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                })
                .show();
    }

    // Methods for GraphFragment

    @Override
    public void setToggleState(boolean isEnabled) {
        if (drawer == null)
            return;

        if (isEnabled) {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            animateToArrow(false);
        } else {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            animateToArrow(true);
        }
        toggle.syncState();
    }

    private void animateToArrow(boolean toArrow) {
        int start, stop;
        if (toArrow) {
            start = 0;
            stop = 1;
        } else {
            start = 1;
            stop = 0;
        }
        ValueAnimator anim = ValueAnimator.ofFloat(start, stop);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float slideOffset = (Float) valueAnimator.getAnimatedValue();
                toggle.onDrawerSlide(drawer, slideOffset);
            }
        });
        anim.setInterpolator(new DecelerateInterpolator());
        anim.setDuration(300);
        anim.start();
    }

    // Util methods

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void navigateToBeginState() {
        Log.d(TAG,"Navigating to begin state");
        if (transmartServers.size() > 0) {
            Log.d(TAG,"Navigating to first server");

            transmartServer = transmartServers.get(0);
            Log.d(TAG, "Setting menu item " + transmartServer.getMenuItemID() + " to checked");

            mNavigationView.setCheckedItem(transmartServer.getMenuItemID());
            Fragment fragment = new ServerOverviewFragment();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment)
                    .addToBackStack("ServerOverviewFragment")
                    .commit();
        } else {
            Log.d(TAG,"Navigating to add_server_item");

            Log.d(TAG, "Setting menu item " + add_server_item + " to checked");
            mNavigationView.setCheckedItem(add_server_item);
            Fragment fragment = new AddNewServerFragment();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        }
    }

    private void writeTransmartServersToFile() {
        if (transmartServers.size() > 0) {
            Log.d(TAG, "Writing: " + gson.toJson(transmartServers));
            try {
                FileOutputStream fos;
                fos = openFileOutput(serversFileName, MODE_MULTI_PROCESS);
                PrintWriter pw = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(fos)));
                pw.println(gson.toJson(transmartServers));
                pw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean readTransmartServersFromFile() {
        try {
            FileInputStream fis = openFileInput(serversFileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            Log.d(TAG,"Reading from "+serversFileName);
            line = br.readLine();
            Type listType = new TypeToken<ArrayList<TransmartServer>>() {}.getType();
            transmartServers = gson.fromJson(line,listType);
            br.close();

            if (transmartServers.size() > 0) {
                Log.d(TAG, "Restored transmartServers");
                refreshNavigationMenu();
                return true;
            } else {
                throw new Error("transmartServers from file resulted in 0 servers");
            }
        } catch (IOException e) {
            Log.i(TAG, "IOException. Probably no servers on file.");
            return false;
        }
    }
}