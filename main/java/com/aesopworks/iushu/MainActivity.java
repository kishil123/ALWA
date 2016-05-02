package com.aesopworks.iushu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;
import com.amazon.identity.auth.device.dataobject.AuthorizationToken;
import com.amazon.identity.auth.device.shared.APIListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Array;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private Button mLoginButton;
    private AmazonAuthorizationManager mAuthManager;
    private String authorizationCode = null;
    private String clientId = null;
    private String redirectUri = null;
    private EditText dvcmdl;
    private EditText dsnno;

    private String codeChallenge = null;
    private URL url = null;
    private HttpURLConnection urlConnection = null;
    private BufferedOutputStream out = null;
    private InputStream in = null;
    private OutputStream os;
    int resCode = -1;
    private String query;
    private Uri.Builder builder;
    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String Name = "nameKey";
    public static final String Email = "emailKey";
    public static final String Account = "accountKey";
    public static final String Zipcode = "zipcodeKey";
    public static final String Acode = "AcodeKey";
    public static final String Clientid = "clntidKey";
    public static final String Redirecturi = "rdruriKey";
    public static final String Codeverifier = "codeverifierKey";
    SharedPreferences sharedpreferences;

    //Generating Code Challenge
    private String codeVerifier = generateCodeVerifier();

    private String generateCodeVerifier() {
        byte[] randomOctetSequence = generateRandomOctetSequence();
        String codeVerifier = base64UrlEncode(randomOctetSequence);
        return codeVerifier;
    }

    private byte[] generateRandomOctetSequence() {
        SecureRandom random = new SecureRandom();
        byte[] octetSequence = new byte[32];
        random.nextBytes(octetSequence);
        return octetSequence;
    }

    private String generateCodeChallenge(String codeVerifier, String codeChallengeMethod) throws NoSuchAlgorithmException {
        String codeChallenge;
        if ("S256".equalsIgnoreCase(codeChallengeMethod)) {
            codeChallenge = base64UrlEncode(
                    MessageDigest.getInstance("SHA-256").digest(
                            codeVerifier.getBytes()));
        } else { //Fall back to code_challenge_method = "plain"
            codeChallenge = codeVerifier;
        }
        return codeChallenge;
    }

    private String base64UrlEncode(byte[] arg) {
        return Base64.encodeToString(arg, Base64.NO_PADDING | Base64.URL_SAFE | Base64.NO_WRAP);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        try
        {
            codeChallenge = generateCodeChallenge(codeVerifier, "S256");
        } catch (Exception e) {
            //print some thing
        }

        mAuthManager = new AmazonAuthorizationManager(this, Bundle.EMPTY);
        final Bundle options = new Bundle();

        setContentView(R.layout.activity_main);

        // Find the button with the login_with_amazon ID
        // and set up a click handler
        mLoginButton = (Button) findViewById(R.id.login_with_amazon);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dvcmdl = (EditText) findViewById(R.id.devmdlText);
                dsnno = (EditText) findViewById(R.id.dsnText);
                String dvcmdlstr = dvcmdl != null ? dvcmdl.getText().toString() : "iUSHUMDL01";
                String dsnstr = dsnno != null ? dsnno.getText().toString() : "1234";

                options.putString(AuthzConstants.BUNDLE_KEY.SCOPE_DATA.val,
                        "{\"dash:replenish\":{\"device_model\":\"" +
                                dvcmdlstr +
                                "\", \"serial\":\"" +
                                dsnstr +
                                "\", \"is_test \":\"true\"} }");

                // These 3 steps are important to get the Authorization code from Amazon
                options.putBoolean(AuthzConstants.BUNDLE_KEY.GET_AUTH_CODE.val, true);
                options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE.val,
                        codeChallenge);
                options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE_METHOD.val,
                        "S256");
                mAuthManager.authorize(
                        new String[]{"dash:replenish", "postal_code"}, options, new AuthorizeListener());


            }
        });
    }

    private class AuthorizeListener implements AuthorizationListener{

        /* Authorization was completed successfully. */
        @Override
        public void onSuccess(Bundle response) {
            mAuthManager.getProfile(new ProfileListener());

            // Communicate the code, clientId and redirectUri back to the
            // actual device that make calls for code to token exchange.

            authorizationCode =
                    response.getString(AuthzConstants.BUNDLE_KEY.AUTHORIZATION_CODE.val);
            try {
                clientId = mAuthManager.getClientId();
            } catch (AuthError authError) {
                authError.printStackTrace();
            }
            try {
                redirectUri = mAuthManager.getRedirectUri();
            } catch (AuthError authError) {
                authError.printStackTrace();
            }

            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(Acode, authorizationCode);
            editor.putString(Clientid, clientId);
            editor.putString(Redirecturi, redirectUri);
            editor.putString(Codeverifier, codeVerifier);
            //removing accesstoken to get a new one
            editor.remove("accesstokenKey");
            editor.apply();
            Intent intent = new Intent(MainActivity.this, LWAActivity.class);
            startActivity(intent);
        }
        /* There was an error during the attempt to authorize the application. */
        @Override
        public void onError(AuthError ae) {
            /* Inform the user of the error */
            String errorResponse = ae.getMessage();
            Log.e(TAG, "AuthError during authorization", ae);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showAuthToast("Error during authorization.  Please try again.");
                }
            });
        }
        /* Authorization was cancelled before it could be completed. */
        @Override
        public void onCancel(Bundle cause) {
            /* reset the UI to a ready-to-login state */
            Log.e(TAG, "User cancelled authorization");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showAuthToast("Authorization cancelled");
                }
            });
        }
    }

    private void showAuthToast(String authToastMessage){
        Toast authToast = Toast.makeText(getApplicationContext(), authToastMessage, Toast.LENGTH_LONG);
        authToast.setGravity(Gravity.CENTER, 0, 0);
        authToast.show();
    }

    private class ProfileListener implements APIListener{

        /* getProfile completed successfully. */
        @Override
        public void onSuccess(Bundle response) {
            // Retrieve the data we need from the Bundle
            Bundle profileBundle = response.getBundle(
                    AuthzConstants.BUNDLE_KEY.PROFILE.val);
            assert profileBundle != null;
            final String uname = profileBundle.getString(
                    AuthzConstants.PROFILE_KEY.NAME.val);
            final String uemail = profileBundle.getString(
                    AuthzConstants.PROFILE_KEY.EMAIL.val);
            final String uaccount = profileBundle.getString(
                    AuthzConstants.PROFILE_KEY.USER_ID.val);
            final String uzipcode = profileBundle.getString(
                    AuthzConstants.PROFILE_KEY.POSTAL_CODE.val);

            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString(Name, uname);
            editor.putString(Email, uemail);
            editor.putString(Account, uaccount);
            editor.putString(Zipcode, uzipcode);
            editor.apply();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });
        }
        /* There was an error during the attempt to get the profile. */
        @Override
        public void onError(AuthError ae) {
            /* Retry or inform the user of the error */
        }
    }

    private class TokenListener implements APIListener{

        /* getToken completed successfully. */
        @Override
        public void onSuccess(Bundle response) {
            final String authzToken =
                    response.getString(AuthzConstants.BUNDLE_KEY.TOKEN.val);
            if (!TextUtils.isEmpty(authzToken))
            {
                // Retrieve the profile data
                mAuthManager.getProfile(new ProfileListener());
            }
        }
        /* There was an error during the attempt to get the token. */
        @Override
        public void onError(AuthError ae) {
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        mAuthManager.getToken(new String []{"profile","postal_code"}, new TokenListener());
    }
}


