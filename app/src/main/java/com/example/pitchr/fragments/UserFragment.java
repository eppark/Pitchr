package com.example.pitchr.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.pitchr.R;
import com.example.pitchr.adapters.UsersAdapter;
import com.example.pitchr.helpers.EndlessRecyclerViewScrollListener;
import com.example.pitchr.models.Following;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

public class UserFragment extends Fragment {

    public static final String TAG = UserFragment.class.getSimpleName();
    ParseUser user;
    RecyclerView rvUsers;
    UsersAdapter adapter;
    List<ParseUser> allUsers;
    SwipeRefreshLayout swipeContainer;
    EndlessRecyclerViewScrollListener scrollListener;
    String id;

    public UserFragment() {
        // Required empty public constructor
    }

    public static UserFragment newInstance(ParseUser user, String id) {
        UserFragment fragment = new UserFragment();
        Bundle args = new Bundle();
        args.putParcelable(ParseUser.class.getSimpleName(), Parcels.wrap(user));
        args.putString(String.class.getSimpleName(), id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get the user
        user = Parcels.unwrap(getArguments().getParcelable(ParseUser.class.getSimpleName()));
        allUsers = new ArrayList<>();
        id = getArguments().getString(String.class.getSimpleName());

        // Recycler view setup: layout manager and the adapter
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        rvUsers = (RecyclerView) getView().findViewById(R.id.rvItems);
        rvUsers.setLayoutManager(layoutManager);
        adapter = new UsersAdapter(this.getContext(), allUsers);
        rvUsers.setAdapter(adapter);

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
                // Load more data
                if (id.equals("Followers")) {
                    queryFollowers(page);
                } else {
                    queryFollowing(page);
                }
            }
        };
        // Add scroll listener to RecyclerView
        rvUsers.addOnScrollListener(scrollListener);

        // Populate the feed
        queryInitial();
    }

    private void queryInitial() {
        adapter.clear();
        if (id.equals("Followers")) {
            queryFollowers(0);
        } else {
            queryFollowing(0);
        }
    }

    // Query a list of followers
    private void queryFollowers(int page) {
        ParseQuery<Following> query = ParseQuery.getQuery(Following.class);
        query.include(Following.KEY_FOLLOWED_BY);
        query.include(Following.KEY_FOLLOWING);
        query.whereEqualTo(Following.KEY_FOLLOWING, user);
        query.setSkip(20 * page);
        query.setLimit(20); // Only show 20 users at a time
        query.addDescendingOrder(Following.KEY_CREATED_AT);
        query.findInBackground(new FindCallback<Following>() {
            @Override
            public void done(List<Following> userList, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting followers", e);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Query followers success!");
                for (Following followingItem : userList) {
                    allUsers.add(followingItem.getFollowedBy());
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    // Query a list of following
    private void queryFollowing(int page) {
        ParseQuery<Following> query = ParseQuery.getQuery(Following.class);
        query.include(Following.KEY_FOLLOWED_BY);
        query.include(Following.KEY_FOLLOWING);
        query.whereEqualTo(Following.KEY_FOLLOWED_BY, user);
        query.setSkip(20 * page);
        query.setLimit(20); // Only show 20 users at a time
        query.addDescendingOrder(Following.KEY_CREATED_AT);
        query.findInBackground(new FindCallback<Following>() {
            @Override
            public void done(List<Following> userList, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting following", e);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Query following success!");
                for (Following followingItem : userList) {
                    allUsers.add(followingItem.getFollowing());
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}