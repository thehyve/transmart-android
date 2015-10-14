package nl.thehyve.transmartclient;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by Ward Weistra on 01-12-14.
 * Copyright (c) 2015 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */

public class TransmartServer implements Parcelable {
    private static final String TAG = "TransmartServer";

    String serverUrl;
    String access_token;
    String refresh_token;
    String prettyName;

    // Standard basic constructor for non-parcel object creation
    public TransmartServer() {
        this.serverUrl = "";
        this.access_token = "";
        this.refresh_token = "";
        this.prettyName = "";
        Log.d(TAG, "transmartServer has been instantiated from scratch.");
    }
    // Constructor to use when re-constructing object from a parcel
    public TransmartServer(Parcel in) {
        readFromParcel(in);
        Log.d(TAG, "transmartServer has been instantiated from parcel.");
    }

    // Method to write the data to a Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(serverUrl);
        out.writeString(access_token);
        out.writeString(refresh_token);
        out.writeString(prettyName);

        Log.d(TAG, "transmartServer has been written to parcel.");
        Log.d(TAG, "Server URL: "+ serverUrl);
    }

    // Method to read the data from a Parcel
    private void readFromParcel(Parcel in) {
        this.serverUrl = in.readString();
        this.access_token = in.readString();
        this.refresh_token = in.readString();
        this.prettyName = in.readString();

        Log.d(TAG, "transmartServer has been read from parcel.");
        Log.d(TAG, "Server URL: "+ serverUrl);
    }

    // Getters
    public String getAccess_token() {
        return access_token;
    }
    public String getRefresh_token() {
        return refresh_token;
    }
    public String getServerUrl() {
        Log.d(TAG,"Asked for serverUrl: " + serverUrl);
        return serverUrl;
    }

    // Setters
    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }
    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        Log.d(TAG,"Set serverUrl to " + serverUrl);
    }

    // Other obligatory stuff for Parcelable

    public static final Parcelable.Creator<TransmartServer> CREATOR
            = new Parcelable.Creator<TransmartServer>() {

        public TransmartServer createFromParcel(Parcel in) {
            Log.d(TAG,"Called createFromParcel");
            return new TransmartServer(in);
        }

        public TransmartServer[] newArray(int size) {
            Log.d(TAG,"Called newArray");
            return new TransmartServer[size];
        }
    };

    @Override
    public int describeContents() {
        Log.d(TAG,"Called describeContents");
        return 0;
    }
}
