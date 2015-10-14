package nl.thehyve.transmartclient;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by Ward Weistra on 01-12-14.
 * Copyright (c) 2015 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */

public class ServerOverviewFragment extends Fragment {
    private static final String TAG = "ServerOverviewFragment";
    private OnFragmentInteractionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        // start StudiesGetter here?
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_serveroverview, container, false);

//      TODO Show waiting sign: "Retrieving studies"

        new StudiesGetter().execute();

        getActivity().setTitle(R.string.serverOverview);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        public void authorizationLost();
        public void connectionLost();
    }

    private class StudiesGetter extends AsyncTask<Void, Void, ServerResult> {

        @Override
        protected ServerResult doInBackground(Void... params) {

            ServerResult serverResult = new ServerResult();

            String serverUrl = MainActivity.transmartServer.getServerUrl();
            String access_token = MainActivity.transmartServer.getAccess_token();

            String query = serverUrl + "/"
                    + "studies"
                    ;

            Log.v(TAG, "Sending query: [" + query + "].");

            DefaultHttpClient httpClient = new DefaultHttpClient();

            HttpGet httpGet = new HttpGet(query);
            httpGet.addHeader("Authorization","Bearer " + access_token);

            String responseLine;
            StringBuilder responseBuilder = new StringBuilder();
            String queryResult;
            try {
                HttpResponse response = httpClient.execute(httpGet);

                StatusLine statusLine = response.getStatusLine();
                Log.i(TAG,"Statusline : " + statusLine);
                int statusCode = statusLine.getStatusCode();
                String statusDescription = statusLine.getReasonPhrase();
                serverResult.setResponseCode(statusCode);
                serverResult.setResponseDescription(statusDescription);

                if (statusCode != 200) {
                    return serverResult;
                }

                InputStream data = response.getEntity().getContent();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(data));

                while ((responseLine = bufferedReader.readLine()) != null) {
                    responseBuilder.append(responseLine);
                }
                queryResult = responseBuilder.toString();
                serverResult.setResult(queryResult);
                Log.i(TAG,"Response : " + queryResult);

            } catch (UnknownHostException e){
                serverResult.setResponseCode(0);
                serverResult.setResponseDescription("Make sure that your internet connection " +
                        "is still working.");
            } catch (IOException e) {
                e.printStackTrace();
            }

            return serverResult;
        }

        @Override
        protected void onPostExecute(ServerResult serverResult) {
            super.onPostExecute(serverResult);

            if (serverResult.getResponseCode() == 200) {
                try {
                    JSONArray jArray = new JSONArray(serverResult.getResult());
                    Log.i(TAG, jArray.toString());


                    final ArrayList<String> studyList = new ArrayList<String>();
                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject study = jArray.getJSONObject(i);
                        String studyId = study.getString("id");
                        Log.i(TAG, "Study: " + studyId);
                        studyList.add(studyId);
                    }

                    ListView studyListView = (ListView) getView().findViewById(R.id.studyList);
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),
                            R.layout.study_item, R.id.studyName, studyList);
                    studyListView.setAdapter(adapter);

                } catch (JSONException e) {
                    Log.i(TAG, "Couldn't parse to JSON: " + serverResult.getResult());
                    e.printStackTrace();
                }
            } else if (serverResult.getResponseCode() == 401) {
                if (mListener != null) {
                    mListener.authorizationLost();
                }
            } else if (serverResult.getResponseCode() == 0) {
                if (mListener != null) {
                    mListener.connectionLost();
                }
            } else {
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Server responded with code "
                        + serverResult.getResponseCode() + ": "
                        + serverResult.getResponseDescription(), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }
}