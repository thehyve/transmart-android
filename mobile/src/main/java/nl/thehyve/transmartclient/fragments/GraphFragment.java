package nl.thehyve.transmartclient.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.thehyve.transmartclient.R;
import nl.thehyve.transmartclient.apiItems.Concept;
import nl.thehyve.transmartclient.apiItems.Observation;
import nl.thehyve.transmartclient.apiItems.Study;
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
    private static final String ARG_TRANSMARTSERVER = "transmartServer";
    private static final String TAG = "GraphFragment";

    private String studyId;
    private Study study;
    private TransmartServer transmartServer;
    private View rootView;

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
        // TODO accept Study instead of StudyID
        args.putString(ARG_STUDYID, studyId);
        args.putParcelable(ARG_TRANSMARTSERVER, transmartServer);
        fragment.setArguments(args);
        return fragment;
    }

    public GraphFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        list = new ArrayList<>();
        cda = new ChartDataAdapter(getActivity(), list);

        if (getArguments() != null) {
            studyId = getArguments().getString(ARG_STUDYID);
            transmartServer = getArguments().getParcelable(ARG_TRANSMARTSERVER);

            assert transmartServer != null;
            if (transmartServer.getConnectionStatus() == TransmartServer.ConnectionStatus.CONNECTED) {
                new ConceptsGetter().execute();
            } else {
                restInteractionListener.notConnectedYet(transmartServer);
            }
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
        mListener.setToggleState(false);

        rootView = inflater.inflate(R.layout.fragment_graph, container, false);

        ListView lv = (ListView) rootView.findViewById(R.id.graphList);
        lv.setAdapter(cda);

        // Inflate the layout for this fragment
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
        restInteractionListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListener.setToggleState(true);
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
        void setToggleState(boolean isEnabled);
    }

    private class ConceptsGetter extends AsyncTask<Void, Void, ServerResult> {

        @Override
        protected ServerResult doInBackground(Void... params) {

            ServerResult serverResult = new ServerResult();

            String serverUrl    = transmartServer.getServerUrl();
            String access_token = transmartServer.getAccess_token();

            String query = serverUrl + "/"
                    + "studies/"
                    + studyId + "/"
                    + "concepts/"
                    ;

            Log.v(TAG, "Sending query: [" + query + "].");

            return serverResult.getServerResult(access_token, query);
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
                    restInteractionListener.authorizationLost(transmartServer);
                }
            } else if (serverResult.getResponseCode() == 0) {
                if (restInteractionListener != null) {
                    restInteractionListener.connectionLost(transmartServer);
                }
            } else {
                String message = String.format(getString(R.string.server_responded_with),
                        serverResult.getResponseCode(),
                        serverResult.getResponseDescription());
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                        .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                            }
                        }).show();
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
            String conceptLink = TextUtils.join("/", fullName);
            Log.d(TAG,"conceptLink: " + conceptLink);
            ServerResult serverResult = new ServerResult();

            String serverUrl    = transmartServer.getServerUrl();
            String access_token = transmartServer.getAccess_token();

            String query = serverUrl + "/"
                    + "studies/"
                    + studyId + "/"
                    + "concepts/"
                    + conceptLink + "/"
                    + "observations/"
                    ;

            Log.v(TAG, "Sending query: [" + query + "].");

            return serverResult.getServerResult(access_token, query);
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
                    restInteractionListener.authorizationLost(transmartServer);
                }
            } else if (serverResult.getResponseCode() == 0) {
                if (restInteractionListener != null) {
                    restInteractionListener.connectionLost(transmartServer);
                }
            } else {
                if (isAdded()) {
                    String message = String.format(getString(R.string.server_responded_with),
                            serverResult.getResponseCode(),
                            serverResult.getResponseDescription());
                    Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                            .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            }).show();
                }
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
