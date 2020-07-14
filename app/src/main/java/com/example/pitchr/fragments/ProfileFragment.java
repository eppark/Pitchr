package com.example.pitchr.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.pitchr.R;
import com.example.pitchr.activities.SettingsActivity;
import com.example.pitchr.adapters.ViewPagerAdapter;
import com.example.pitchr.models.Following;
import com.google.android.material.tabs.TabLayout;
import com.parse.DeleteCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.boltsinternal.Task;

import org.parceler.Parcels;

public class ProfileFragment extends Fragment {

    public static final String TAG = ProfileFragment.class.getSimpleName();
    public static final int RESULT_CODE = 1337;
    public ParseUser user;
    TextView tvUsername;
    ImageView ivPfp;
    Button btnFollow;
    TabLayout htabTabs;
    ViewPager htabViewpager;
    Following followingObject;
    public boolean following;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static ProfileFragment newInstance(ParseUser user) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putParcelable(ParseUser.class.getSimpleName(), Parcels.wrap(user));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ivPfp = view.findViewById(R.id.ivPfp);
        tvUsername = view.findViewById(R.id.tvUsername);
        btnFollow = view.findViewById(R.id.btnFollow);
        htabTabs = view.findViewById(R.id.htab_tabs);
        htabViewpager = view.findViewById(R.id.htab_viewpager);

        // Set user info
        user = (ParseUser) Parcels.unwrap(getArguments().getParcelable(ParseUser.class.getSimpleName()));
        tvUsername.setText(user.getUsername());

        // Set the images if we have them
        ParseFile pfpImage = user.getParseFile("pfp");
        if (pfpImage != null) {
            Glide.with(this).load(pfpImage.getUrl()).circleCrop().into(ivPfp);
        } else {
            Glide.with(this).load(R.drawable.default_pfp).circleCrop().into(ivPfp);
        }

        // Set up view pager and tabs
        setupViewPager();
        htabTabs.setupWithViewPager(htabViewpager);
        htabTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Change tabs accordingly
                htabViewpager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // If this isn't the current user, show the follow button
        if (!user.getUsername().equals(ParseUser.getCurrentUser().getUsername())) {
            ParseQuery<Following> query = ParseQuery.getQuery(Following.class);
            query.include(Following.KEY_FOLLOWED_BY);
            query.include(Following.KEY_FOLLOWING);
            query.whereEqualTo(Following.KEY_FOLLOWING, user);
            query.whereEqualTo(Following.KEY_FOLLOWED_BY, ParseUser.getCurrentUser());
            // If the user is currently following them, show that
            query.getFirstInBackground(new GetCallback<Following>() {
                @Override
                public void done(Following object, ParseException e) {
                    if (object == null) {
                        following = false;
                    } else {
                        followingObject = object;
                        following = true;
                    }
                    setupFollowStatus();
                }
            });

            // Set up following/unfollowing via button
            btnFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (following) {
                        followingObject.deleteInBackground(new DeleteCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e != null) {
                                    Log.e(TAG, "Issue with unfollowing", e);
                                    return;
                                }
                                Log.d(TAG, "Unfollow success");
                                following = false;
                                setupFollowStatus();
                            }
                        });
                    } else {
                        followingObject = new Following();
                        followingObject.put(Following.KEY_FOLLOWED_BY, ParseUser.getCurrentUser());
                        followingObject.put(Following.KEY_FOLLOWING, user);
                        followingObject.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e != null) {
                                    Log.e(TAG, "Issue with following", e);
                                    return;
                                }
                                Log.d(TAG, "Follow success");
                                following = true;
                                setupFollowStatus();
                            }
                        });
                    }
                }
            });
        } else {
            // If this is the current user, we can change the button to settings
            btnFollow.setSelected(false);
            btnFollow.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            btnFollow.setText("SETTINGS");

            // Set up settings page
            btnFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Go to the settings page
                    Intent i = new Intent(getContext(), SettingsActivity.class);
                    startActivityForResult(i, RESULT_CODE);
                }
            });
        }
    }

    // Set up fav songs, followers, and following tabs
    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());
        adapter.addFrag(FavSongsFragment.newInstance(user), "Fav Songs");
        adapter.addFrag(UserFragment.newInstance(user, "Followers"), "Followers");
        adapter.addFrag(UserFragment.newInstance(user, "Following"), "Following");
        htabViewpager.setAdapter(adapter);
    }

    // Change the button text/color accordingly
    private void setupFollowStatus() {
        if (following) {
            btnFollow.setSelected(true);
            btnFollow.setTextColor(ContextCompat.getColor(getContext(), R.color.spotifyGreen));
            btnFollow.setText("FOLLOWING");
        } else {
            btnFollow.setSelected(false);
            btnFollow.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            btnFollow.setText("FOLLOW");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CODE) {
            getActivity().finish();
        }
    }
}