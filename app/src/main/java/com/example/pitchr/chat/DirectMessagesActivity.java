package com.example.pitchr.chat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.pitchr.R;
import com.example.pitchr.databinding.ActivityDirectMessagesBinding;
import com.example.pitchr.helpers.EndlessRecyclerViewScrollListener;
import com.example.pitchr.models.Following;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
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
        adapter = new MessagesAdapter(this, allUsers);
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

    // Only get messages from people who are following you
    private void queryMessages(int page) {
        ParseQuery<Following> query = ParseQuery.getQuery(Following.class);
        query.include(Following.KEY_FOLLOWED_BY);
        query.include(Following.KEY_FOLLOWING);
        query.whereEqualTo(Following.KEY_FOLLOWING, ParseUser.getCurrentUser());
        query.setSkip(20 * page);
        query.setLimit(20); // Only show 20 users at a time
        query.addDescendingOrder(Following.KEY_CREATED_AT);
        query.findInBackground(new FindCallback<Following>() {
            @Override
            public void done(List<Following> userList, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting followers", e);
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Query followers success!");
                for (Following followingItem : userList) {
                    allUsers.add(followingItem.getFollowedBy());
                }
                if (allUsers.size() == 0) {
                    // If we have no users
                    Toast.makeText(getApplicationContext(), "You don't have any followers to message!", Toast.LENGTH_SHORT).show();
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}