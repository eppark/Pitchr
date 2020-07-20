package com.example.pitchr.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.pitchr.ParseApplication;
import com.example.pitchr.databinding.ActivityLoginBinding;
import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

        if (ParseUser.getCurrentUser() != null) {
            // If the session isn't valid, log the user out
            if (!ParseUser.getCurrentUser().isAuthenticated()) {
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
                String username = binding.etUsername.getText().toString();
                String password = binding.etPassword.getText().toString();
                loginUser(username, password);
            }
        });

        // Set up signing up button
        binding.btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.pbLoading.setVisibility(View.VISIBLE); // Show the progress bar
                Log.i(TAG, "onClick signup button");
                String username = binding.etUsername.getText().toString();
                String password = binding.etPassword.getText().toString();
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
                    ParseApplication.logEvent("loginEvent", Arrays.asList("status", "type"), Arrays.asList("failure", "login"));
                    return;
                }
                goSpotifyAuth(false);
                Toast.makeText(LoginActivity.this, "Login success.", Toast.LENGTH_SHORT).show();

                // LOG TO ANALYTICS
                ParseApplication.logEvent("loginEvent", Arrays.asList("status", "type"), Arrays.asList("success", "login"));
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
                    ParseApplication.logEvent("loginEvent", Arrays.asList("status", "type"), Arrays.asList("failure", "signup"));
                    return;
                }
                goSpotifyAuth(false);
                Toast.makeText(LoginActivity.this, "Signup success.", Toast.LENGTH_SHORT).show();

                // LOG TO ANALYTICS
                ParseApplication.logEvent("loginEvent", Arrays.asList("status", "type"), Arrays.asList("success", "signup"));
            }
        });
    }

    // Go to the Spotify authentication activity
    private void goSpotifyAuth(boolean returning) {
        Intent i = new Intent(this, SpotifyAuthenticationActivity.class);
        i.putExtra("returning", returning);
        Log.d(TAG, "spotifyauth");
        startActivity(i);
        finish();
    }
}