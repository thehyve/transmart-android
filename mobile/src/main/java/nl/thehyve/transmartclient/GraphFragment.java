package nl.thehyve.transmartclient;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GraphFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GraphFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GraphFragment extends Fragment {
    private static final String ARG_STUDYID = "studyId";
    private static final String TAG = "GraphFragment";

    private String studyId;

    private OnFragmentInteractionListener mListener;
    private RestInteractionListener restInteractionListener;
    private BarChart mBarChart;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment GraphFragment.
     */

    public static GraphFragment newInstance(String studyId, TransmartServer transmartServer) {
        GraphFragment fragment = new GraphFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STUDYID, studyId);
        fragment.setArguments(args);
        return fragment;
    }

    public GraphFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studyId = getArguments().getString(ARG_STUDYID);
            new ConceptsGetter().execute();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        getActivity().setTitle(studyId);

        View rootView = inflater.inflate(R.layout.fragment_graph, container, false);
        mBarChart = (BarChart) rootView.findViewById(R.id.chart);
        mBarChart.setDescription("Age");
        mBarChart.setNoDataTextDescription("Loading data...");
        mBarChart.setDescriptionPosition(200, 130);
        mBarChart.setDescriptionTextSize(100);

        // Inflate the layout for this fragment
        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
            restInteractionListener = (RestInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener" +
                    " and RestInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    private class ConceptsGetter extends AsyncTask<Void, Void, ServerResult> {

        @Override
        protected ServerResult doInBackground(Void... params) {

            ServerResult serverResult = new ServerResult();

            String serverUrl = MainActivity.transmartServer.getServerUrl();
            String access_token = MainActivity.transmartServer.getAccess_token();

            String query = serverUrl + "/"
                    + "studies/"
                    + studyId + "/"
                    + "concepts/"
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
                    JSONObject json = new JSONObject(serverResult.getResult());
                    JSONArray jArray = json.getJSONArray("ontology_terms");//new JSONArray(serverResult.getResult());
                    Log.i(TAG, jArray.toString());

//                    studyList.clear();

                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject concept = jArray.getJSONObject(i);
                        String conceptName = concept.getString("name");
                        Log.i(TAG, "Concept: " + conceptName);
//                        studyList.add(studyId);
                        if (conceptName.equals("Age")){
                            new ObservationsGetter().execute(concept.getString("fullName"));
                        }
                    }

//                    mAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    Log.i(TAG, "Couldn't parse to JSON: " + serverResult.getResult());
                    e.printStackTrace();
                }
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

    private class ObservationsGetter extends AsyncTask<String, Void, ServerResult> {

        @Override
        protected ServerResult doInBackground(String... params) {

            String[] fullName = params[0].split("\\\\");
            Log.d(TAG,"fullName: " + fullName);
            fullName = Arrays.copyOfRange(fullName, 3, fullName.length);
            String conceptLink = TextUtils.join("/", fullName);
            Log.d(TAG,"conceptLink: " + conceptLink);
            ServerResult serverResult = new ServerResult();

            String serverUrl = MainActivity.transmartServer.getServerUrl();
            String access_token = MainActivity.transmartServer.getAccess_token();

            String query = serverUrl + "/"
                    + "studies/"
                    + studyId + "/"
                    + "concepts/"
                    + conceptLink + "/"
                    + "observations/"
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
                    new CalculateHistogram().execute(jArray);
                } catch (JSONException e) {
                    Log.i(TAG, "Couldn't parse to JSON: " + serverResult.getResult());
                    e.printStackTrace();
                }
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

    private class CalculateHistogram extends AsyncTask<JSONArray, Void, BarData> {

        @Override
        protected BarData doInBackground(JSONArray... params) {

            BarData data = new BarData();

            try {

                JSONArray jArray = params[0];

                List<Float> valueList = new ArrayList<>();

                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject observation = jArray.getJSONObject(i);;
                    String valueString = observation.getString("value");
                    Log.i(TAG, "Value: " + valueString);
                    if (! valueString.equals("null")) {
                        float value = Float.parseFloat(valueString);
                        valueList.add(value);
                    }
                }

                Collections.sort(valueList);
                float min = valueList.get(0);//0;
                float max = valueList.get(valueList.size() - 1);//100;
                int nrbins = 10;
                float binwidth = (max-min)/nrbins;
                final int[] result = new int[nrbins];

                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject observation = jArray.getJSONObject(i);;
                    String valueString = observation.getString("value");
                    Log.i(TAG, "Value: " + valueString);
                    if (! valueString.equals("null")) {
                        float value = Float.parseFloat(valueString);
                        int bin = (int) ((value - min) / binwidth);
                        if (bin >= 0 && bin < nrbins) {
                            result[bin] += 1;
                        }
                    }
                }

                ArrayList<BarEntry> line = new ArrayList<>();

                for (int i=0; i < result.length; i++) {
                    BarEntry newEntry = new BarEntry(result[i], i);
                    line.add(newEntry);
                }

                BarDataSet setComp1 = new BarDataSet(line, studyId);
                setComp1.setAxisDependency(YAxis.AxisDependency.LEFT);

                ArrayList<BarDataSet> dataSets = new ArrayList<>();
                dataSets.add(setComp1);

                ArrayList<String> xVals = new ArrayList<>();

                for (int i = 0; i < nrbins; i++) {
                    String xVal = (i*binwidth) + "-" + ((i+1)*binwidth);
                    xVals.add(xVal);
                }

                data = new BarData(xVals, dataSets);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return data;
        }

        protected void onPostExecute(BarData data) {
            super.onPostExecute(data);
            mBarChart.setData(data);
            mBarChart.invalidate();
        }
    }

}
