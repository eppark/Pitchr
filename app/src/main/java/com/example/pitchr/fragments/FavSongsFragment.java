package com.example.pitchr.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.ResultReceiver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.activities.SettingsActivity;
import com.example.pitchr.adapters.SongsAdapter;
import com.example.pitchr.helpers.EndlessRecyclerViewScrollListener;
import com.example.pitchr.models.FavSongs;
import com.example.pitchr.models.Song;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

public class FavSongsFragment extends Fragment {

    public static final String TAG = FavSongsFragment.class.getSimpleName();
    public static final int RESULT_CODE = 4567;
    ParseUser user;
    public RecyclerView rvSongs;
    SongsAdapter adapter;
    ArrayList<Song> allSongs;
    SwipeRefreshLayout swipeContainer;
    TextView tvNoSongs;
    Button btnEditSongs;
    EndlessRecyclerViewScrollListener scrollListener;

    public FavSongsFragment() {
        // Required empty public constructor
    }

    public static FavSongsFragment newInstance(ParseUser user) {
        FavSongsFragment fragment = new FavSongsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ParseUser.class.getSimpleName(), Parcels.wrap(user));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fav_songs, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get the user
        user = Parcels.unwrap(getArguments().getParcelable(ParseUser.class.getSimpleName()));
        allSongs = new ArrayList<>();
        tvNoSongs = (TextView) view.findViewById(R.id.tvNoSongs);
        btnEditSongs = (Button) view.findViewById(R.id.btnEditSongs);
        tvNoSongs.setVisibility(View.GONE);
        btnEditSongs.setVisibility(View.GONE);

        // Recycler view setup: layout manager and the adapter
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        rvSongs = (RecyclerView) getView().findViewById(R.id.rvItems);
        rvSongs.setLayoutManager(layoutManager);
        adapter = new SongsAdapter(this.getContext(), allSongs);
        rvSongs.setAdapter(adapter);

        // Setup refresh listener which triggers new data loading
        swipeContainer = (SwipeRefreshLayout) getView().findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh the list
                queryInitial();
                swipeContainer.setRefreshing(false);
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(R.color.spotifyGreen);

        // Endless scrolling
        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                querySongs(page);
            }
        };
        // Add scroll listener to RecyclerView
        rvSongs.addOnScrollListener(scrollListener);

        // Populate the feed
        queryInitial();

        // Set the adapter position click listener
        adapter.setOnItemClickListener(new SongsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                if (((ParseApplication) (getContext().getApplicationContext())).spotifyExists) {
                    // Make sure the position is valid
                    if (position != RecyclerView.NO_POSITION) {
                        // Check if we're playing, pausing, or resuming
                        if (adapter.currentPosition == position) {
                            ((ParseApplication) getContext().getApplicationContext()).mSpotifyAppRemote.getPlayerApi().pause();
                            adapter.currentPosition = -1;
                        } else {
                            ((ParseApplication) getContext().getApplicationContext()).mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + allSongs.get(position).getSpotifyId());
                            adapter.currentPosition = position;
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        });

        // Set button to edit song list if the current user doesn't have any favorite songs
        btnEditSongs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to the settings page
                Intent i = new Intent(getContext(), SettingsActivity.class);
                i.putExtra("finisher", new ResultReceiver(null) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        //((MainActivity) getActivity()).onStop();
                        getActivity().finish();
                    }
                });
                i.putExtra("updater", new ResultReceiver(null) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        queryInitial();
                    }
                });
                startActivityForResult(i, RESULT_CODE);
            }
        });
    }

    public void queryInitial() {
        adapter.clear();
        querySongs(0);
    }

    // Query a list of top songs
    private void querySongs(int page) {
        ParseQuery<FavSongs> query = ParseQuery.getQuery(FavSongs.class);
        query.include(FavSongs.KEY_USER);
        query.include(FavSongs.KEY_SONG);
        query.whereEqualTo(FavSongs.KEY_USER, user);
        query.setSkip(20 * page);
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
                    allSongs.add(songItem.getSong());
                }
                if (allSongs.size() == 0) {
                    // If there are no fav songs for the user, show the message
                    tvNoSongs.setVisibility(View.VISIBLE);

                    if (user.getObjectId().equals(ParseUser.getCurrentUser().getObjectId())) {
                        btnEditSongs.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvNoSongs.setVisibility(View.GONE);
                    btnEditSongs.setVisibility(View.GONE);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}