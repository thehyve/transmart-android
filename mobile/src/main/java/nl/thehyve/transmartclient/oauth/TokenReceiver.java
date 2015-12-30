package nl.thehyve.transmartclient.oauth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import nl.thehyve.transmartclient.rest.ServerResult;

/**
 * Created by Ward Weistra on 13-10-15.
 * Copyright (c) 2015 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */
public class TokenReceiver extends BroadcastReceiver {

    private static final String TAG = "TokenReceiver";
    TokenReceivedListener mListener = null;
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            ServerResult serverResult = extras.getParcelable("serverResult");
            boolean userefreshtoken = extras.getBoolean("userefreshtoken");
            Log.d(TAG, " Token received in serverResult = " + serverResult);
            this.mListener.onTokenReceived(serverResult, userefreshtoken);
        }
    }

    public void setTokenReceivedListener(TokenReceivedListener mListener){
        this.mListener=mListener;
    }

    public interface TokenReceivedListener {
        void onTokenReceived(ServerResult serverResult, boolean userefreshtoken);
    }
}
