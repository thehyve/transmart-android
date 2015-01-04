package nl.thehyve.transmartclient;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by Ward Weistra on 01-12-14.
 */
public class TransmartServer implements Parcelable {
    private static final String TAG = "TransmartServer";

    String serverUrl;

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    String access_token;
    String refresh_token;
    String prettyName;

    public TransmartServer() {
        this.serverUrl = "";
        this.access_token = "";
        this.refresh_token = "";
        this.prettyName = "";
        Log.d(TAG, "transmartServer has been instantiated from scratch.");
    }

    public TransmartServer(Parcel in) {
        this.serverUrl = in.readString();
        this.access_token = "";
        this.refresh_token = "";
        this.prettyName = "";
        Log.d(TAG, "transmartServer has been instantiated from parcel.");
        Log.d(TAG, "Server URL: "+ serverUrl);
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        Log.d(TAG,"Set serverUrl to " + serverUrl);
    }

    public String getServerUrl() {
        Log.d(TAG,"Asked for serverUrl: " + serverUrl);
        return serverUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(serverUrl);
        Log.d(TAG, "transmartServer has been written to parcel.");
        Log.d(TAG, "Server URL: "+ serverUrl);
//        out.writeString(access_token);
//        out.writeString(refresh_token);
//        out.writeString(prettyName);
    }
}
