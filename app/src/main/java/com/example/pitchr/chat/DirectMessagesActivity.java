package com.example.pitchr.chat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.databinding.ActivityDirectMessagesBinding;
import com.example.pitchr.helpers.EndlessRecyclerViewScrollListener;
import com.example.pitchr.models.Following;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectMessagesActivity extends AppCompatActivity {

    private static final String TAG = DirectMessagesActivity.class.getSimpleName();
    protected List<ParseUser> allUsers;
    protected MessagesAdapter adapter;

    // Swipe to refresh and endless scrolling
    private EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // LOG TO ANALYTICS
        ParseApplication.logActivityEvent("directMessagesActivity");

        // Set ViewBinding
        final ActivityDirectMessagesBinding binding = ActivityDirectMessagesBinding.inflate(getLayoutInflater());

        // layout of activity is stored in a special property called root
        View view = binding.getRoot();
        setContentView(view);
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextAppearance(this, R.style.PitchrTextAppearance);
        getSupportActionBar().setTitle("Messages");
        binding.toolbar.setTitle("Messages");

        // Set message user list, adapter, and layout
        allUsers = new ArrayList<>();
        adapter = new MessagesAdapter(this, allUsers, this);
        binding.rvMessages.setAdapter(adapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        binding.rvMessages.setLayoutManager(linearLayoutManager);

        // Set the refresher
        // Setup refresh listener which triggers new data loading
        binding.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initialQuery();
                binding.swipeContainer.setRefreshing(false);
            }
        });
        // Configure the refreshing colors
        binding.swipeContainer.setColorSchemeResources(R.color.spotifyGreen);

        // Retain an instance for fresh searches
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                queryMessages(page);
            }
        };
        // Adds the scroll listener to RecyclerView
        binding.rvMessages.addOnScrollListener(scrollListener);

        // Get initial messages
        initialQuery();
    }

    // Initial query
    private void initialQuery() {
        adapter.clear();
        queryMessages(0);
    }

    // Only get messages from people who are following you and you are following
    private void queryMessages(int page) {
        // Get messages from people you are following
        ParseQuery<Following> followingParseQuery = ParseQuery.getQuery(Following.class);
        followingParseQuery.whereEqualTo(Following.KEY_FOLLOWING, ParseUser.getCurrentUser());

        // Get messages from people who are following you
        ParseQuery<Following> followerParseQuery = ParseQuery.getQuery(Following.class);
        followerParseQuery.whereEqualTo(Following.KEY_FOLLOWED_BY, ParseUser.getCurrentUser());

        // Now combine them
        List<ParseQuery<Following>> queries = new ArrayList<>();
        queries.add(followerParseQuery);
        queries.add(followingParseQuery);

        ParseQuery<Following> query = ParseQuery.or(queries);
        query.include(Following.KEY_FOLLOWED_BY);
        query.include(Following.KEY_FOLLOWING);
        query.setSkip(20 * page);
        query.setLimit(20); // Only show 20 users at a time
        query.addAscendingOrder(Following.KEY_UPDATED_AT);
        query.findInBackground(new FindCallback<Following>() {
            @Override
            public void done(List<Following> userList, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting followers", e);
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Query followers success!");

                // Get all the users
                for (Following followingItem : userList) {
                    ParseUser other;
                    if (followingItem.getFollowedBy().getObjectId().equals(ParseUser.getCurrentUser().getObjectId())) {
                        other = followingItem.getFollowing();
                    } else {
                        other = followingItem.getFollowedBy();
                    }
                    // We only want to add the user if it is new
                    boolean isNew = true;
                    for (ParseUser current : allUsers) {
                        if (current.getObjectId().equals(other.getObjectId())) {
                            isNew = false;
                            break;
                        }
                    }
                    if (isNew) {
                        allUsers.add(other);
                    }
                }
                if (allUsers.size() == 0) {
                    // If we have no users
                    Toast.makeText(getApplicationContext(), "You aren't following/followed by anyone to message!", Toast.LENGTH_SHORT).show();
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}