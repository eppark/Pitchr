package com.example.pitchr.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.adapters.PostsAdapter;
import com.example.pitchr.databinding.ActivityMainBinding;
import com.example.pitchr.fragments.CommentDialogFragment;
import com.example.pitchr.fragments.DetailsFragment;
import com.example.pitchr.fragments.MatchesFragment;
import com.example.pitchr.fragments.PostsFragment;
import com.example.pitchr.fragments.ProfileFragment;
import com.example.pitchr.fragments.SearchUsersFragment;
import com.example.pitchr.models.Comment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.parse.ParseUser;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp;
import com.spotify.android.appremote.api.error.NotLoggedInException;
import com.spotify.android.appremote.api.error.UserNotAuthorizedException;
import com.spotify.protocol.types.Repeat;
import com.spotify.sdk.android.auth.AuthorizationClient;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements CommentDialogFragment.CommentDialogFragmentListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    public final FragmentManager fragmentManager = getSupportFragmentManager();
    public ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // LOG TO ANALYTICS
        ParseApplication.logEvent("mainActivity", Arrays.asList("status"), Arrays.asList("success"));

        // Set ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        // layout of activity is stored in a special property called root
        View view = binding.getRoot();
        setContentView(view);
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextAppearance(this, R.style.PitchrTextAppearance);
        getSupportActionBar().setTitle(" ");

        // Get Spotify service
        connect();

        // Set the bottom navigation view
        binding.bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Fragment fragment;
                switch (menuItem.getItemId()) {
                    case R.id.action_home:
                        fragment = new PostsFragment();
                        break;
                    case R.id.action_match:
                        fragment = new MatchesFragment();
                        break;
                    case R.id.action_search:
                        fragment = new SearchUsersFragment();
                        break;
                    case R.id.action_profile:
                    default:
                        fragment = ProfileFragment.newInstance(ParseUser.getCurrentUser());
                        break;
                }
                fragmentManager.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).replace(R.id.flContainer, fragment).commit();
                return true;
            }
        });

        // Set default selection
        binding.bottomNavigationView.setSelectedItemId(R.id.action_home);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onFinishCommentDialog(Comment comment) {
        ((DetailsFragment) fragmentManager.findFragmentByTag(PostsAdapter.TAG)).allComments.add(0, comment);
        ((DetailsFragment) fragmentManager.findFragmentByTag(PostsAdapter.TAG)).adapter.notifyDataSetChanged();
    }

    // Spotify player
    private void connect() {
        // Authorization
        ConnectionParams connectionParams = new ConnectionParams.Builder(getString(R.string.spotify_api_key)).setRedirectUri(getString(R.string.redirect_url))
                .showAuthView(false).build();

        // Connect to Spotify
        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                Log.d(TAG, "Spotify connected successfully");
                ((ParseApplication) getApplicationContext()).spotifyExists = true;

                ((ParseApplication) getApplicationContext()).mSpotifyAppRemote = spotifyAppRemote;

                // Set it so that the player repeats the currently playing song and doesn't shuffle
                ((ParseApplication) getApplicationContext()).mSpotifyAppRemote.getPlayerApi().setRepeat(Repeat.ONE);
                ((ParseApplication) getApplicationContext()).mSpotifyAppRemote.getPlayerApi().setShuffle(false);
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e(TAG, "Spotify failed to connect", throwable);
                if (throwable instanceof NotLoggedInException || throwable instanceof UserNotAuthorizedException) {
                    // Show login button and trigger the login flow from auth library when clicked
                    Toast.makeText(MainActivity.this, "Login issue. Try again.", Toast.LENGTH_SHORT).show();
                    ParseUser.logOut();
                    AuthorizationClient.clearCookies(MainActivity.this); // Clear Spotify cookies
                    Intent i = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(i);
                    finish();
                } else if (throwable instanceof CouldNotFindSpotifyApp) {
                    // Show button to download Spotify
                    ((ParseApplication) getApplicationContext()).spotifyExists = false;
                    Toast.makeText(MainActivity.this, "Please install the Spotify app!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.spotify.music")));
                }
            }
        });
    }

    // Disconnect when we need to
    public void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(((ParseApplication) getApplicationContext()).mSpotifyAppRemote);
    }

    @Override
    protected void onResume() {
        super.onResume();
        onStop();
        connect();
    }
}