package nl.thehyve.transmartclient.rest;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.List;

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
    String serverLabel;
    int menuItemID;
    ConnectionStatus connectionStatus;
    boolean wasConnected;

    public enum ConnectionStatus {
        ABANDONED            (false),
        SENTTOURL            (true ),
        CODERECEIVED         (true ),
        CONNECTED            (false),
        ACCESSTOKENEXPIRED   (false),
        CODERECEIVEDRECONNECT(true ),
        REFRESHTOKENEXPIRED  (false);

        private final boolean unique;

        ConnectionStatus(boolean unique) {
            this.unique = unique;
        }

        public boolean isUnique() {
            return unique;
        }
    }

    // Standard basic constructor for non-parcel object creation
    public TransmartServer() {
        this.serverUrl = "";
        this.access_token = "";
        this.refresh_token = "";
        this.serverLabel = "";
        this.connectionStatus = null;
        this.wasConnected = false;
    }
    // Constructor to use when re-constructing object from a parcel
    public TransmartServer(Parcel in) {
        readFromParcel(in);
    }

    // Method to write the data to a Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(serverUrl);
        out.writeString(access_token);
        out.writeString(refresh_token);
        out.writeString(serverLabel);
        out.writeInt(menuItemID);
        out.writeSerializable(connectionStatus);
        out.writeByte((byte) (wasConnected ? 1 : 0));
    }

    // Method to read the data from a Parcel
    private void readFromParcel(Parcel in) {
        this.serverUrl = in.readString();
        this.access_token = in.readString();
        this.refresh_token = in.readString();
        this.serverLabel = in.readString();
        this.menuItemID = in.readInt();
        this.connectionStatus = (ConnectionStatus) in.readSerializable();
        this.wasConnected = in.readByte() == 1;
    }

    // Getters
    public String getAccess_token() {
        return access_token;
    }
    public String getRefresh_token() {
        return refresh_token;
    }
    public String getServerUrl() {
        return serverUrl;
    }
    public String getServerLabel() {
        return serverLabel;
    }
    public int getMenuItemID() {
        return menuItemID;
    }
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }
    public boolean wasConnected() {
        return wasConnected;
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
    }
    public void setServerLabel(String serverLabel) {
        this.serverLabel = serverLabel;
    }
    public void setMenuItemID(int menuItemID) {
        this.menuItemID = menuItemID;
    }

    public void setNonUniqueConnectionStatus(ConnectionStatus connectionStatus) {
        if (connectionStatus.isUnique()) {
            throw new Error("Using setNonUniqueConnectionStatus for unique ConnectionStatus");
        } else {
            this.connectionStatus = connectionStatus;
            if (connectionStatus == ConnectionStatus.CONNECTED) {
                this.wasConnected = true;
            }
        }
    }

    public void setUniqueConnectionStatus(ConnectionStatus connectionStatus, List<TransmartServer> transmartServers) {
        if (connectionStatus.isUnique()) {
            for (TransmartServer transmartServer : transmartServers) {
                if (transmartServer.getConnectionStatus() == connectionStatus && transmartServer != this){
                    throw new Error("Setting unique connection status, but this status has not been made unique");
                }
            }
            this.connectionStatus = connectionStatus;
        } else {
            throw new Error("Using setUniqueConnectionStatus for non-unique ConnectionStatus");
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %s)",serverLabel,serverUrl, hashCode());
    }

    // Other obligatory stuff for Parcelable

    public static final Parcelable.Creator<TransmartServer> CREATOR
            = new Parcelable.Creator<TransmartServer>() {

        public TransmartServer createFromParcel(Parcel in) {
            return new TransmartServer(in);
        }

        public TransmartServer[] newArray(int size) {
            return new TransmartServer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
