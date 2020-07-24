package com.example.pitchr.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.adapters.UsersAdapter;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import me.xdrop.fuzzywuzzy.FuzzySearch;

public class SearchUsersFragment extends Fragment {

    public static final String TAG = SearchUsersFragment.class.getSimpleName();
    private UsersAdapter adapter;
    private List<ParseUser> allUsers;
    private static final int LIMIT = 20;
    RecyclerView rvUsers;
    TextView tvNoUsers;
    ProgressBar pbProgressAction;
    String userQuery;

    public SearchUsersFragment() {
        // Required empty public constructor
    }

    public static SearchUsersFragment newInstance(String param1, String param2) {
        SearchUsersFragment fragment = new SearchUsersFragment();
        return fragment;
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
        return inflater.inflate(R.layout.fragment_search_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // LOG TO ANALYTICS
        ParseApplication.logEvent("searchUsersFragment", Arrays.asList("status"), Arrays.asList("success"));

        view.setBackgroundColor(getResources().getColor(R.color.gray3));

        // Set adapter, recycler view, and list
        allUsers = new ArrayList<>();
        rvUsers = (RecyclerView) getView().findViewById(R.id.rvUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UsersAdapter(getContext(), allUsers);
        rvUsers.setAdapter(adapter);
        tvNoUsers = (TextView) view.findViewById(R.id.tvNoUsers);
        tvNoUsers.setVisibility(View.VISIBLE);

        // Hide the progress bar at first
        pbProgressAction = (ProgressBar) view.findViewById(R.id.pbProgressAction);
        pbProgressAction.setVisibility(View.GONE);
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();
        inflater.inflate(R.menu.menu_search_list, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint("Search users...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                pbProgressAction.setVisibility(View.VISIBLE);

                // Perform query here
                allUsers.clear();
                userQuery = query;
                fetchUsers(userQuery);

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

        super.onCreateOptionsMenu(menu, inflater);
    }

    // Uses fuzzy matching to find the users that best match the results
    private void fetchUsers(String query) {
        ParseQuery<ParseUser> userQuery = ParseQuery.getQuery(ParseUser.class);
        userQuery.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> userList, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting followers", e);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Query followers success!");
                TreeMap<Integer, List<ParseUser>> userMap = new TreeMap<>(Collections.reverseOrder());
                for (ParseUser currentUser : userList) {
                    // Use Fuzzy search to put all the users in descending order (100 match is highest, 0 match is lowest)
                    int ratio = FuzzySearch.ratio(query, currentUser.getUsername());
                    if (ratio >= 75) {
                        if (userMap.containsKey(ratio)) {
                            List<ParseUser> temp = userMap.get(ratio);
                            temp.add(currentUser);
                            userMap.put(ratio, temp);
                        } else {
                            userMap.put(ratio, new ArrayList<>(Arrays.asList(currentUser)));
                        }
                    }
                }
                // Now we can put the sorted values into our list
                for(Map.Entry<Integer, List<ParseUser>> entry : userMap.entrySet()) {
                    allUsers.addAll(entry.getValue());
                }

                // If we have no matching users, let the user know
                if (allUsers.size() == 0) {
                    tvNoUsers.setVisibility(View.VISIBLE);
                    tvNoUsers.setText(R.string.no_users_selected);
                } else {
                    tvNoUsers.setVisibility(View.GONE);
                }
                pbProgressAction.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            }
        });
    }
}