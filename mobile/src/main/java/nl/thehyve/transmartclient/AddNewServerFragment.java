package nl.thehyve.transmartclient;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by wardweistra on 01-12-14.
 */
public class AddNewServerFragment extends Fragment {
    private static final String TAG = "AddNewServerFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_addnewserver, container, false);

        getActivity().setTitle(R.string.addnewserver);
        return rootView;
    }
}