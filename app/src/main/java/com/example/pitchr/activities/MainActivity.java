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
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp;
import com.spotify.android.appremote.api.error.NotLoggedInException;
import com.spotify.android.appremote.api.error.UserNotAuthorizedException;
import com.spotify.protocol.types.Repeat;
import com.spotify.sdk.android.auth.AuthorizationClient;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements CommentDialogFragment.CommentDialogFragmentListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    public final FragmentManager fragmentManager = getSupportFragmentManager();
    public ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // LOG TO ANALYTICS
        ParseApplication.logActivityEvent("mainActivity");

        // Sign up for notifications
        FirebaseMessaging.getInstance().subscribeToTopic(ParseUser.getCurrentUser().getUsername());

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

        // Set up ads
        setupAds();

        // Set the bottom navigation view
        binding.bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Fragment fragment;
                String tag;
                switch (menuItem.getItemId()) {
                    case R.id.action_home:
                        fragment = new PostsFragment();
                        tag = "HOME_POSTS_FRAGMENT";
                        break;
                    case R.id.action_match:
                        fragment = new MatchesFragment();
                        tag = "HOME_MATCHES_FRAGMENT";
                        break;
                    case R.id.action_search:
                        fragment = new SearchUsersFragment();
                        tag = "HOME_SEARCH_FRAGMENT";
                        break;
                    case R.id.action_profile:
                    default:
                        fragment = ProfileFragment.newInstance(ParseUser.getCurrentUser());
                        tag = "HOME_PROFILE_FRAGMENT";
                        break;
                }
                fragmentManager.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).replace(R.id.flContainer, fragment, tag).commit();
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

    // Show new comments
    @Override
    public void onFinishCommentDialog(Comment comment) {
        ((DetailsFragment) fragmentManager.findFragmentByTag(PostsAdapter.TAG)).allComments.add(0, comment);
        ((DetailsFragment) fragmentManager.findFragmentByTag(PostsAdapter.TAG)).adapter.notifyDataSetChanged();

        // Notify the other user that their post was commented on
        String topic = String.format("/topics/%s", comment.getOriginalPost().getUser().getUsername());
        String notificationTitle = "Pitchr";
        String notificationMessage = String.format("%s commented on your post about %s!", ParseUser.getCurrentUser().getUsername(), comment.getOriginalPost().getSong().getName());
        String icon = ((ParseFile) ParseUser.getCurrentUser().get("pfp")) != null ? ((ParseFile) ParseUser.getCurrentUser().get("pfp")).getUrl() : "";

        JSONObject notification = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            // Set the message
            notificationBody.put("title", notificationTitle);
            notificationBody.put("message", notificationMessage);
            if (!icon.isEmpty()) {
                notificationBody.put("icon", icon);
            } else {
                notificationBody.put("icon", getString(R.string.default_app_icon_url));
            }

            // Set the topic
            notification.put("to", topic);
            notification.put("data", notificationBody);
        } catch (JSONException ex) {
            Log.e(TAG, "onCreate error!", ex);
        }
        // Send the notification
        ParseApplication.sendNotification(notification, getApplicationContext());
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
                    // Show button to matches_image Spotify
                    ((ParseApplication) getApplicationContext()).spotifyExists = false;
                    Toast.makeText(MainActivity.this, "Please install the Spotify app!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.spotify.music")));
                } else {
                    ((ParseApplication) getApplicationContext()).spotifyExists = false;
                }
            }
        });
    }

    // Disconnect when we need to
    public void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(((ParseApplication) getApplicationContext()).mSpotifyAppRemote);
    }

    // When we resume, we should stop and reconnect just in case
    @Override
    protected void onResume() {
        super.onResume();
        onStop();
        connect();
        FirebaseMessaging.getInstance().subscribeToTopic(ParseUser.getCurrentUser().getUsername());
    }

    // Set up ads
    private void setupAds() {
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
    }

    // Destroy the ads when we can
    @Override
    protected void onDestroy() {
        onStop();
        if (((PostsFragment) fragmentManager.findFragmentByTag("HOME_POSTS_FRAGMENT")) != null) {
            ((PostsFragment) fragmentManager.findFragmentByTag("HOME_POSTS_FRAGMENT")).clearAdData();
        }
        super.onDestroy();
    }
}