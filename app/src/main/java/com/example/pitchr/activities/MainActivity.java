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
import com.example.pitchr.databinding.ActivityMainBinding;
import com.example.pitchr.fragments.PostsFragment;
import com.example.pitchr.fragments.ProfileFragment;
import com.example.pitchr.models.FavSongs;
import com.example.pitchr.models.Song;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.boltsinternal.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    public final FragmentManager fragmentManager = getSupportFragmentManager();
    public static final int FAV_SONG_LIMIT = 10;
    public ActivityMainBinding binding;
    public SpotifyApi spotifyApi;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the Spotify API
        spotifyApi = new SpotifyApi();
        token = ParseUser.getCurrentUser().getString("token"); // get the access token
        spotifyApi.setAccessToken(token);

        // Get the fav songs for the current user if we need to
        queryFavSongs();

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
    }

    // Create a list of favorite songs from Spotify for the user
    private void queryFavSongs() {
        ParseQuery<FavSongs> query = ParseQuery.getQuery(FavSongs.class);
        query.include(FavSongs.KEY_USER);
        query.include(FavSongs.KEY_SONG);
        query.whereEqualTo(FavSongs.KEY_USER, ParseUser.getCurrentUser());
        query.getFirstInBackground(new GetCallback<FavSongs>() {
            @Override
            public void done(FavSongs favSong, ParseException e) {
                if (favSong == null) {
                    // If we don't have any fav songs, we should query them from Spotify
                    Map<String, Object> options = new HashMap<>();
                    options.put(SpotifyService.LIMIT, FAV_SONG_LIMIT);
                    options.put(SpotifyService.TIME_RANGE, "long_term");

                    spotifyApi.getService().getTopTracks(options, new Callback<Pager<Track>>() {
                        @Override
                        public void success(Pager<Track> trackPager, Response response) {
                            Log.d(TAG, "Get fav tracks success!");
                            for (Song song : Song.songsFromTracksList(trackPager.items)) {
                                // For each song, turn it into a Song for the database
                                addToFavSong(song);
                            }
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Log.e(TAG, "Get fav tracks failed!", error);
                        }
                    });
                }
            }
        });
    }

    // Create new FavSong objects for the database
    private void addToFavSong(Song song) {
        // Query in the database if the Song already exists
        ParseQuery<Song> songQuery = new ParseQuery<>(Song.class);
        songQuery.include(Song.KEY_SPOTIFY_ID);
        songQuery.whereEqualTo(Song.KEY_SPOTIFY_ID, song.getSpotifyId());
        Task<Song> object = songQuery.getFirstInBackground();

        // Create a new FavSongs row
        FavSongs favSongsObject = new FavSongs();
        favSongsObject.put(FavSongs.KEY_USER, ParseUser.getCurrentUser());
        if (object.getResult() == null) {
            // If the Song isn't already in the database, we need to save it
            song.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    favSongsObject.put(FavSongs.KEY_SONG, song);
                    favSongsObject.saveInBackground();
                }
            });
        } else {
            // If the song is already in the database, we don't want to create a new one
            favSongsObject.setSong((Song) ParseObject.createWithoutData(song.getClassName(), object.getResult().getObjectId()));
            favSongsObject.saveInBackground();
        }
    }
}