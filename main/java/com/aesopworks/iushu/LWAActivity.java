package com.aesopworks.iushu;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.DefaultSyncCallback;
import com.amazonaws.mobileconnectors.s3.transfermanager.PersistableTransfer;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitosync.model.Dataset;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.Constants;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LWAActivity extends AppCompatActivity {

    private TextView acode;
    private TextView clientid;
    private TextView redirecturi;
    private TextView rftkn;
    private TextView actkn;
    private TextView eival;
    private CheckBox autoacstkn;
    private URL url = null;
    private HttpURLConnection urlConnection = null;
    private BufferedOutputStream out = null;
    private InputStream in = null;
    private OutputStream os;
    int resCode = -1;
    private String query;
    private Uri.Builder builder;
    private String atvl;
    private String rtvl;
    private String ttvl;
    private String eivl;
    private String Authcode;
    private String Clntid;
    private String Rdruri;
    private String Codevrfr;
    private String Rfshtkn;
    private String Acsstkn;
    private String drsUrl;
    private Button mGNATButton;
    private Button mUPLDButton;
    public static final String MyPREFERENCES = "MyPrefs";
    public static final String Rftkn = "refreshtokenKey";
    public static final String Actkn = "accesstokenKey";
    public static final String DRSURL = "drsUrlKey";
    AmazonS3 s3;
    TransferUtility transferUtility;
    File fileToUpload = new File("/data/user/0/com.aesopworks.iushu/shared_prefs/MyPrefs.xml");
    SharedPreferences sharedpreferences;
    private ProgressDialog progressDialog;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lwa);

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        Authcode = sharedpreferences.getString("AcodeKey", "");
        Clntid = sharedpreferences.getString("clntidKey", "");
        Rdruri = sharedpreferences.getString("rdruriKey", "");
        Codevrfr = sharedpreferences.getString("codeverifierKey", "");
        Rfshtkn = sharedpreferences.getString("refreshtokenKey", null);
        Acsstkn = sharedpreferences.getString("accesstokenKey", null);


        acode = (TextView) findViewById(R.id.authCode);
        acode.setText(Authcode);
        clientid = (TextView) findViewById(R.id.clntId);
        clientid.setText(Clntid);
        redirecturi = (TextView) findViewById(R.id.rdrUri);
        redirecturi.setText(Rdruri);

        if (Rfshtkn != null & Acsstkn != null) {
            rftkn = (TextView) findViewById(R.id.rftkn);
            assert rftkn != null;
            rftkn.setText(Rfshtkn);
            actkn = (TextView) findViewById(R.id.actkn);
            assert actkn != null;
            actkn.setText(Acsstkn);

        } else {
            new access_refresh_tkn().execute();
        }
        Toolbar myToolbar = (Toolbar) findViewById(R.id.lbl_toolbar);
        setSupportActionBar(myToolbar);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                WebView myWebView = (WebView) findViewById(R.id.drswebView);
                assert myWebView != null;
                myWebView.loadUrl(sharedpreferences.getString("drsUrlKey", ""));
                // User chose the "Settings" item, show the app settings UI...
                return true;

            case R.id.action_upload:
            // User chose the "Favorite" action, mark the current item
            // as a favorite...
                credentialsProvider();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "LWA Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.aesopworks.iushu/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "LWA Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.aesopworks.iushu/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

 //Get Refresh Token and Access Token for the first time using Authorization Code
    private class access_refresh_tkn extends AsyncTask<Void, Void, String>
    {
        protected void onPreExecute() {
            //display progress dialog.
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                url = new URL("https://api.amazon.com/auth/o2/token");
                urlConnection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String responseContent = null;
            if (urlConnection != null) {
                try {
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    os = urlConnection.getOutputStream();
                    writeStream(os);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    resCode = urlConnection.getResponseCode();
                    if (resCode == HttpURLConnection.HTTP_OK) {
                        in = new BufferedInputStream(urlConnection.getInputStream());
                        responseContent = readStream(in);
                    } else {
                        in = urlConnection.getErrorStream();
                        responseContent = readStream(in);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                }
            }
            return responseContent;
        }

        protected void onPostExecute(String result) {
            // dismiss progress dialog and update ui
            onResponseReceived(result);
        }

        private void writeStream(OutputStream out) {
            builder = new Uri.Builder()
                    .appendQueryParameter("grant_type", "authorization_code")
                    .appendQueryParameter("code", Authcode)
                    .appendQueryParameter("client_id", Clntid)
                    .appendQueryParameter("redirect_uri", Rdruri)
                    .appendQueryParameter("code_verifier", Codevrfr);
            query = builder.build().getEncodedQuery();

            try {
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private String readStream(InputStream in) throws IOException {
            InputStreamReader isw = new InputStreamReader(in);
            BufferedReader brin = new BufferedReader(isw);
            String inputLine;
            StringBuilder responsein1 = new StringBuilder();

            while ((inputLine = brin.readLine()) != null) {
                responsein1.append(inputLine);
            }
            brin.close();
            return responsein1.toString();
        }

    }
// Post Execute of Amazon Refresh Token
    private void onResponseReceived(String result) {

        // TODO Auto-generated method stub
        // Create a new JSONObject to hold the access token and extract
        // the token from the response.
        try {
            JSONObject parsedObject = new JSONObject(result);
            rtvl = parsedObject.getString("refresh_token");
            atvl = parsedObject.getString("access_token");

            rftkn = (TextView) findViewById(R.id.rftkn);
            assert rftkn != null;
            rftkn.setText(rtvl);
            actkn = (TextView) findViewById(R.id.actkn);
            assert actkn != null;
            actkn.setText(atvl);
            eival = (TextView) findViewById(R.id.eival);
            autoacstkn = (CheckBox) findViewById(R.id.cb_accesstkn);
            assert eival != null;
            Long cdtime = Long.valueOf(parsedObject.getString("expires_in"));
            cdtime = cdtime * 10;
            CountDownTimer expires_in = new CountDownTimer(cdtime, 1000) {

                public void onTick(long millisUntilFinished) {
                    eival.setText(String.format(getString(R.string.expires_in), millisUntilFinished / 1000));
                }

                public void onFinish() {
                    if (autoacstkn.isChecked()) {
                        new get_new_access_token().execute();
                    } else {
                        actkn.setText(R.string.alert_actkn);
                    }
                }
            }.start();
            mGNATButton = (Button) findViewById(R.id.gnat_btn);
            mGNATButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new get_new_access_token().execute();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        drsUrl = "https://drs-web.amazon.com/settings?access_token=" + atvl + "&exitUri=amzn//com.aesopworks.iushu";
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(Rftkn, rtvl);
        editor.putString(Actkn, atvl);
        editor.putString(DRSURL, drsUrl);
        editor.apply();
    }

    // Post Execute of Amazon Access Token
    private void onactknResponseReceived(String result) {

        // TODO Auto-generated method stub
        // Create a new JSONObject to hold the access token and extract
        // the token from the response.
        try {
            JSONObject parsedObject = new JSONObject(result);
            rtvl = parsedObject.getString("refresh_token");
            atvl = parsedObject.getString("access_token");

            rftkn = (TextView) findViewById(R.id.rftkn);
            assert rftkn != null;
            rftkn.setText(rtvl);
            actkn = (TextView) findViewById(R.id.actkn);
            assert actkn != null;
            actkn.setText(atvl);
            eival = (TextView) findViewById(R.id.eival);
            assert eival != null;
            autoacstkn = (CheckBox) findViewById(R.id.cb_accesstkn);
            assert eival != null;
            Long cdtime = Long.valueOf(parsedObject.getString("expires_in"));
            cdtime = cdtime * 10;
            CountDownTimer expires_in = new CountDownTimer(cdtime, 1000) {
                public void onTick(long millisUntilFinished) {
                    eival.setText(String.format(getString(R.string.expires_in), millisUntilFinished / 1000));
                }
                public void onFinish() {
                    if (autoacstkn.isChecked()) {
                        new get_new_access_token().execute();
                    } else {
                        actkn.setText(R.string.alert_actkn);
                    }
                }
            }.start();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        drsUrl = "https://drs-web.amazon.com/settings?access_token=" + atvl + "&exitUri=amzn//com.aesopworks.iushu";
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(Rftkn, rtvl);
        editor.putString(Actkn, atvl);
        editor.putString(DRSURL, drsUrl);
        editor.apply();
    }

    //Get Access Token from the Refresh Token
    private class get_new_access_token extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                url = new URL("https://api.amazon.com/auth/o2/token");
                urlConnection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String responseContent = null;
            if (urlConnection != null) {
                try {
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    os = urlConnection.getOutputStream();
                    writeStream(os);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    resCode = urlConnection.getResponseCode();
                    if (resCode == HttpURLConnection.HTTP_OK) {
                        in = new BufferedInputStream(urlConnection.getInputStream());
                        responseContent = readStream(in);
                    } else {
                        in = urlConnection.getErrorStream();
                        responseContent = readStream(in);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                }
            }
            return responseContent;
        }

        protected void onPostExecute(String result) {
            // dismiss progress dialog and update ui
            onactknResponseReceived(result);
        }

        private void writeStream(OutputStream out) {
            builder = new Uri.Builder()
                    .appendQueryParameter("grant_type", "refresh_token")
                    .appendQueryParameter("refresh_token", sharedpreferences.getString("refreshtokenKey", null))
                    .appendQueryParameter("client_id", Clntid);
            query = builder.build().getEncodedQuery();

            try {
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private String readStream(InputStream in) throws IOException {
            InputStreamReader isw = new InputStreamReader(in);
            BufferedReader brin = new BufferedReader(isw);
            String inputLine;
            StringBuilder responsein1 = new StringBuilder();

            while ((inputLine = brin.readLine()) != null) {
                responsein1.append(inputLine);
            }
            brin.close();
            return responsein1.toString();
        }
    }


    //// Storing data to Cognito Sync

     public void credentialsProvider(){

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                com.aesopworks.iushu.Constants.COGNITO_POOL_ID, // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        syncdata(credentialsProvider);
    }

    // Storing data to Cognito Sync
    public void syncdata(CognitoCachingCredentialsProvider credentialsProvider)
    {
        CognitoSyncManager syncClient = new CognitoSyncManager(
                getApplicationContext(),
                Regions.US_EAST_1,
                credentialsProvider);

        // Create a record in a dataset and synchronize with the server
        com.amazonaws.mobileconnectors.cognito.Dataset dataset = syncClient.openOrCreateDataset("myDataset");
        dataset.put("AcodeKey", Authcode);
        dataset.put("clntidKey", Clntid);
        dataset.put("rdruriKey", Rdruri);
        dataset.put("refreshtokenKey", sharedpreferences.getString("refreshtokenKey", null));
        dataset.put("accesstokenKey", sharedpreferences.getString("accesstokenKey", null));
        dataset.synchronize(new DefaultSyncCallback() {
            //@Override
            public void onSuccess(Dataset dataset, List newRecords) {
                //Your handler code here
                progressDialog = ProgressDialog.show(getApplicationContext(), "", "Successfully uploaded");
            }
        });
    }

}
