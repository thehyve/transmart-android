package nl.thehyve.transmartclient.fragments;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.thehyve.transmartclient.MainActivity;
import nl.thehyve.transmartclient.R;
import nl.thehyve.transmartclient.apiItems.Concept;
import nl.thehyve.transmartclient.apiItems.Observation;
import nl.thehyve.transmartclient.rest.RestInteractionListener;
import nl.thehyve.transmartclient.rest.ServerResult;
import nl.thehyve.transmartclient.rest.TransmartServer;
import nl.thehyve.transmartclient.chartItems.BarChartItem;
import nl.thehyve.transmartclient.chartItems.ChartItem;
import nl.thehyve.transmartclient.chartItems.PieChartItem;

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
    private ChartDataAdapter cda;
    private ArrayList<ChartItem> list;

    Gson gson = new Gson();

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

        list = new ArrayList<ChartItem>();
        cda = new ChartDataAdapter(getActivity(), list);

        if (getArguments() != null) {
            studyId = getArguments().getString(ARG_STUDYID);
            new ConceptsGetter().execute();
        }
    }

    /** adapter that supports 3 different item types */
    private class ChartDataAdapter extends ArrayAdapter<ChartItem> {

        public ChartDataAdapter(Context context, List<ChartItem> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getItem(position).getView(position, convertView, getContext());
        }

        @Override
        public int getItemViewType(int position) {
            // return the views type
            return getItem(position).getItemType();
        }

        @Override
        public int getViewTypeCount() {
            return 3; // we have 3 different item-types
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        getActivity().setTitle(studyId);

        View rootView = inflater.inflate(R.layout.fragment_graph, container, false);

        ListView lv = (ListView) rootView.findViewById(R.id.graphList);
        lv.setAdapter(cda);

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
                JsonParser parser = new JsonParser();
                JsonObject json = (JsonObject) parser.parse(serverResult.getResult());
                JsonArray jArray = json.get("ontology_terms").getAsJsonArray();
                Log.i(TAG, jArray.toString());

                for (int i = 0; i < jArray.size(); i++) {
                    JsonElement conceptJSON = jArray.get(i);
                    Concept concept = gson.fromJson(conceptJSON, Concept.class);
                    String conceptName = concept.getName();
                    String conceptType = concept.getType();
                    Log.i(TAG, "Concept: " + conceptName + " (" + conceptType + ")");
                    if (conceptName.equals("Age")
                            || conceptName.equals("Sex")
                            || conceptName.equals("Gender")
                            || conceptName.equals("Race")) {
                        new ObservationsGetter().execute(concept);
                    }
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

    private class ObservationsGetter extends AsyncTask<Concept, Void, ServerResult> {
        private Concept concept;

        @Override
        protected ServerResult doInBackground(Concept... params) {
            concept = params[0];
            String[] fullName = concept.getFullName().split("\\\\");
            Log.d(TAG,"fullName: " + fullName);
            fullName = Arrays.copyOfRange(fullName, 3, fullName.length);
            for (int i=0;i<fullName.length;i++) {
                try {
                    fullName[i] = URLEncoder.encode(fullName[i],"utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            String conceptLink = null;
            conceptLink = TextUtils.join("/", fullName);
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
                Observation[] observations = gson.fromJson(serverResult.getResult(),Observation[].class);
                if (concept.getType().equals("NUMERIC")) {
                    new CalculateNumericHistogram().execute(observations);
                } else if (concept.getType().equals("UNKNOWN")) {
                    new CalculateCategoricalPiechart().execute(observations);
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

    private class CalculateNumericHistogram extends AsyncTask<Observation[], Void, BarData> {

        @Override
        protected BarData doInBackground(Observation[]... params) {

            BarData data;

            Observation[] observations = params[0];

            List<Float> valueList = new ArrayList<>();

            for (Observation observation : observations) {
                String valueString = observation.getValue();
                Log.i(TAG, "Value: " + valueString);
                if (valueString != null) {
                    float value = Float.parseFloat(valueString);
                    valueList.add(value);
                }
            }

            Collections.sort(valueList);
            float min = valueList.get(0);
            float max = valueList.get(valueList.size() - 1);
            int nrbins = 10;
            float binwidth = (max-min)/nrbins;
            final int[] result = new int[nrbins];

            for (Observation observation : observations) {
                String valueString = observation.getValue();
                Log.i(TAG, "Value: " + valueString);
                if (valueString != null) {
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

            BarDataSet setComp1 = new BarDataSet(line, "Subset 1");
            setComp1.setAxisDependency(YAxis.AxisDependency.LEFT);

            ArrayList<BarDataSet> dataSets = new ArrayList<>();
            dataSets.add(setComp1);

            ArrayList<String> xVals = new ArrayList<>();

            for (int i = 0; i < nrbins; i++) {
                String xVal = (i*binwidth) + "-" + ((i+1)*binwidth);
                xVals.add(xVal);
            }

            data = new BarData(xVals, dataSets);

            return data;
        }

        protected void onPostExecute(BarData data) {
            super.onPostExecute(data);
            BarChartItem newBarChart = new BarChartItem(data, getActivity());
            list.add(newBarChart);
            cda.notifyDataSetChanged();
        }
    }

    private class CalculateCategoricalPiechart extends AsyncTask<Observation[], Void, PieData> {

        @Override
        protected PieData doInBackground(Observation[]... params) {

            PieData data;

            Observation[] observations = params[0];

            Map<String,Integer> valueMap = new HashMap<>();

            for (Observation observation : observations) {
                String valueString = observation.getValue();
                Log.i(TAG, "Value: " + valueString);
                if (valueString != null) {
                    Integer count = valueMap.get(valueString);
                    if (count != null) {
                        valueMap.put(valueString, count + 1);
                    } else {
                        valueMap.put(valueString, 1);
                    }
                }
            }

            ArrayList<Entry> line = new ArrayList<>();
            ArrayList<String> xVals = new ArrayList<>();

            int j = 0;
            for (Map.Entry<String, Integer> entry : valueMap.entrySet()) {
                line.add(new Entry(entry.getValue(), j));
                xVals.add(j, entry.getKey());
                j++;
            }

            PieDataSet setComp1 = new PieDataSet(line, "Subset 1");
            setComp1.setSliceSpace(2f);
            setComp1.setColors(ColorTemplate.VORDIPLOM_COLORS);


            data = new PieData(xVals, setComp1);

            return data;
        }

        protected void onPostExecute(PieData data) {
            super.onPostExecute(data);
            PieChartItem newPieChart = new PieChartItem(data, getActivity());
            list.add(newPieChart);
            cda.notifyDataSetChanged();
        }
    }

}
