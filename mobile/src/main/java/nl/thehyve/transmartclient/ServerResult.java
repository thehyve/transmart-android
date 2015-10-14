package nl.thehyve.transmartclient;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by Ward Weistra on 22-12-14.
 * Copyright (c) 2015 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */

public class ServerResult implements Parcelable {

    private static final String TAG = "ServerResult";

    private int responseCode;
    private String responseDescription;
    private String result;

    public ServerResult() {
        this.responseCode = 0;
        this.responseDescription = "";
        this.result = "";
    }

    public ServerResult(int responseCode, String responseDescription, String result) {
        this.responseCode = responseCode;
        this.responseDescription = responseDescription;
        this.result = result;
    }

    public ServerResult(Parcel in) {
        readFromParcel(in);
        Log.d(TAG, "ServerResult has been instantiated from parcel.");
    }

    public static final Creator<ServerResult> CREATOR = new Creator<ServerResult>() {
        @Override
        public ServerResult createFromParcel(Parcel in) {
            return new ServerResult(in);
        }

        @Override
        public ServerResult[] newArray(int size) {
            return new ServerResult[size];
        }
    };

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseDescription() {
        return responseDescription;
    }

    public void setResponseDescription(String responseDescription) {
        this.responseDescription = responseDescription;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Method to write the data to a Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(responseCode);
        out.writeString(responseDescription);
        out.writeString(result);

        Log.d(TAG, "ServerResult has been written to parcel.");
    }

    // Method to read the data from a Parcel
    private void readFromParcel(Parcel in) {
        this.responseCode = in.readInt();
        this.responseDescription = in.readString();
        this.result = in.readString();

        Log.d(TAG, "ServerResult has been read from parcel.");
    }
}
