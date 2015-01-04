package nl.thehyve.transmartclient;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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

/**
 * Created by Ward Weistra on 01-12-14.
 */
public class ServerOverviewFragment extends Fragment {
    private static final String TAG = "ServerOverviewFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_serveroverview, container, false);

        new StudiesGetter().execute();

        getActivity().setTitle(R.string.serverOverview);
        return rootView;
    }

    // TODO return
    private class StudiesGetter extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

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

                ListView studyListView = (ListView) getView().findViewById(R.id.studyList);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),
                        R.layout.study_item, R.id.studyName, studyList);
                studyListView.setAdapter(adapter);

            } catch (JSONException e) {
                Log.i(TAG,"Couldn't parse to JSON: " + queryResult);
                e.printStackTrace();
            }

        }
    }
}