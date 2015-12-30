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
import android.support.v4.app.FragmentTransaction;
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
import nl.thehyve.transmartclient.fragments.MenuListener;
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
        AddNewServerFragment.OnFragmentInteractionListener,
        MenuListener,
        RestInteractionListener {

    private static final String TAG = "MainActivity";
    private final IntentFilter intentFilter = new IntentFilter(TokenGetterTask.TOKEN_RECEIVED_INTENT);
    private final TokenReceiver tokenReceiver = new TokenReceiver();

    public static String CLIENT_ID = "android-client";
    public static String CLIENT_SECRET = "";

    public static List<TransmartServer> transmartServers = new ArrayList<>();
    private CoordinatorLayout coordinatorLayout;
    private NavigationView mNavigationView;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private LocalBroadcastManager mBroadcastMgr;
    private android.support.v4.app.FragmentManager fragmentManager;

    int about_item, add_server_item;

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

        if (savedInstanceState == null) {
            Log.d(TAG, "savedInstanceState is null");
            readTransmartServersFromFile();
        }

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

            TransmartServer transmartServer = getUniqueConnectionStatus(
                    TransmartServer.ConnectionStatus.SENTTOURL);

            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("oauthCodeUsed", true);
            editor.apply();

            if (transmartServer != null) {
                setUniqueConnectionStatus(transmartServer, TransmartServer.ConnectionStatus.CODERECEIVED);
                Log.d(TAG, "Now the connectionStatus is " + transmartServer.getConnectionStatus());

                Fragment fragment = AddNewServerFragment.newInstance(
                        transmartServer.getServerUrl(),
                        transmartServer.getServerLabel(),
                        true);
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit();

                new TokenGetterTask(this.getApplicationContext(), transmartServer, CLIENT_ID, CLIENT_SECRET).execute(code);
            } else {
                Log.w(TAG,"No servers with connectionStatus: SENTTOURL");
            }

            receivedURI = true;
        }

        if (savedInstanceState == null) {
            if (!receivedURI) {
                navigateToBeginState(true);
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
        Log.d(TAG, "In onOptionsItemSelected");
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
            int menuItemId = menuItem.getItemId();
            Log.d(TAG,"Clicked on menuItem ID: "+ menuItemId);

            if (menuItemId != about_item) {
                menuItem.setChecked(true);

                //Closing drawer on item click
                drawer.closeDrawers();
            }

            //Check to see which item was being clicked and perform appropriate action
            if (menuItemId == about_item) {
                Log.d(TAG,"Clicked about_item: "+ about_item);

                // Get package version information
                PackageInfo pInfo = null;
                try {
                    pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                assert pInfo != null;
                String versionName = pInfo.versionName;
                int versionCode = pInfo.versionCode;

                // Create message with links
                // TODO insert Github icon with link
                SpannableString s = new SpannableString(String.format(getString(R.string.about_text),
                        versionName,
                        versionCode));
                Linkify.addLinks(s, Linkify.WEB_URLS);

                // Create dialog
                TextView message = (TextView) new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.info_title)
                        .setMessage(s)
                        .setIcon(R.drawable.thehyve)
                        .setPositiveButton(R.string.info_positive, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        })
                        .show()
                        .findViewById(android.R.id.message);
                // Make links clickable
                message.setMovementMethod(LinkMovementMethod.getInstance());


                return true;
            } else if (menuItemId == add_server_item) {
                Log.d(TAG, "Clicked add_server_item: " + add_server_item);

                Fragment fragment = new AddNewServerFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .addToBackStack("AddNewServerFragment")
                        .commit();
                return true;
            } else {

                for (TransmartServer transmartServer : transmartServers){

                    if (menuItemId == transmartServer.getMenuItemID()) {
                        Log.d(TAG,"Clicked transmartServer ID: "+ transmartServer.getMenuItemID());

                        Fragment fragment = ServerOverviewFragment.newInstance(transmartServer);
                        fragmentManager.beginTransaction()
                                .replace(R.id.content_frame, fragment)
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

        int order = 0;
        int group = 1;

        SubMenu serverMenu = menu.addSubMenu(Menu.NONE, order, order, "Servers");
        order += 1;

        Log.d(TAG,"Number of servers connected for menu: "+transmartServers.size());
        for (TransmartServer menuTransmartServer : transmartServers) {
            Log.d(TAG,"Added server label: "+menuTransmartServer.getServerLabel()+". ("+menuTransmartServer+")");
            int menuItemID = serverMenu.add(group, order, order, transmartServer.getServerLabel())
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
        Log.d(TAG, "Checking serverLabel: " + serverLabel);

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
        
        Resources res = getResources();
        String[] endings = res.getStringArray(R.array.url_endings);
        for (String ending : endings) {
            if (serverUrl.endsWith(ending)) {
                serverUrl = serverUrl.substring(0,serverUrl.length()-ending.length());
                Log.d(TAG,"Removed ending: "+ending);
                break;
            }
        }
        Log.d(TAG, serverUrl);

        TransmartServer transmartServer = new TransmartServer();
        transmartServer.setServerUrl(serverUrl);
        transmartServer.setServerLabel(serverLabel);
        connectToTranSMARTServer(transmartServer);
    }

    public void connectToTranSMARTServer(TransmartServer transmartServer) {

        Log.d(TAG,"Connecting to transmart server: "+ transmartServer.getServerLabel());

        String query = transmartServer.getServerUrl() + "/oauth/authorize?"
                + "response_type=code"
                + "&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET
                + "&redirect_uri=" + "transmart://oauthresponse"
                ;

        // TODO make sharedpreferences unnecessary?
        // We need an Editor object to make preference changes.
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("oauthCodeUsed", false);

        Log.d(TAG, "Opening URL: " + query);

        // Commit the edits!
        editor.apply();
        hideKeyboard();

        setUniqueConnectionStatus(transmartServer, TransmartServer.ConnectionStatus.SENTTOURL);
        if (!transmartServers.contains(transmartServer)) {
            transmartServers.add(transmartServer);
            refreshNavigationMenu();
        }
        writeTransmartServersToFile();

        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(query));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            String message = String.format(getString(R.string.no_app_for_url), transmartServer.getServerUrl());
            TextInputLayout inputServerUrl = (TextInputLayout) findViewById(R.id.input_server_url);
            inputServerUrl.setError(message);
        }

    }

    // Methods for TokenReceiver

    public void onTokenReceived(ServerResult serverResult) {
        if (serverResult.getResponseCode() == 200) {
            String access_token;

            TransmartServer transmartServer = getUniqueConnectionStatus(
                    TransmartServer.ConnectionStatus.CODERECEIVED);

            if (transmartServer != null) {

                Log.d(TAG, "Received token, going to add it to server: " + transmartServer.getServerLabel());

                try {
                    JSONObject jObject = new JSONObject(serverResult.getResult());
                    access_token = jObject.getString("access_token");
                    transmartServer.setAccess_token(access_token);
                    String refresh_token = jObject.getString("refresh_token");
                    transmartServer.setRefresh_token(refresh_token);
                    Log.i(TAG, "access_token : " + access_token);
                    Log.i(TAG, "refresh_token : " + refresh_token);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                transmartServer.setConnectionStatus(TransmartServer.ConnectionStatus.CONNECTED);
                refreshNavigationMenu();

                Log.d(TAG, "Setting menu item " + transmartServer.getMenuItemID() + " to checked");
                mNavigationView.setCheckedItem(transmartServer.getMenuItemID());

                Fragment fragment = ServerOverviewFragment.newInstance(transmartServer);
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commitAllowingStateLoss();

            } else {
                Log.w(TAG,"No servers with connectionStatus: CODERECEIVED");
            }

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
    // Methods for MenuListener interface

        }
    @Override
    public void setMenuItemChecked(int menuItem) {
        Log.d(TAG, "Setting menu item " + menuItem + " to checked");
        mNavigationView.setCheckedItem(menuItem);
    }

    // Methods for RestInteractionListener interface


    @Override
    public void notConnectedYet(final TransmartServer transmartServer) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.not_connected_yet)
                .setMessage(R.string.not_connected_yet_text)
                .setIcon(R.drawable.ic_disconnected_94)
                .setPositiveButton(R.string.not_connected_yet_positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        connectToTranSMARTServer(transmartServer);
                    }
                })
                .setNegativeButton(R.string.not_connected_yet_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                })
                .show();
    }

    @Override
    public void authorizationLost(final TransmartServer transmartServer) {
        // TODO first try to renew the authorisation token with the refresh token. Only on fail give reconnect dialog.
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.authorization_lost)
                .setMessage(R.string.authorization_lost_text)
                .setIcon(R.drawable.ic_disconnected_94)
                .setPositiveButton(R.string.authorization_lost_positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        connectToTranSMARTServer(transmartServer);
                    }
                })
                .setNegativeButton(R.string.authorization_lost_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // TODO make general for graph or server overview
                        Fragment fragment = ServerOverviewFragment.newInstance(transmartServer);
                        fragmentManager.beginTransaction()
                                .replace(R.id.content_frame, fragment)
                                .commit();
                    }
                })
                .show();
    }

    @Override
    public void connectionLost(final TransmartServer transmartServer) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.connection_lost)
                .setMessage(R.string.connection_lost_text)
                .setIcon(R.drawable.ic_signal_cellular_connected_no_internet_4_bar_black_24dp)
                .setPositiveButton(R.string.connection_lost_positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // TODO make general for graph or server overview
                        Fragment fragment = ServerOverviewFragment.newInstance(transmartServer);
                        fragmentManager.beginTransaction()
                                .replace(R.id.content_frame, fragment)
                                .commit();
                    }
                })
                .setNegativeButton(R.string.connection_lost_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                })
                .show();
    }

    // Methods for ServerOverviewFragment

    @Override
    public void onStudyClicked(String studyId, TransmartServer transmartServer) {
        Fragment fragment = GraphFragment.newInstance(studyId, transmartServer);
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack("GraphFragment")
                .commit();
    }

    @Override
    public void removeServerDialog(final TransmartServer transmartServer) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.sure_remove_server)
                .setMessage(R.string.sure_remove_server_text)
                .setIcon(R.drawable.ic_warning_black_24dp)
                .setPositiveButton(R.string.sure_remove_server_positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        removeServer(transmartServer);
                    }
                })
                .setNegativeButton(R.string.sure_remove_server_negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                })
                .show();
    }

    // Removes server without dialog
    private void removeServer(TransmartServer transmartServer) {
        transmartServers.remove(transmartServer);
        refreshNavigationMenu();
        navigateToBeginState(false);
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

    private void navigateToBeginState(boolean firstFragment) {
        Log.d(TAG,"Navigating to begin state");

        FragmentTransaction ft = fragmentManager.beginTransaction();
        String fragmentName;

        if (transmartServers.size() > 0) {
            TransmartServer transmartServer = transmartServers.get(0);
            Log.d(TAG, "Setting menu item " + transmartServer.getMenuItemID() + " to checked");
            mNavigationView.setCheckedItem(transmartServer.getMenuItemID());

            Fragment fragment = ServerOverviewFragment.newInstance(transmartServer);
            ft.replace(R.id.content_frame, fragment);
            fragmentName = "ServerOverviewFragment";
        } else {
            Log.d(TAG, "Setting menu item " + add_server_item + " to checked");
            mNavigationView.setCheckedItem(add_server_item);

            Fragment fragment = new AddNewServerFragment();
            ft.replace(R.id.content_frame, fragment);
            fragmentName = "AddNewServerFragment";
        }

        Log.d(TAG,"Navigating to "+fragmentName);
        if (!firstFragment) {
            ft.addToBackStack(fragmentName);
        }

        ft.commit();
    }

    private void writeTransmartServersToFile() {
        if (transmartServers.size() > 0) {
            Log.d(TAG, "Writing: " + gson.toJson(transmartServers));
            try {
                FileOutputStream fos = openFileOutput(serversFileName, MODE_MULTI_PROCESS);
                PrintWriter pw = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(fos)));
                pw.println(gson.toJson(transmartServers));
                pw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            deleteFile(serversFileName);
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

    private TransmartServer getUniqueConnectionStatus(
            TransmartServer.ConnectionStatus connectionStatus) {

        for (TransmartServer transmartServer : transmartServers) {
            if (transmartServer.getConnectionStatus() == connectionStatus) {
                return transmartServer;
            }
        }

        return null;
    }

    private void setUniqueConnectionStatus(
            TransmartServer transmartServer,
            TransmartServer.ConnectionStatus connectionStatus) {

        Log.d(TAG,"Going to set connectionStatus to "+connectionStatus+" for "+transmartServer);
        Log.d(TAG,"All servers for connectionStatus:"+transmartServers);
        // Make sure all other abandoned TransmartServers of this type get removed
        for (TransmartServer transmartServerOther : transmartServers) {
            if (transmartServerOther.getConnectionStatus() == connectionStatus
                    && transmartServerOther != transmartServer) {
                Log.d(TAG, "Setting to notconnected: "+transmartServerOther);
                transmartServerOther.setConnectionStatus(TransmartServer.ConnectionStatus.NOTCONNECTED);
            }
        }

        transmartServer.setConnectionStatus(connectionStatus);
    }
}