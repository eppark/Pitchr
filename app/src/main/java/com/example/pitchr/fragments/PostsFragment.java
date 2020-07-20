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

import com.example.pitchr.R;
import com.example.pitchr.activities.MainActivity;
import com.example.pitchr.activities.SearchActivity;
import com.example.pitchr.adapters.PostsAdapter;
import com.example.pitchr.chat.DirectMessagesActivity;
import com.example.pitchr.helpers.EndlessRecyclerViewScrollListener;
import com.example.pitchr.models.Following;
import com.example.pitchr.models.Post;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class PostsFragment extends Fragment {

    private static final String TAG = PostsFragment.class.getSimpleName();
    private static final int RESULT_CODE = 11037;
    protected RecyclerView rvPosts;
    protected PostsAdapter adapter;
    protected List<Post> allPosts;
    protected List<ParseUser> following;
    Button btnFindUsers;
    TextView tvNoPosts;
    ProgressBar pbLoading;
    FloatingActionButton fabCompose;

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
        rvPosts = (RecyclerView) view.findViewById(R.id.rvPosts);
        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        pbLoading = (ProgressBar) view.findViewById(R.id.pbLoading);
        fabCompose = (FloatingActionButton) view.findViewById(R.id.fabCompose);
        pbLoading.setVisibility(View.GONE); // Hide progress bar at first
        btnFindUsers = (Button) view.findViewById(R.id.btnFindUsers);
        tvNoPosts = (TextView) view.findViewById(R.id.tvNoPosts);
        tvNoPosts.setVisibility(View.GONE);
        btnFindUsers.setVisibility(View.GONE); // Hide lack of posts at first

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
                Intent i = new Intent(getContext(), SearchActivity.class);
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
        queryFollowing(0);
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
                allPosts.addAll(posts);
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
}