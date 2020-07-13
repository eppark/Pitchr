package com.example.pitchr;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.pitchr.activities.LoginActivity;
import com.example.pitchr.activities.MainActivity;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class SpotifyAuthenticationActivity extends AppCompatActivity {

    // Get credentials from shared preferences
    private SharedPreferences.Editor editor;
    private SharedPreferences msharedPreferences;

    private static final String TAG = SpotifyAuthenticationActivity.class.getSimpleName();
    private static final String SCOPES = "streaming,user-top-read,app-remote-control";
    private static final int REQUEST_CODE = 11037;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_spotify_authentication);

        msharedPreferences = this.getSharedPreferences("SPOTIFY", 0);
        token = msharedPreferences.getString("token", ""); // null by default if the key isn't there

        Log.d(TAG, "oncreate");
        if (token.isEmpty()) {
            // Authenticate
            authenticateSpotify();
        } else {
            // We can just go to the main activity
            goMainActivity();
        }
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
                    editor = getSharedPreferences("SPOTIFY", 0).edit();
                    token = response.getAccessToken();
                    editor.putString("token", token);
                    Log.d("STARTING", "GOT AUTH TOKEN");
                    editor.apply();
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
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    private void goMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("token", token);
        startActivity(i);
        finish();
    }
}