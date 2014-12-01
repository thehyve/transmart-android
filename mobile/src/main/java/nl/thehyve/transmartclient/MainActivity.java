package nl.thehyve.transmartclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    public static String CLIENT_ID = "api-client";
    public static String CLIENT_SECRET = "api-client";
    TransmartServer transmartServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (transmartServer == null){
            Log.d(TAG,"transmartServer wasn't set. Setting it now.");
            transmartServer = new TransmartServer();
            SharedPreferences settings = getPreferences(MODE_PRIVATE);
            String currentServerUrl = settings.getString("currentServerUrl", "");
            Log.d(TAG,"Retrieved currentServerUrl from settings: "+currentServerUrl);
            transmartServer.setServerUrl(currentServerUrl);
        } else {
            Log.d(TAG,"transmartServer is already set");
        }

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null && uri.toString()
                .startsWith("transmart://oauthresponse"))
        {
            Log.d("TranSMART","Received uri");
            String code = uri.getQueryParameter("code");
            Toast toast = Toast.makeText(this, "Received OAuth code: " + code, Toast.LENGTH_SHORT);
            toast.show();
            Log.d("TranSMART","Received OAuth code: " + code);

            new TokenGetterTask().execute(code);
        }

        // TODO Check for whether there are already servers defined
        Fragment fragment = new AddNewServerFragment();

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

    @Override
    protected void onStop() {
        Log.d(TAG,"onStop called");
        // We need an Editor object to make preference changes.
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String currentServerUrl = transmartServer.getServerUrl();
        editor.putString("currentServerUrl", currentServerUrl);
        Log.d(TAG,"Saved currentServerUrl in to settings: "+currentServerUrl);

        // Commit the edits!
        editor.apply();

        super.onStop();
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
        if (id == R.id.action_about) {

            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Developed at The Hyve");

            final TextView message = new TextView(this);
            message.setMovementMethod(LinkMovementMethod.getInstance());

            SpannableString s = new SpannableString("We provide open source solutions for bioinformatics. " +
                    "Find us at http://thehyve.nl\n\n" +
                    "Contribute at https://github.com/wardweistra/tranSMARTClient");
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
        }

        return super.onOptionsItemSelected(item);
    }

    // This is the method that is called when the submit button is clicked

    public void connectToTranSMARTServer(View view) {

        EditText serverUrlEditText = (EditText) findViewById(R.id.serverUrl);
        String serverUrl = serverUrlEditText.getText().toString();

        if (serverUrl.equals("")) {
            Toast toast = Toast.makeText(this, "Please specify your server URL", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        transmartServer.setServerUrl(serverUrl);

        String query = serverUrl + "/oauth/authorize?"
                + "response_type=code"
                + "&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET
                + "&redirect_uri=" + "transmart://oauthresponse"
                ;

        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(query));
        startActivity(intent);
    }

    public class TransmartServer {
        String serverUrl;
        String access_token;
        String refresh_token;
        String prettyName;

        public TransmartServer() {
            this.serverUrl = "";
            this.access_token = "";
            this.refresh_token = "";
            this.prettyName = "";
            Log.d(TAG,"transmartServer has been instantiated.");
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            Log.d(TAG,"Set serverUrl to " + serverUrl);
        }

        public String getServerUrl() {
            Log.d(TAG,"Asked for serverUrl: " + serverUrl);
            return serverUrl;
        }

    }

    private class TokenGetterTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String code = String.valueOf(params[0]);

            Log.d(TAG,"Getting serverUrl");
            String serverUrl = transmartServer.getServerUrl();
            Log.d(TAG,"serverUrl: " + serverUrl);

            String query = serverUrl + "/oauth/token?"
                    + "grant_type=authorization_code"
                    + "&client_id=" + CLIENT_ID
                    + "&client_secret=" + CLIENT_SECRET
                    + "&code=" + code
                    + "&redirect_uri=" + "transmart://oauthresponse" //CALLBACK_URL
                    ;


            Log.v(TAG, "Sending query: [" + query + "].");

            DefaultHttpClient httpClient = new DefaultHttpClient();

            HttpGet httpGet = new HttpGet(query);

            String responseLine;
            StringBuilder responseBuilder = new StringBuilder();
            String result = "";
            try {
                HttpResponse response = httpClient.execute(httpGet);
                Log.i(TAG,"Statusline : " + response.getStatusLine());

                InputStream data = response.getEntity().getContent();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(data));

                while ((responseLine = bufferedReader.readLine()) != null) {
                    responseBuilder.append(responseLine);
                }
                result = responseBuilder.toString();
                Log.i(TAG,"Response : " + result);

            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);

//            TODO Save details of server if connection was successful
//            TODO Create new ServerOverview
//            TODO In there: Start new AsyncTask to get studies from server
            Fragment fragment = new ServerOverviewFragment();

            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

            new StudiesGetter().execute(result);

        }

    }

    private class StudiesGetter extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String result = String.valueOf(params[0]);
            Log.d(TAG,"Setting serverUrl");
            String serverUrl = transmartServer.getServerUrl();
            Log.d(TAG,"serverUrl: " + serverUrl);

            String access_token = "";

            try {
                JSONObject jObject = new JSONObject(result);
                access_token = jObject.getString("access_token");
                String refresh_token = jObject.getString("refresh_token");
                Log.i(TAG,"access_token : " + access_token);
                Log.i(TAG,"refresh_token : " + refresh_token);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String query = serverUrl + "/"
                    + "studies"
                    ;

            Log.v(TAG, "Sending query: [" + query + "].");

            DefaultHttpClient httpClient = new DefaultHttpClient();

            HttpGet httpGet = new HttpGet(query);
            httpGet.addHeader("Authorization","Bearer " + access_token);

            String responseLine;
            StringBuilder responseBuilder = new StringBuilder();
            String queryResult = "";
            try {
                HttpResponse response = httpClient.execute(httpGet);
                Log.i(TAG,"Statusline : " + response.getStatusLine());

                InputStream data = response.getEntity().getContent();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(data));

                while ((responseLine = bufferedReader.readLine()) != null) {
                    responseBuilder.append(responseLine);
                }
                queryResult = responseBuilder.toString();
                Log.i(TAG,"Response : " + queryResult);

            } catch (IOException e) {
                e.printStackTrace();
            }

            // TODO cache list of studies
            // Display list of studies in ListView



            return queryResult;
        }

        @Override
        protected void onPostExecute(String queryResult) {
            super.onPostExecute(queryResult);

            try {
                JSONArray jArray = new JSONArray(queryResult);
                Log.i(TAG,jArray.toString());


                final ArrayList<String> studyList = new ArrayList<String>();
                for(int i = 0; i < jArray.length(); i++){
                    JSONObject study = jArray.getJSONObject(i);
                    String studyId = study.getString("id");
                    Log.i(TAG,"Study: "+studyId);
                    studyList.add(studyId);
                }

                ListView studyListView = (ListView) findViewById(R.id.studyList);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
                        R.layout.study_item, R.id.studyName, studyList);
                studyListView.setAdapter(adapter);

            } catch (JSONException e) {
                Log.i(TAG,"Couldn't parse to JSON: " + queryResult);
                e.printStackTrace();
            }

        }
    }

    public static class ServerOverviewFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_serveroverview, container, false);

            getActivity().setTitle(R.string.studies);
            return rootView;
        }
    }

    public static class AddNewServerFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_addnewserver, container, false);

            getActivity().setTitle(R.string.addnewserver);
            return rootView;
        }
    }


}
