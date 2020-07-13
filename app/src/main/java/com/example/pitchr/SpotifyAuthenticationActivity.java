package com.example.pitchr;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.pitchr.activities.MainActivity;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class SpotifyAuthenticationActivity extends AppCompatActivity {

    private static final String TAG = SpotifyAuthenticationActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotify_authentication);
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();
        if (uri != null) {
            AuthenticationResponse response = AuthenticationResponse.fromUri(uri);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    Intent i = new Intent(SpotifyAuthenticationActivity.this, MainActivity.class);
                    i.putExtra("token", response.getAccessToken());
                    startActivity(i);
                    Log.d(TAG, "success");
                    finish();
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.d(TAG, "Error spotify auth");
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
                    Log.d(TAG, "Auth flow cancelled");
            }
        }
    }
}