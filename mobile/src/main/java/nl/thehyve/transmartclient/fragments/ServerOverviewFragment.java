package nl.thehyve.transmartclient.fragments;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;

import nl.thehyve.transmartclient.MainActivity;
import nl.thehyve.transmartclient.R;
import nl.thehyve.transmartclient.apiItems.Study;
import nl.thehyve.transmartclient.rest.RestInteractionListener;
import nl.thehyve.transmartclient.rest.ServerResult;

/**
 * Created by Ward Weistra on 01-12-14.
 * Copyright (c) 2015 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */

public class ServerOverviewFragment extends Fragment implements ListView.OnItemClickListener {
    private static final String TAG = "ServerOverviewFragment";
    private OnFragmentInteractionListener mListener;
    private RestInteractionListener restInteractionListener;
    private ArrayAdapter mAdapter;
    private ArrayList<String> studyList;

    Gson gson = new Gson();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        studyList = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.study_item, R.id.studyName, studyList);

        // start StudiesGetter here?
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_serveroverview, container, false);

        // Set the adapter
        ListView mListView = (ListView) rootView.findViewById(R.id.studyList);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

//      TODO Show waiting sign: "Retrieving studies"

        new StudiesGetter().execute();

        getActivity().setTitle(R.string.serverOverview);
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
            restInteractionListener = (RestInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener" +
                    " and RestInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String studyId = (String) parent.getItemAtPosition(position);
        mListener.onStudyClicked(studyId);
    }

    public interface OnFragmentInteractionListener {
        void onStudyClicked(String studyId);
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

            return serverResult.getServerResult(access_token, query);
        }

        @Override
        protected void onPostExecute(ServerResult serverResult) {
            super.onPostExecute(serverResult);

            if (serverResult.getResponseCode() == 200) {
                JsonParser parser = new JsonParser();
                JsonArray jArray;
                try {
                    // Return for newer APIs should be a JsonObject with a studies JsonArray in it
                    JsonObject json = (JsonObject) parser.parse(serverResult.getResult());
                    jArray = json.get("studies").getAsJsonArray();
                } catch (java.lang.ClassCastException e1){
                    try {
                        // Return for older APIs should be a JsonArray directly
                        jArray = (JsonArray) parser.parse(serverResult.getResult());
                    } catch (java.lang.ClassCastException e2){
                        throw new ClassCastException("Studies call doesn't return JsonObject or JsonArray");
                    }
                }

                Log.i(TAG, jArray.toString());

                studyList.clear();

                for (int i = 0; i < jArray.size(); i++) {
                    JsonElement studyJSON = jArray.get(i);
                    Study study = gson.fromJson(studyJSON, Study.class);
                    String studyId = study.getId();
                    String studyName = study.getOntologyTerm().getName();
                    Log.i(TAG, "Study: " + studyName + " (" + studyId + ")");
                    studyList.add(studyId);
                }

                mAdapter.notifyDataSetChanged();
            } else if (serverResult.getResponseCode() == 401) {
                if (restInteractionListener != null) {
                    restInteractionListener.authorizationLost();
                }
            } else if (serverResult.getResponseCode() == 0) {
                if (restInteractionListener != null) {
                    restInteractionListener.connectionLost();
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