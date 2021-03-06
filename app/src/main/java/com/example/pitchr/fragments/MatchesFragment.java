package com.example.pitchr.fragments;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.Toast;

import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.adapters.MatchesAdapter;
import com.example.pitchr.helpers.LinePagerIndicatorDecoration;
import com.example.pitchr.models.FavSongs;
import com.example.pitchr.models.Following;
import com.example.pitchr.models.Match;
import com.example.pitchr.models.Match2;
import com.example.pitchr.models.MatchItem;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MatchesFragment extends Fragment {

    private static final String TAG = MatchesFragment.class.getSimpleName();
    protected RecyclerView rvMatches;
    protected MatchesAdapter matchesAdapter;
    protected List<MatchItem> allMatches;
    Button btnFindMatches;
    TextSwitcher tsMatches;
    ImageView ivLoadingAnimation;
    private List<ParseUser> followingList;
    int attempts;

    public MatchesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        return inflater.inflate(R.layout.fragment_matches, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // LOG TO ANALYTICS
        ParseApplication.logActivityEvent("matchesFragment");

        // View binding
        rvMatches = (RecyclerView) view.findViewById(R.id.rvMatches);
        btnFindMatches = (Button) view.findViewById(R.id.btnFindMatches);
        tsMatches = (TextSwitcher) view.findViewById(R.id.tsFindUsers);
        tsMatches.setCurrentText(getString(R.string.find_users_with_the_same_music_tastes_as_you));
        tsMatches.setInAnimation(getContext(), android.R.anim.fade_in);
        tsMatches.setOutAnimation(getContext(), android.R.anim.fade_out);
        ivLoadingAnimation = (ImageView) view.findViewById(R.id.ivAnimation);

        // Show the animation first
        ivLoadingAnimation.setVisibility(View.VISIBLE);
        AnimationDrawable animationDrawable = (AnimationDrawable) ivLoadingAnimation.getBackground();
        animationDrawable.setEnterFadeDuration(10);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();
        btnFindMatches.setVisibility(View.VISIBLE);
        tsMatches.setVisibility(View.VISIBLE);
        btnFindMatches.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.gray2));
        btnFindMatches.setEnabled(true);
        attempts = 0;

        // Set posts, adapter, and layout (horizontal scrolling)
        allMatches = new ArrayList<>();
        matchesAdapter = new MatchesAdapter(getContext(), allMatches);
        rvMatches.setAdapter(matchesAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvMatches.setLayoutManager(linearLayoutManager);

        // Add pager behavior
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvMatches);
        rvMatches.addItemDecoration(new LinePagerIndicatorDecoration());

        // When the find matches button is clicked, we need to query matches from the database
        btnFindMatches.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check that the user really does have some favorite songs first
                ParseQuery<FavSongs> query = ParseQuery.getQuery(FavSongs.class);
                query.whereEqualTo(FavSongs.KEY_USER, ParseUser.getCurrentUser());
                query.getFirstInBackground(new GetCallback<FavSongs>() {
                    @Override
                    public void done(FavSongs object, ParseException e) {
                        // If we have no favorite songs, we can't match anything! :(
                        if (object == null) {
                            Toast.makeText(getContext(), "Add songs on your profile first!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        // Else, we can find some matches for the user
                        btnFindMatches.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.gray1));
                        btnFindMatches.setEnabled(false);
                        tsMatches.setText(getString(R.string.finding_matches_for_you));

                        // Get the matches for the user depending on the version
                        getMatches(((ParseApplication) getContext().getApplicationContext()).version);
                    }
                });
            }
        });
    }

    // Get matches for the user
    private void getMatches(int version) {
        // Get the following, since we don't want matches with any users we are following
        followingList = new ArrayList<>();
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
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Query following success!");
                for (Following followingItem : userList) {
                    followingList.add(followingItem.getFollowing());
                }

                // Now we can query for matches depending on the version
                switch (version) {
                    case 1:
                        getMatchList();
                        break;
                    case 2:
                    default:
                        getMatch2List();
                        break;
                }
            }
        });
    }

    // Get matches (that aren't users the current already follows)
    // This function gets matches that follow algorithm 1
    private void getMatchList() {
        // Query
        ParseQuery<Match> matchQuery = ParseQuery.getQuery(Match.class);
        matchQuery.include(Match.KEY_TO);
        matchQuery.whereEqualTo(Match.KEY_FROM, ParseUser.getCurrentUser());
        matchQuery.whereNotContainedIn(Match.KEY_TO, Arrays.asList(followingList));
        matchQuery.addDescendingOrder(Match.KEY_PERCENT);
        matchQuery.findInBackground(new FindCallback<Match>() {

            @Override
            public void done(List<Match> matches, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting matches", e);
                    Toast.makeText(getContext(), "Failed to get matches", Toast.LENGTH_SHORT).show();

                    // LOG TO ANALYTICS
                    ParseApplication.logEvent("matchEvent", Arrays.asList("status", "type", "version"), Arrays.asList("failure", "null", "1"));
                    return;
                }
                Log.d(TAG, "Query matches success!");

                if (matches.size() > 0) {
                    // LOG TO ANALYTICS
                    ParseApplication.logEvent("matchEvent", Arrays.asList("status", "type", "version"), Arrays.asList("success", "existing", "1"));

                    // If the matches already exists, just use that
                    allMatches.clear();

                    for (int i = 0; i < 5; i++) {
                        if (i < matches.size()) {
                            allMatches.add(new MatchItem(matches.get(i).getTo(), matches.get(i).getPercent()));
                        }
                    }
                    matchesAdapter.notifyDataSetChanged();

                    // Show the recycler view now!
                    tsMatches.setVisibility(View.INVISIBLE);
                    btnFindMatches.setVisibility(View.INVISIBLE);
                    ivLoadingAnimation.setVisibility(View.INVISIBLE);
                    tsMatches.setVisibility(View.GONE);
                    btnFindMatches.setVisibility(View.GONE);
                    ivLoadingAnimation.setVisibility(View.GONE);
                } else {
                    // If matches don't exist, create them
                    if (attempts < 1) {
                        attempts++; // We don't want to loop forever

                        // Call Cloud function
                        HashMap<String, Object> params = new HashMap<String, Object>();
                        params.put("currentuser", ParseUser.getCurrentUser().getObjectId());
                        ParseCloud.callFunctionInBackground("findMatchForUser", params, new FunctionCallback<Object>() {
                            @Override
                            public void done(Object object, ParseException e) {
                                if (e != null) {
                                    // Log the failure
                                    Log.e(TAG, "Issue with creating match1", e);
                                    Toast.makeText(getContext(), "Failed to create matches", Toast.LENGTH_SHORT).show();

                                    // LOG TO ANALYTICS
                                    ParseApplication.logEvent("matchEvent", Arrays.asList("status", "type", "version"), Arrays.asList("failure", "new", "1"));
                                    return;
                                }
                                // Else we are successful!
                                // LOG TO ANALYTICS
                                ParseApplication.logEvent("matchEvent", Arrays.asList("status", "type", "version"), Arrays.asList("success", "new", "1"));

                                getMatchList();
                            }
                        });
                    } else if (attempts < 5) {
                        attempts++;
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getMatchList();
                            }
                        }, 3000); // Wait 3 seconds to ensure we don't spam the server
                    }
                }
            }
        });
    }

    // Get matches2 (that aren't users the current already follows)
    // This function gets matches that follow algorithm 2
    private void getMatch2List() {
        // Query
        ParseQuery<Match2> matchQuery = ParseQuery.getQuery(Match2.class);
        matchQuery.include(Match2.KEY_TO);
        matchQuery.whereEqualTo(Match2.KEY_FROM, ParseUser.getCurrentUser());
        matchQuery.whereNotContainedIn(Match2.KEY_TO, Arrays.asList(followingList));
        matchQuery.addDescendingOrder(Match2.KEY_PERCENT);
        matchQuery.findInBackground(new FindCallback<Match2>() {

            @Override
            public void done(List<Match2> matches, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting matches", e);
                    Toast.makeText(getContext(), "Failed to get matches", Toast.LENGTH_SHORT).show();

                    // LOG TO ANALYTICS
                    ParseApplication.logEvent("matchEvent", Arrays.asList("status", "type", "version"), Arrays.asList("failure", "null", "2"));
                    return;
                }
                Log.d(TAG, "Query matches success!");

                if (matches.size() > 0) {
                    // LOG TO ANALYTICS
                    ParseApplication.logEvent("matchEvent", Arrays.asList("status", "type", "version"), Arrays.asList("success", "existing", "2"));

                    // If the matches already exists, just use that
                    allMatches.clear();

                    for (int i = 0; i < 5; i++) {
                        if (i < matches.size()) {
                            allMatches.add(new MatchItem(matches.get(i).getTo(), matches.get(i).getPercent()));
                        }
                    }
                    matchesAdapter.notifyDataSetChanged();

                    // Show the recycler view now!
                    tsMatches.setVisibility(View.INVISIBLE);
                    btnFindMatches.setVisibility(View.INVISIBLE);
                    ivLoadingAnimation.setVisibility(View.INVISIBLE);
                    tsMatches.setVisibility(View.GONE);
                    btnFindMatches.setVisibility(View.GONE);
                    ivLoadingAnimation.setVisibility(View.GONE);
                } else {
                    // If matches don't exist, create them
                    if (attempts < 1) {
                        attempts++; // We don't want to loop forever

                        // Call Cloud function
                        HashMap<String, Object> params = new HashMap<String, Object>();
                        params.put("currentuser", ParseUser.getCurrentUser().getObjectId());
                        ParseCloud.callFunctionInBackground("findMatch2ForUser", params, new FunctionCallback<Object>() {
                            @Override
                            public void done(Object object, ParseException e) {
                                if (e != null) {
                                    // Log the failure
                                    Log.e(TAG, "Issue with creating match2", e);
                                    Toast.makeText(getContext(), "Failed to create matches", Toast.LENGTH_SHORT).show();

                                    // LOG TO ANALYTICS
                                    ParseApplication.logEvent("matchEvent", Arrays.asList("status", "type", "version"), Arrays.asList("failure", "new", "2"));
                                    return;
                                }
                                // Else we are successful!
                                // LOG TO ANALYTICS
                                ParseApplication.logEvent("matchEvent", Arrays.asList("status", "type", "version"), Arrays.asList("success", "new", "2"));

                                getMatch2List();
                            }
                        });
                    } else if (attempts < 5) {
                        attempts++;
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getMatch2List();
                            }
                        }, 3000); // Wait 3 seconds to ensure we don't spam the server
                    }
                }
            }
        });
    }
}