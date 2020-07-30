package com.example.pitchr.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.activities.MainActivity;
import com.example.pitchr.activities.SearchSongsActivity;
import com.example.pitchr.adapters.PostsAdapter;
import com.example.pitchr.chat.DirectMessagesActivity;
import com.example.pitchr.helpers.EndlessRecyclerViewScrollListener;
import com.example.pitchr.models.FavSongs;
import com.example.pitchr.models.Following;
import com.example.pitchr.models.Match;
import com.example.pitchr.models.Match2;
import com.example.pitchr.models.Post;
import com.example.pitchr.models.PostItem;
import com.example.pitchr.models.Song;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PostsFragment extends Fragment {

    private static final String TAG = PostsFragment.class.getSimpleName();
    private static final int RESULT_CODE = 11037;
    protected RecyclerView rvPosts;
    protected PostsAdapter adapter;
    protected List<PostItem> allPosts;
    protected List<ParseUser> following;
    ArrayList<Song> songRecs;
    List<Song> myFavSongs;
    ArrayList<UnifiedNativeAd> allAds;
    Button btnFindUsers;
    TextView tvNoPosts;
    ProgressBar pbLoading;

    // The AdLoader used to load ads.
    private AdLoader adLoader;

    // Floating Action Button for composing
    FloatingActionButton fabCompose;
    public static final String CIRCULAR_REVEAL_X = "EXTRA_CIRCULAR_REVEAL_X";
    public static final String CIRCULAR_REVEAL_Y = "EXTRA_CIRCULAR_REVEAL_Y";

    // Swipe to refresh and endless scrolling
    private SwipeRefreshLayout swipeContainer;
    private EndlessRecyclerViewScrollListener scrollListener;

    public PostsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        return inflater.inflate(R.layout.fragment_posts, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // LOG TO ANALYTICS
        ParseApplication.logEvent("postsFragment", Arrays.asList("status"), Arrays.asList("success"));

        // View binding
        rvPosts = (RecyclerView) view.findViewById(R.id.rvPosts);
        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        pbLoading = (ProgressBar) view.findViewById(R.id.pbLoading);
        fabCompose = (FloatingActionButton) view.findViewById(R.id.fabCompose);
        pbLoading.setVisibility(View.GONE); // Hide progress bar at first
        btnFindUsers = (Button) view.findViewById(R.id.btnFindUsers);
        tvNoPosts = (TextView) view.findViewById(R.id.tvNoPosts);
        tvNoPosts.setVisibility(View.GONE);
        btnFindUsers.setVisibility(View.GONE); // Hide lack of posts at first

        // Set up array lists for songs, recommendations, and ads
        myFavSongs = new ArrayList<>();
        songRecs = new ArrayList<>();
        allAds = new ArrayList<>();

        // Set posts, adapter, and layout
        allPosts = new ArrayList<>();
        following = new ArrayList<>();
        adapter = new PostsAdapter(getContext(), allPosts);
        rvPosts.setAdapter(adapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        rvPosts.setLayoutManager(linearLayoutManager);

        // Set the refresher
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initialQuery();
                swipeContainer.setRefreshing(false);
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(R.color.spotifyGreen);

        // Retain an instance for fresh searches
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                pbLoading.setVisibility(View.VISIBLE); // Show progress bar
                queryFollowing(page);
            }
        };
        // Adds the scroll listener to RecyclerView
        rvPosts.addOnScrollListener(scrollListener);

        // When the user scrolls, hide the compose button
        rvPosts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && fabCompose.getVisibility() == View.VISIBLE) {
                    fabCompose.hide();
                } else if (dy < 0 && fabCompose.getVisibility() != View.VISIBLE) {
                    fabCompose.show();
                }
            }
        });

        // Get posts initially
        initialQuery();

        // Create the compose button click event
        fabCompose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the button's position
                int[] location = new int[2];
                fabCompose.getLocationOnScreen(location);

                // Start the activity
                Intent i = new Intent(getContext(), SearchSongsActivity.class);
                i.putExtra(CIRCULAR_REVEAL_X, location[0]);
                i.putExtra(CIRCULAR_REVEAL_Y, location[1]);
                startActivityForResult(i, RESULT_CODE);
            }
        });

        // Create find users to match click event
        btnFindUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Take them to the matching page
                FragmentTransaction ft = ((MainActivity) getContext()).getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.replace(R.id.flContainer, new MatchesFragment(), TAG);
                ft.addToBackStack(TAG);
                ft.commit();
            }
        });
    }

    // Initial query
    private void initialQuery() {
        adapter.clear();
        pbLoading.setVisibility(View.VISIBLE); // Show progress bar

        // Clear favorite songs, song recommendations, and ads
        myFavSongs.clear();
        songRecs.clear();
        clearAdData();

        // Get the current user's favorite songs
        ParseQuery<FavSongs> query = ParseQuery.getQuery(FavSongs.class);
        query.include(FavSongs.KEY_USER);
        query.include(FavSongs.KEY_SONG);
        query.whereEqualTo(FavSongs.KEY_USER, ParseUser.getCurrentUser());
        query.setLimit(20); // Only show 20 songs at a time
        query.addDescendingOrder(FavSongs.KEY_CREATED_AT);
        query.findInBackground(new FindCallback<FavSongs>() {
            @Override
            public void done(List<FavSongs> songsList, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting fav songs", e);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Query fav songs success!");
                for (FavSongs songItem : songsList) {
                    myFavSongs.add(songItem.getSong());
                }

                switch (((ParseApplication) getContext().getApplicationContext()).version) {
                    case 1:
                        // Query for the users that the current user is matched with
                        // Use algorithm 1
                        ParseQuery<Match> matchQuery = ParseQuery.getQuery(Match.class);
                        matchQuery.include(Match.KEY_TO);
                        matchQuery.whereEqualTo(Match.KEY_FROM, ParseUser.getCurrentUser());
                        matchQuery.addDescendingOrder(Match.KEY_PERCENT);
                        matchQuery.setLimit(5);
                        matchQuery.findInBackground(new FindCallback<Match>() {
                            @Override
                            public void done(List<Match> matches, ParseException e) {
                                if (e != null) {
                                    Log.e(TAG, "Issue with getting matches", e);
                                    Toast.makeText(getContext(), "Failed to get matches", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Log.d(TAG, "Query matches success!");

                                if (matches.size() > 0) {
                                    // Get the favorite songs for each match
                                    for (Match match : matches) {
                                        // We don't want exact matches
                                        if (match.getPercent() < 1.0) {
                                            ParseQuery<FavSongs> query = ParseQuery.getQuery(FavSongs.class);
                                            query.include(FavSongs.KEY_USER);
                                            query.include(FavSongs.KEY_SONG);
                                            query.whereEqualTo(FavSongs.KEY_USER, match.getTo());
                                            query.setLimit(10); // Only show 10 songs at a time
                                            query.addDescendingOrder(FavSongs.KEY_CREATED_AT);
                                            query.findInBackground(new FindCallback<FavSongs>() {
                                                @Override
                                                public void done(List<FavSongs> songsList, ParseException e) {
                                                    if (e != null) {
                                                        Log.e(TAG, "Issue with getting fav songs", e);
                                                        return;
                                                    }
                                                    Log.d(TAG, "Query fav songs success!");

                                                    // If the song isn't in the current user's favorite songs list, we can add is to our recommended
                                                    for (FavSongs songItem : songsList) {
                                                        if (!myFavSongs.contains(songItem.getSong())) {
                                                            songRecs.add(songItem.getSong());
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                                // Query for the users that the current user is following
                                queryFollowing(0);
                            }
                        });
                        break;
                    case 2:
                    default:
                        // Query for the users that the current user is matched with
                        // Use algorithm 2
                        ParseQuery<Match2> match2Query = ParseQuery.getQuery(Match2.class);
                        match2Query.include(Match2.KEY_TO);
                        match2Query.whereEqualTo(Match2.KEY_FROM, ParseUser.getCurrentUser());
                        match2Query.addDescendingOrder(Match2.KEY_PERCENT);
                        match2Query.setLimit(5);
                        match2Query.findInBackground(new FindCallback<Match2>() {
                            @Override
                            public void done(List<Match2> matches, ParseException e) {
                                if (e != null) {
                                    Log.e(TAG, "Issue with getting matches", e);
                                    Toast.makeText(getContext(), "Failed to get matches", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Log.d(TAG, "Query matches success!");

                                if (matches.size() > 0) {
                                    // Get the favorite songs for each match
                                    for (Match2 match : matches) {
                                        // We don't want exact matches
                                        if (match.getPercent() < 1.0) {
                                            ParseQuery<FavSongs> query = ParseQuery.getQuery(FavSongs.class);
                                            query.include(FavSongs.KEY_USER);
                                            query.include(FavSongs.KEY_SONG);
                                            query.whereEqualTo(FavSongs.KEY_USER, match.getTo());
                                            query.setLimit(10); // Only show 10 songs at a time
                                            query.addDescendingOrder(FavSongs.KEY_CREATED_AT);
                                            query.findInBackground(new FindCallback<FavSongs>() {
                                                @Override
                                                public void done(List<FavSongs> songsList, ParseException e) {
                                                    if (e != null) {
                                                        Log.e(TAG, "Issue with getting fav songs", e);
                                                        return;
                                                    }
                                                    Log.d(TAG, "Query fav songs success!");

                                                    // If the song isn't in the current user's favorite songs list, we can add is to our recommended
                                                    for (FavSongs songItem : songsList) {
                                                        boolean newSong = true;
                                                        for (Song mySong : myFavSongs) {
                                                            if (mySong.getSpotifyId().equals(songItem.getSong().getSpotifyId())) {
                                                                newSong = false;
                                                                break;
                                                            }
                                                        }
                                                        if (newSong) {
                                                            songRecs.add(songItem.getSong());
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                                // Query for the users that the current user is following
                                queryFollowing(0);
                            }
                        });
                        break;
                }
            }
        });
    }

    // Query posts by the current user and by the user's following list
    private void queryFollowing(int page) {
        following.clear();
        following.add(ParseUser.getCurrentUser()); // also want posts from the current user
        ParseQuery<Following> query = ParseQuery.getQuery(Following.class);
        query.include(Following.KEY_FOLLOWED_BY);
        query.include(Following.KEY_FOLLOWING);
        query.whereEqualTo(Following.KEY_FOLLOWED_BY, ParseUser.getCurrentUser());
        query.addDescendingOrder(Following.KEY_CREATED_AT);
        query.findInBackground(new FindCallback<Following>() {
            @Override
            public void done(List<Following> userList, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting following", e);
                    return;
                }
                Log.d(TAG, "Query following success!");
                for (Following followingItem : userList) {
                    following.add(followingItem.getFollowing());
                }
                queryPosts(page);
            }
        });
    }

    // Query posts from database
    protected void queryPosts(int page) {
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
        query.include(Post.KEY_AUTHOR);
        query.include(Post.KEY_SONG);
        query.whereContainedIn(Post.KEY_AUTHOR, following);
        query.setSkip(20 * page);
        query.setLimit(20); // Only show 20 posts
        query.addDescendingOrder(Post.KEY_CREATED_AT);
        query.findInBackground(new FindCallback<Post>() {
            @Override
            public void done(List<Post> posts, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting posts", e);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Query posts success!");

                // Add each of the posts
                for (Post post : posts) {
                    allPosts.add(new PostItem(post, PostItem.TYPE_POST, null));

                    // If this is the 5th post, show the recommended songs
                    if (allPosts.size() == 5 && songRecs.size() > 0) {
                        allPosts.add(new PostItem(null, PostItem.TYPE_REC, songRecs));
                    }
                }
                if (allAds.isEmpty() && allPosts.size() > 5) {
                    loadNativeAds();
                }
                pbLoading.setVisibility(View.GONE); // Hide progress bar
                adapter.notifyDataSetChanged();

                // If we have no posts, show the option to the user to find a match
                if (allPosts.size() == 0) {
                    tvNoPosts.setVisibility(View.VISIBLE);
                    btnFindUsers.setVisibility(View.VISIBLE);
                    btnFindUsers.setClickable(true);
                } else {
                    tvNoPosts.setVisibility(View.GONE);
                    btnFindUsers.setVisibility(View.GONE);
                }
            }
        });
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // Refresh the feed
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CODE) {
            initialQuery();
        }
    }

    // Handle menu clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.miMessages:
                Intent i = new Intent(getContext(), DirectMessagesActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Destroy all data to prevent memory leaks when applicable
    public void clearAdData() {
        for (UnifiedNativeAd ad : allAds) {
            ad.destroy();
        }
        allAds.clear();
    }

    // Insert ads into our list
    private void insertAdsInAdapterItems() {
        if (allAds.size() <= 0) {
            return;
        }

        // Insert the ads based on how many posts and ads we have
        int offset = (allPosts.size() / (allAds.size() + 1)) + 1;
        int index = offset;
        for (UnifiedNativeAd ad : allAds) {
            allPosts.add(index, new PostItem(null, PostItem.TYPE_AD, null, ad));
            index = index + offset;
        }
    }

    // Load the ads
    private void loadNativeAds() {
        AdLoader.Builder builder = new AdLoader.Builder(getContext(), getString(R.string.ad_unit_id));
        adLoader = builder.forUnifiedNativeAd(
                new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        // A native ad loaded successfully, check if the ad loader has finished loading and if so, insert the ads into the list.
                        allAds.add(unifiedNativeAd);
                        if (!adLoader.isLoading()) {
                            insertAdsInAdapterItems();
                        }
                    }
                }).withAdListener(
                new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // A native ad failed to load, check if the ad loader has finished loading and if so, insert the ads into the list.
                        Log.e(TAG, "The previous native ad failed to load. Attempting to load another.");
                        if (!adLoader.isLoading()) {
                            insertAdsInAdapterItems();
                        }
                    }
                }).build();

        // Load the Native Express ad.
        adLoader.loadAds(new AdRequest.Builder().build(), Math.round(allPosts.size() / 5));
    }
}