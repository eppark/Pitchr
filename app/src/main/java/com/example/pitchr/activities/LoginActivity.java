package com.example.pitchr.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.pitchr.ParseApplication;
import com.example.pitchr.databinding.ActivityLoginBinding;
import com.google.firebase.messaging.FirebaseMessaging;
import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class LoginActivity extends AppCompatActivity {

    public static final String TAG = LoginActivity.class.getSimpleName();
    ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set ViewBinding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());

        // layout of activity is stored in a special property called root
        View view = binding.getRoot();
        setContentView(view);
        binding.pbLoading.setVisibility(View.INVISIBLE); // Hide the progress bar at first

        // LOG TO ANALYTICS
        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        // LOG TO ANALYTICS
        ParseApplication.logActivityEvent("loginActivity");

        if (ParseUser.getCurrentUser() != null) {
            // If the session isn't valid, log the user out
            if (!ParseUser.getCurrentUser().isAuthenticated()) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(ParseUser.getCurrentUser().getUsername());
                ParseUser.logOut();
            } else {
                // Else we can just authenticate with Spotify
                goSpotifyAuth(true);
            }
        }

        // Set up logging in button
        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.pbLoading.setVisibility(View.VISIBLE); // Show the progress bar
                Log.i(TAG, "onClick login button");
                String username = binding.etUsername.getEditText().getText().toString();
                String password = binding.etPassword.getEditText().getText().toString();
                loginUser(username, password);
            }
        });

        // Set up signing up button
        binding.btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.pbLoading.setVisibility(View.VISIBLE); // Show the progress bar
                Log.i(TAG, "onClick signup button");
                String username = binding.etUsername.getEditText().getText().toString();
                String password = binding.etPassword.getEditText().getText().toString();
                signupUser(username, password);
            }
        });
    }

    private void loginUser(String username, String password) {
        Log.i(TAG, "Attempting to login user " + username);
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                binding.pbLoading.setVisibility(View.INVISIBLE); // Hide the progress bar
                if (e != null) {
                    // There was an error
                    Log.e(TAG, "Issue with login", e);
                    Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                    // LOG TO ANALYTICS
                    ParseApplication.logLoginEvent("failure");
                    return;
                }
                Toast.makeText(LoginActivity.this, "Login success.", Toast.LENGTH_SHORT).show();

                // LOG TO ANALYTICS
                ParseApplication.logLoginEvent("success");

                // Now authenticate via Spotify
                goSpotifyAuth(false);
            }
        });
    }

    private void signupUser(String username, String password) {
        Log.i(TAG, "Attempting to signup user...");
        // Create the ParseUser
        ParseUser user = new ParseUser();
        // Set core properties
        user.setUsername(username);
        user.setPassword(password);
        // Invoke signUpInBackground
        user.signUpInBackground(new SignUpCallback() {
            public void done(ParseException e) {
                binding.pbLoading.setVisibility(View.INVISIBLE); // Hide the progress bar
                if (e != null) {
                    // There was an error
                    Log.e(TAG, "Issue with signup", e);
                    Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                    // LOG TO ANALYTICS
                    ParseApplication.logSignupEvent("failure");
                    return;
                }
                Toast.makeText(LoginActivity.this, "Signup success.", Toast.LENGTH_SHORT).show();

                // LOG TO ANALYTICS
                ParseApplication.logSignupEvent("success");

                // Now authenticate via Spotify
                goSpotifyAuth(false);
            }
        });
    }

    // Go to the Spotify authentication activity
    private void goSpotifyAuth(boolean returning) {
        // SET USER IN ANALYTICS
        ParseApplication.mFirebaseAnalytics.setUserId(ParseUser.getCurrentUser().getUsername());

        // Go to the Spotify authentication activity
        Intent i = new Intent(this, SpotifyAuthenticationActivity.class);
        i.putExtra("returning", returning);
        Log.d(TAG, "spotifyauth");
        startActivity(i);
        finish();
    }
}