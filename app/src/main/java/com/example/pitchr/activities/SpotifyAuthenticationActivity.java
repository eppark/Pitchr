package com.example.pitchr.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.example.pitchr.R;
import com.parse.ParseUser;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class SpotifyAuthenticationActivity extends AppCompatActivity {

    private static final String TAG = SpotifyAuthenticationActivity.class.getSimpleName();
    private static final String SCOPES = "streaming,user-top-read,app-remote-control";
    private static final int REQUEST_CODE = 11037;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_spotify_authentication);

        authenticateSpotify();
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
                    goMainActivity();
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.d(TAG, "Error authenticating!");
                    Toast.makeText(SpotifyAuthenticationActivity.this, response.getError(), Toast.LENGTH_SHORT).show();
                    authenticateSpotify();
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
                    Log.d(TAG, "Authentication flow cancelled");
                    Toast.makeText(SpotifyAuthenticationActivity.this, "Authentication flow cancelled!", Toast.LENGTH_SHORT).show();
                    authenticateSpotify();
            }
        }
    }

    private void authenticateSpotify() {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(getString(R.string.spotify_api_key), AuthenticationResponse.Type.TOKEN, getString(R.string.redirect_url));
        builder.setScopes(new String[]{SCOPES});
        builder.setShowDialog(true);
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    private void goMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }
}