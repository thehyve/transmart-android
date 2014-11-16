package nl.thehyve.transmartclient;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import net.smartam.leeloo.client.URLConnectionClient;
import net.smartam.leeloo.client.request.OAuthClientRequest;
import net.smartam.leeloo.client.response.OAuthJSONAccessTokenResponse;
import net.smartam.leeloo.common.exception.OAuthProblemException;
import net.smartam.leeloo.common.exception.OAuthSystemException;
import net.smartam.leeloo.client.OAuthClient;
import net.smartam.leeloo.common.message.types.GrantType;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    public static String SERVER_URL = "http://75.124.74.46:5880/transmart";
    public static String OAUTH_URL = SERVER_URL + "/oauth/authorize";
    public static String OAUTH_ACCESS_TOKEN_URL = SERVER_URL + "/oauth/token";

    public static String CLIENT_ID = "api-client";
    public static String CLIENT_SECRET = "api-client";
    public static String CALLBACK_URL = SERVER_URL + "/oauth/verify";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();

        Uri uri = intent.getData();

        if (uri != null && uri.toString()
                .startsWith("transmart://oauthresponse"))
        {
            Log.d("TranSMART","Received uri");
            String code = uri.getQueryParameter("code");
            Toast toast = Toast.makeText(this, "Received OAuth code: " + code, Toast.LENGTH_LONG);
            toast.show();
            Log.d("TranSMART","Received OAuth code: " + code);

//           view.postUrl(OAUTH_ACCESS_TOKEN_URL, query.getBytes());
            new TokenGetterTask().execute(code);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // This is the method that is called when the submit button is clicked

    public void connectToTranSMARTServer(View view) throws OAuthSystemException {

        EditText serverUrlEditText = (EditText) findViewById(R.id.serverUrl);
        String serverUrl = serverUrlEditText.getText().toString();

        OAuthClientRequest request;
        request = OAuthClientRequest
                .authorizationLocation(serverUrl + "/oauth/authorize")
                .setClientId("api-client")
                .setRedirectURI("transmart://oauthresponse")
                .buildQueryMessage();

        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(request.getLocationUri() + "&response_type=code&client_secret=api-client"));
        startActivity(intent);
    }

    private class TokenGetterTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String code = String.valueOf(params[0]);
//
//            OAuthClientRequest request = null;
//
//            try {
//                Log.i(TAG, "Started trying...");
//                request = OAuthClientRequest.tokenLocation(OAUTH_ACCESS_TOKEN_URL)
//                        .setGrantType(GrantType.AUTHORIZATION_CODE)
//                        .setClientId("api-client")
//                        .setClientSecret("api-client")
//                        .setRedirectURI(CALLBACK_URL)
//                        .setCode(code)
//                        .buildBodyMessage();
//                Log.i(TAG, "Request body: " + request.getBody());
//                Log.i(TAG, "Request headers: " + request.getHeaders());
//                Log.i(TAG, "Request location URI: " + request.getLocationUri());
//            } catch (OAuthSystemException e) {
//                e.printStackTrace();
//            }
//
//            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
//
//            OAuthJSONAccessTokenResponse response = null;
//            try {
//                response = oAuthClient.accessToken(request);
//            } catch (OAuthSystemException e) {
//                e.printStackTrace();
//            } catch (OAuthProblemException e) {
//                e.printStackTrace();
//            }
//
//            String token = null;
//            if (response != null) {
//                token = response.getAccessToken();
//                Log.i(TAG,"Response: " + response.toString());
//            } else {
//                Log.i(TAG,"Response is null");
//            }
//
//            Log.i(TAG,"Token: " + token);
//            return token;

            String query = OAUTH_ACCESS_TOKEN_URL + "?"
                    + "grant_type=authorization_code"
                    + "&client_id=" + CLIENT_ID
                    + "&client_secret=" + CLIENT_SECRET
                    + "&code=" + code
                    + "&redirect_uri=" + CALLBACK_URL
                    ;


            Log.v(TAG, "Sending query: [" + query + "].");

            DefaultHttpClient httpClient = new DefaultHttpClient();

            HttpGet httpGet = new HttpGet(query);
//            httpGet.addHeader("accept","application/json");

            String responseLine;
            StringBuilder responseBuilder = new StringBuilder();
            try {
                HttpResponse response = httpClient.execute(httpGet);
                Log.i(TAG,"Statusline : " + response.getStatusLine());
                InputStream data = response.getEntity().getContent();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(data));

                while ((responseLine = bufferedReader.readLine()) != null) {
                    responseBuilder.append(responseLine);
                }
                Log.i(TAG,"Response : " + responseBuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseBuilder.toString();
        }
    }
}
