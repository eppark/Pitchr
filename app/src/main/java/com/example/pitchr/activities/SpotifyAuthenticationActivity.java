package com.example.pitchr.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.parse.ParseUser;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import java.util.Arrays;

public class SpotifyAuthenticationActivity extends AppCompatActivity {

    private static final String TAG = SpotifyAuthenticationActivity.class.getSimpleName();
    private static final String SCOPES = "streaming,user-top-read,app-remote-control";
    private static final int REQUEST_CODE = 11037;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_spotify_authentication);
        boolean returning = getIntent().getBooleanExtra("returning", false);

        // LOG TO ANALYTICS
        ParseApplication.logEvent("spotifyAuthenticationActivity", Arrays.asList("status"), Arrays.asList("success"));

        // If the user is a returning user, we don't need to show the dialog option again. Otherwise, we can show it
        authenticateSpotify(!returning);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    ParseUser.getCurrentUser().put("token", response.getAccessToken());
                    ParseUser.getCurrentUser().saveInBackground();
                    Log.d(TAG, "Successfully got auth token");

                    // LOG TO ANALYTICS
                    ParseApplication.logEvent("spotifyAuthEvent", Arrays.asList("status"), Arrays.asList("success"));

                    // Go to the main activity
                    goMainActivity();
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.d(TAG, "Error authenticating!");

                    // LOG TO ANALYTICS
                    ParseApplication.logEvent("spotifyAuthEvent", Arrays.asList("status"), Arrays.asList("failure"));

                    // Try logging in again
                    retryLogin();
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
                    Log.d(TAG, "Authentication flow cancelled");

                    // LOG TO ANALYTICS
                    ParseApplication.logEvent("spotifyAuthEvent", Arrays.asList("status"), Arrays.asList("cancelled"));

                    // Try logging in again
                    retryLogin();
            }
        }
    }

    // Build the authenticator
    private void authenticateSpotify(boolean dialog) {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(getString(R.string.spotify_api_key), AuthenticationResponse.Type.TOKEN, getString(R.string.redirect_url));
        builder.setScopes(new String[]{SCOPES});
        builder.setShowDialog(dialog);
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    // Go to the main activity
    private void goMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    // Try logging again if we're running into issues
    private void retryLogin() {
        ParseUser.logOut();
        AuthorizationClient.clearCookies(this); // Clear Spotify cookies
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
        finish();
    }
}