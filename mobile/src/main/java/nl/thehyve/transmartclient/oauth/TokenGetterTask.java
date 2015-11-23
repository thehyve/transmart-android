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

public class TokenGetterTask extends AsyncTask<String, Void, ServerResult> {

    private static final String TAG = "TokenGetterTask";
    public static final String TOKEN_RECEIVED_INTENT = "nl.thehyve.transmartclient.TokenReceived";

    private Context mContext;
    private TransmartServer transmartServer;
    private String client_id;
    private String client_secret;

    public TokenGetterTask(Context mContext, TransmartServer transmartServer,
                           String client_id, String client_secret) {
        this.mContext = mContext;
        this.transmartServer = transmartServer;
        this.client_id = client_id;
        this.client_secret = client_secret;
    }

    @Override
    protected ServerResult doInBackground(String... params) {

        ServerResult serverResult = new ServerResult();

        String code = String.valueOf(params[0]);

        String serverUrl = transmartServer.getServerUrl();

        String query = serverUrl + "/oauth/token?"
                + "grant_type=authorization_code"
                + "&client_id=" + client_id
                + "&client_secret=" + client_secret
                + "&code=" + code
                + "&redirect_uri=" + "transmart://oauthresponse" //CALLBACK_URL
                ;


        Log.v(TAG, "Sending query: [" + query + "].");

        return serverResult.getServerResult(null, query);
    }

    protected void onPostExecute(ServerResult serverResult) {
        super.onPostExecute(serverResult);

        LocalBroadcastManager mBroadcastMgr = LocalBroadcastManager
                .getInstance(this.mContext);
        Intent i = new Intent(TOKEN_RECEIVED_INTENT);
        i.putExtra("serverResult", serverResult);
        mBroadcastMgr.sendBroadcast(i);
    }
}
