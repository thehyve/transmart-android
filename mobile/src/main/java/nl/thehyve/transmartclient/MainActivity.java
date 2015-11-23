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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
    private LocalBroadcastManager mBroadcastMgr;
    private android.support.v4.app.FragmentManager fragmentManager;

    Integer about_item, add_server_item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "--> onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                hideKeyboard();
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        refreshNavigationMenu();

        NavigationView mNavigationView = (NavigationView) findViewById(R.id.nav_view);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                Integer menuItemId = menuItem.getItemId();
                Log.d(TAG,"menuItem ID: "+ menuItemId);

                if (!menuItemId.equals(about_item)) {
                    menuItem.setChecked(true);

                    //Closing drawer on item click
                    drawer.closeDrawers();
                }

                //Check to see which item was being clicked and perform appropriate action
                if (menuItemId.equals(about_item)) {
                    Log.d(TAG,"about_item: "+ about_item);
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
                    Log.d(TAG,"add_server_item: "+ add_server_item);

                    Fragment fragment = new AddNewServerFragment();
                    fragmentManager.beginTransaction().replace(R.id.content_frame, fragment)
                            .addToBackStack("AddNewServerFragment")
                            .commit();
                    return true;
                } else {

                    for (TransmartServer transmartServerItem : transmartServers){
                        Log.d(TAG,"transmartServerItem ID: "+ transmartServerItem.getMenuItemID());

                        if (menuItemId.equals(transmartServerItem.getMenuItemID())) {
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
        });


        fragmentManager = getSupportFragmentManager();

//        When the activity is started from the OAuth return URL: Get the code out
        Intent intent = getIntent();
        Uri uri = intent.getData();
        Log.d(TAG,"uri: "+uri);
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        boolean oauthCodeUsed = settings.getBoolean("oauthCodeUsed", false);
        Log.d(TAG,"oauthCodeUsed: "+oauthCodeUsed);
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

    private void refreshNavigationMenu() {

        NavigationView mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        final Menu menu = mNavigationView.getMenu();

        menu.clear();

        SubMenu serverMenu = menu.addSubMenu(1,1,Menu.NONE,"Servers");
        serverMenu.setGroupCheckable(2,true,true);
        Integer order = 2;
        Log.d(TAG,"Number of servers connected for menu: "+transmartServers.size());
        for (TransmartServer transmartServer : transmartServers) {
            Integer menuItemID = serverMenu.add(2, order, Menu.NONE, transmartServer.getServerLabel())
                    .setIcon(R.drawable.ic_action_accounts)
                    .getItemId();
            transmartServer.setMenuItemID(menuItemID);
            order += 1;
        }
        add_server_item = serverMenu.add(2,order,Menu.NONE,"Add server").setIcon(R.drawable.ic_action_new_account).getItemId();
        Log.d(TAG, "add_server_item: " + add_server_item);
        // Pointless adding of drawable (which will not show up) to get the menu item to be drawn
        serverMenu.setIcon(R.drawable.ic_action_accounts);
        about_item = menu.add(2,order+1,Menu.NONE,"About").setIcon(R.drawable.ic_action_about).getItemId();
        Log.d(TAG,"about_item: "+ about_item);
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
        super.onDestroy();
    }

    // This is the method that is called when the submit button is clicked

    public void checkTransmartServerUrl(View view) {
        EditText serverUrlEditText = (EditText) findViewById(R.id.serverUrlField);
        EditText serverLabelField  = (EditText) findViewById(R.id.serverLabelField);

        String serverUrl   = serverUrlEditText.getText().toString();
        String serverLabel = serverLabelField.getText().toString();
        Log.d(TAG,"serverLabel: "+serverLabel);

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

    // Methods for GraphFragment

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}