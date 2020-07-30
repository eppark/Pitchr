package com.example.pitchr.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;

import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.adapters.SongsAdapter;
import com.example.pitchr.databinding.ActivitySearchBinding;
import com.example.pitchr.fragments.PostsFragment;
import com.example.pitchr.helpers.EndlessRecyclerViewScrollListener;
import com.example.pitchr.models.Song;
import com.google.android.material.chip.ChipGroup;
import com.parse.ParseUser;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.PlaylistsPager;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.client.Response;

import static android.view.View.NO_ID;

public class SearchSongsActivity extends AppCompatActivity {

    public static final String TAG = SearchSongsActivity.class.getSimpleName();
    public static final int RESULT_CODE = 11037;
    SpotifyService spotify;
    ActivitySearchBinding binding;
    private SongsAdapter songsAdapter;
    private ArrayList<Song> aSongs;

    // Limit of songs to get
    private static final int LIMIT = 20;

    // Search query or playlist ID
    String songQuery;
    String playlistId = "";

    // For reveal animation
    int revealX;
    int revealY;

    // Endless scrolling
    private EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // LOG TO ANALYTICS
        ParseApplication.logEvent("searchActivity", Arrays.asList("status"), Arrays.asList("success"));

        // Set ViewBinding
        binding = ActivitySearchBinding.inflate(getLayoutInflater());

        // layout of activity is stored in a special property called root
        View view = binding.getRoot();
        setContentView(view);
        setSupportActionBar(binding.toolbar);
        view.setBackgroundColor(getResources().getColor(R.color.gray3));
        getSupportActionBar().setTitle("Find song");
        binding.toolbar.setTitleTextAppearance(this, R.style.PitchrTextAppearance);
        binding.pbProgressAction.setVisibility(View.GONE);
        binding.tvSearchSongs.setVisibility(View.VISIBLE);

        // Show the circular reveal animation if we clicked from the floating action button
        binding.rlSearch.setVisibility(View.INVISIBLE);
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getIntent().hasExtra(PostsFragment.CIRCULAR_REVEAL_X) && getIntent().hasExtra(PostsFragment.CIRCULAR_REVEAL_Y)) {
            revealX = getIntent().getIntExtra(PostsFragment.CIRCULAR_REVEAL_X, 0);
            revealY = getIntent().getIntExtra(PostsFragment.CIRCULAR_REVEAL_Y, 0);

            ViewTreeObserver viewTreeObserver = binding.rlSearch.getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Calculate the radius
                        float finalRadius = (float) (Math.max(binding.rlSearch.getWidth(), binding.rlSearch.getHeight()) * 1.1);

                        // Create the animator for this view (the start radius is zero)
                        Animator circularReveal = ViewAnimationUtils.createCircularReveal(binding.rlSearch, revealX, revealY, 0, finalRadius);
                        circularReveal.setDuration(350);
                        circularReveal.setInterpolator(new AccelerateInterpolator());

                        // Make the view visible and start the animation
                        binding.rlSearch.setVisibility(View.VISIBLE);
                        circularReveal.start();

                        binding.rlSearch.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
            }
        } else {
            binding.rlSearch.setVisibility(View.VISIBLE);
        }

        // Get Spotify service
        SpotifyApi spotifyApi = new SpotifyApi();
        spotifyApi.setAccessToken(ParseUser.getCurrentUser().getString("token"));
        spotify = spotifyApi.getService();

        // Initialize the adapter
        aSongs = new ArrayList<>();
        songsAdapter = new SongsAdapter(this, aSongs, SongsAdapter.TYPE_SONG);
        songsAdapter.setOnItemClickListener(new SongsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                // Make sure the position is valid
                if (position != RecyclerView.NO_POSITION) {
                    Song song = aSongs.get(position);

                    // Check which intent we are coming from
                    if (getIntent().getBooleanExtra("add", false)) {
                        // This means we are in the add activity, so we want to go back to the favorite songs list with the new Song
                        Intent data = new Intent();
                        data.putExtra(Song.class.getSimpleName(), Parcels.wrap(song));
                        setResult(RESULT_OK, data);
                        finish();
                    } else {
                        // Create an intent for the new activity
                        Intent intent = new Intent(SearchSongsActivity.this, ComposeActivity.class);
                        intent.putExtra(Song.class.getSimpleName(), Parcels.wrap(song)); // serialize the movie using Parceler

                        // Show the activity
                        startActivityForResult(intent, RESULT_CODE);
                        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                    }
                }
            }
        });

        // Attach the adapter to the RecyclerView
        binding.rvSongs.setAdapter(songsAdapter);

        // Set layout manager to position the items
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        binding.rvSongs.setLayoutManager(linearLayoutManager);

        // Retain an instance for fresh searches
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (playlistId.isEmpty()) {
                    fetchSongs(songQuery, page);
                } else {
                    fetchGenreSongs(page);
                }
            }
        };

        // Adds the scroll listener to RecyclerView
        binding.rvSongs.addOnScrollListener(scrollListener);

        // Show the different song playlists if the user clicks a genre button
        binding.chipGroup.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, int checkedId) {
                // If we de-checked, set that
                if (checkedId == NO_ID) {
                    playlistId = "";
                } else {
                    // Show the loading bar and clear the arraylist first
                    binding.pbProgressAction.setVisibility(View.VISIBLE);
                    aSongs.clear();

                    // Get the category ID and set options to just get the first playlist
                    String categoryId = group.findViewById(checkedId).getContentDescription().toString();
                    Map<String, Object> options = new HashMap<>();
                    options.put(SpotifyService.LIMIT, 1);

                    // Get the first playlist for the genre
                    spotify.getPlaylistsForCategory(categoryId, options, new SpotifyCallback<PlaylistsPager>() {
                        @Override
                        public void failure(SpotifyError spotifyError) {
                            Log.e(TAG, "Search playlists failed!", spotifyError);
                        }

                        @Override
                        public void success(PlaylistsPager playlistsPager, Response response) {
                            Log.d(TAG, "Search playlists success!");

                            // Get the songs from the playlist
                            playlistId = playlistsPager.playlists.items.get(0).id;
                            fetchGenreSongs(0);
                        }
                    });
                }
            }
        });
    }

    // Executes an API call to the Spotify playlist endpoint, parses the results
    // Converts them into an array of Song objects and adds them to the adapter
    private void fetchGenreSongs(int offset) {
        Map<String, Object> options = new HashMap<>();
        options.put(SpotifyService.OFFSET, LIMIT * offset);
        options.put(SpotifyService.LIMIT, LIMIT);

        spotify.getPlaylistTracks("", playlistId, new SpotifyCallback<Pager<PlaylistTrack>>() {
            @Override
            public void failure(SpotifyError spotifyError) {
                Log.e(TAG, "Get playlist tracks failed!", spotifyError);
            }

            @Override
            public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                Log.d(TAG, "Get playlist tracks success!");
                // Add to the song list
                for (PlaylistTrack playlistTrack : playlistTrackPager.items) {
                    aSongs.add(Song.songFromTrack(playlistTrack.track));
                }
                songsAdapter.notifyDataSetChanged();
                binding.pbProgressAction.setVisibility(View.GONE);

                // Just in case there are no songs that match the query
                if (aSongs.size() == 0) {
                    binding.tvSearchSongs.setVisibility(View.VISIBLE);
                } else {
                    binding.tvSearchSongs.setVisibility(View.GONE);
                }
            }
        });
    }

    // Executes an API call to the Spotify search endpoint, parses the results
    // Converts them into an array of Song objects and adds them to the adapter
    private void fetchSongs(String query, int offset) {
        Map<String, Object> options = new HashMap<>();
        options.put(SpotifyService.OFFSET, LIMIT * offset);
        options.put(SpotifyService.LIMIT, LIMIT);

        spotify.searchTracks(query, options, new SpotifyCallback<TracksPager>() {
            @Override
            public void failure(SpotifyError spotifyError) {
                Log.e(TAG, "Search tracks failed!", spotifyError);
            }

            @Override
            public void success(TracksPager tracksPager, Response response) {
                Log.d(TAG, "Search tracks success!");
                // Add to the song list
                aSongs.addAll(Song.songsFromTracksList(tracksPager.tracks.items));
                songsAdapter.notifyDataSetChanged();
                binding.pbProgressAction.setVisibility(View.GONE);

                // Just in case there are no songs that match the query
                if (aSongs.size() == 0) {
                    binding.tvSearchSongs.setVisibility(View.VISIBLE);
                } else {
                    binding.tvSearchSongs.setVisibility(View.GONE);
                }
            }
        });
    }

    // Show search option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search_list, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint("Search songs...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                binding.pbProgressAction.setVisibility(View.VISIBLE);
                // perform query here
                aSongs.clear();
                songQuery = query;
                fetchSongs(songQuery, 0);

                // workaround to avoid issues with some emulators and keyboard devices firing twice if a keyboard enter is used
                // see https://code.google.com/p/android/issues/detail?id=24599
                searchView.clearFocus();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    // If we compose, we need to finish this activity as well
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CODE) {
            setResult(RESULT_CODE);
            finish();
        }
    }

    // If we press the back button, show the circular reveal transformation back to the floating action button
    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getIntent().hasExtra(PostsFragment.CIRCULAR_REVEAL_X) && getIntent().hasExtra(PostsFragment.CIRCULAR_REVEAL_Y)) {
            // Set the radius and animation start/end coordinates
            float finalRadius = (float) (Math.max(binding.rlSearch.getWidth(), binding.rlSearch.getHeight()) * 1.1);
            Animator circularReveal = ViewAnimationUtils.createCircularReveal(binding.rlSearch, revealX, revealY, finalRadius, 0);
            circularReveal.setDuration(400);

            // When the animation ends, we want to finish the activity
            circularReveal.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    binding.rlSearch.setVisibility(View.INVISIBLE);
                    finish();
                }
            });

            // Start the animation
            circularReveal.start();
        } else {
            finish();
        }
    }
}