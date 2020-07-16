package com.example.pitchr.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.example.pitchr.R;
import com.example.pitchr.adapters.PostsAdapter;
import com.example.pitchr.databinding.ActivityMainBinding;
import com.example.pitchr.fragments.CommentDialogFragment;
import com.example.pitchr.fragments.DetailsFragment;
import com.example.pitchr.fragments.PostsFragment;
import com.example.pitchr.fragments.ProfileFragment;
import com.example.pitchr.models.Comment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.parse.ParseUser;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

public class MainActivity extends AppCompatActivity implements CommentDialogFragment.CommentDialogFragmentListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    public final FragmentManager fragmentManager = getSupportFragmentManager();
    public ActivityMainBinding binding;
    public SpotifyAppRemote mSpotifyAppRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        // layout of activity is stored in a special property called root
        View view = binding.getRoot();
        setContentView(view);
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextAppearance(this, R.style.PitchrTextAppearance);
        getSupportActionBar().setTitle(" ");

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
                        //fragment = new ComposeFragment();
                        fragment = new PostsFragment();
                        break;
                    case R.id.action_profile:
                    default:
                        fragment = ProfileFragment.newInstance(ParseUser.getCurrentUser());
                        break;
                }
                fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).commit();
                return true;
            }
        });

        // Set default selection
        binding.bottomNavigationView.setSelectedItemId(R.id.action_home);

        // Get Spotify service
        //connect();
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
                mSpotifyAppRemote = spotifyAppRemote;
                Log.d(TAG, "Spotify connected successfully");
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e(TAG, "Spotify failed to connect", throwable);
            }
        });
    }

    // Disconnect when we need to
    public void onStop() {
        super.onStop();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }
}