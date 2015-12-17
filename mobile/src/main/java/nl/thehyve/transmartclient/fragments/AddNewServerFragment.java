package nl.thehyve.transmartclient.fragments;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import nl.thehyve.transmartclient.R;

/**
 * Created by Ward Weistra on 01-12-14.
 * Copyright (c) 2015 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */

public class AddNewServerFragment extends Fragment {
    private static final String ARG_SERVERURL = "serverUrl";
    private static final String ARG_SERVERLABEL = "serverLabel";
    private static final String ARG_WAITINGFORTOKEN = "waitingForToken";
    private static final String TAG = "AddNewServerFragment";

    private String serverUrl;
    private String serverLabel;
    private boolean waitingForToken;

    public static AddNewServerFragment newInstance(String serverUrl, String serverLabel, boolean waitingForToken) {
        AddNewServerFragment fragment = new AddNewServerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SERVERURL, serverUrl);
        args.putString(ARG_SERVERLABEL, serverLabel);
        args.putBoolean(ARG_WAITINGFORTOKEN, waitingForToken);
        fragment.setArguments(args);
        return fragment;
    }

    public AddNewServerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (getArguments() != null) {
            serverUrl = getArguments().getString(ARG_SERVERURL);
            serverLabel = getArguments().getString(ARG_SERVERLABEL);
            waitingForToken = getArguments().getBoolean(ARG_WAITINGFORTOKEN);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_addnewserver, container, false);
        getActivity().setTitle(R.string.addnewserver);

        final EditText serverUrlField = (EditText) rootView.findViewById(R.id.serverUrlField);
        final EditText serverLabelField = (EditText) rootView.findViewById(R.id.serverLabelField);
        final Button connect_button = (Button) rootView.findViewById(R.id.connect_button);

        if (serverUrl != null || serverLabel != null) {
            serverUrlField.setText(serverUrl);
            serverLabelField.setText(serverLabel);
        }

        if (waitingForToken) {
            // TODO show loading sign
            serverUrlField.setEnabled(false);
            serverLabelField.setEnabled(false);
            connect_button.setEnabled(false);
        }

        LinearLayout exampleContainer = (LinearLayout) rootView.findViewById(R.id.exampleContainer);
        exampleContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverUrlField.setText(getString(R.string.urlExample));
                serverLabelField.setText(getString(R.string.labelExample));
            }
        });

        serverLabelField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    connect_button.performClick();
                    return true;
                }
                return false;
            }
        });

//      TODO Set menuitem clicked from here

        return rootView;
    }
}