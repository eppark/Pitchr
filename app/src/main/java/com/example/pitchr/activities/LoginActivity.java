package com.example.pitchr.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.pitchr.R;
import com.example.pitchr.SpotifyAuthenticationActivity;
import com.example.pitchr.databinding.ActivityLoginBinding;
import com.parse.LogInCallback;
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
        Glide.with(this).load(R.drawable.pitchr_name_green).into(binding.imageView);

        // If we've already logged in before, skip the login screen
        if (ParseUser.getCurrentUser() != null) {
            ParseUser.logOut();
        }

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
                    return;
                }
                goSpotifyAuth();
                Toast.makeText(LoginActivity.this, "Login success.", Toast.LENGTH_SHORT).show();
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
                    return;
                }
                goSpotifyAuth();
                Toast.makeText(LoginActivity.this, "Signup success.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goSpotifyAuth() {
        Intent i = new Intent(this, SpotifyAuthenticationActivity.class);
        Log.d(TAG, "spotifyauth");
        startActivity(i);
        finish();
    }
}