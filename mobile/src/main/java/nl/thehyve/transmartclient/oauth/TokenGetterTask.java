package nl.thehyve.transmartclient.oauth;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import nl.thehyve.transmartclient.rest.ServerResult;
import nl.thehyve.transmartclient.rest.TransmartServer;

/**
 * Created by Ward Weistra on 10-10-15.
 * Copyright (c) 2015 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */

public class TokenGetterTask extends AsyncTask<Void, Void, ServerResult> {

    private static final String TAG = "TokenGetterTask";
    public static final String TOKEN_RECEIVED_INTENT = "nl.thehyve.transmartclient.TokenReceived";

    private Context mContext;
    private TransmartServer transmartServer;
    private String client_id;
    private String client_secret;
    private String code;
    private boolean reconnect;

    public TokenGetterTask(Context mContext, TransmartServer transmartServer,
                           String client_id, String client_secret, String code) {
        this.mContext = mContext;
        this.transmartServer = transmartServer;
        this.client_id = client_id;
        this.client_secret = client_secret;
        this.code = code;
        this.reconnect = false;
    }

    public TokenGetterTask(Context mContext, TransmartServer transmartServer,
                           String client_id, String client_secret) {
        this.mContext = mContext;
        this.transmartServer = transmartServer;
        this.client_id = client_id;
        this.client_secret = client_secret;
        this.code = null;
        this.reconnect = true;
    }

    @Override
    protected ServerResult doInBackground(Void... params) {

        ServerResult serverResult = new ServerResult();

        String serverUrl = transmartServer.getServerUrl();

        String query;

        // TODO use was connected instead of reconnect
        if (reconnect) {

            query = serverUrl + "/oauth/token?" +
                    "grant_type=refresh_token" +
                    "&client_id=" + client_id +
                    "&client_secret=" + client_secret +
                    "&refresh_token=" + transmartServer.getRefresh_token() +
                    "&redirect_uri=" + "transmart://oauthresponse" //CALLBACK_URL
                    ;

        } else {

            query = serverUrl + "/oauth/token?"
                    + "grant_type=authorization_code"
                    + "&client_id=" + client_id
                    + "&client_secret=" + client_secret
                    + "&code=" + code
                    + "&redirect_uri=" + "transmart://oauthresponse" //CALLBACK_URL
                    ;
        }

        Log.v(TAG, "Sending query: [" + query + "].");

        return serverResult.getServerResult(null, query);
    }

    protected void onPostExecute(ServerResult serverResult) {
        super.onPostExecute(serverResult);

        Log.d(TAG,"Sending broadcast for serverResult with reconnect = "+reconnect);
        LocalBroadcastManager mBroadcastMgr = LocalBroadcastManager
                .getInstance(this.mContext);
        Intent i = new Intent(TOKEN_RECEIVED_INTENT);
        i.putExtra("serverResult", serverResult);
        i.putExtra("reconnect", reconnect);
        // TODO pass along transmartServer or unique identifier for it
        mBroadcastMgr.sendBroadcast(i);
    }
}
