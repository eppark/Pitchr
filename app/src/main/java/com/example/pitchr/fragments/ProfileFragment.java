package com.example.pitchr.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.os.Environment;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.activities.MainActivity;
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

import org.parceler.Parcels;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ProfileFragment extends Fragment {

    public static final String TAG = ProfileFragment.class.getSimpleName();
    public static final int RESULT_CODE = 1337;
    public ParseUser user;
    TextView tvUsername;
    ImageView ivPfp;
    ImageView htabHeader;
    Button btnFollow;
    TabLayout htabTabs;
    ViewPager htabViewpager;
    ImageButton ibtnShare;
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
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ivPfp = view.findViewById(R.id.ivPfp);
        tvUsername = view.findViewById(R.id.tvUsername);
        btnFollow = view.findViewById(R.id.btnFollow);
        htabHeader = view.findViewById(R.id.htab_header);
        htabTabs = view.findViewById(R.id.htab_tabs);
        htabViewpager = view.findViewById(R.id.htab_viewpager);
        ibtnShare = view.findViewById(R.id.ibtnShare);
        ibtnShare.setVisibility(View.GONE); // Hide the share button at first

        // Set user info
        user = (ParseUser) Parcels.unwrap(getArguments().getParcelable(ParseUser.class.getSimpleName()));
        tvUsername.setText(user.getUsername());

        // Set the images if we have them
        setImages();

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

        // If this isn't the current user, show the follow button and hide the share button
        if (!user.getObjectId().equals(ParseUser.getCurrentUser().getObjectId())) {
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

                                    // LOG TO ANALYTICS
                                    ParseApplication.logEvent("followEvent", Arrays.asList("status", "type"), Arrays.asList("failure", "unfollow"));
                                    return;
                                }
                                Log.d(TAG, "Unfollow success");

                                // LOG TO ANALYTICS
                                ParseApplication.logEvent("followEvent", Arrays.asList("status", "type"), Arrays.asList("success", "unfollow"));

                                // Set variable
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

                                    // LOG TO ANALYTICS
                                    ParseApplication.logEvent("followEvent", Arrays.asList("status", "type"), Arrays.asList("failure", "follow"));
                                    return;
                                }
                                Log.d(TAG, "Follow success");

                                // LOG TO ANALYTICS
                                ParseApplication.logEvent("followEvent", Arrays.asList("status", "type"), Arrays.asList("success", "follow"));

                                // Set variable
                                following = true;
                                setupFollowStatus();
                            }
                        });
                    }
                }
            });

            // Hide the share button
            ibtnShare.setVisibility(View.GONE);
        } else {
            // If this is the current user, we can change the button to settings
            btnFollow.setSelected(false);
            btnFollow.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            btnFollow.setText(R.string.settings);

            // Set up settings page
            btnFollow.setOnClickListener(new View.OnClickListener() {
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
                            setImages();
                            ((FavSongsFragment) ((ViewPagerAdapter) htabViewpager.getAdapter()).getItem(0)).queryInitial();
                        }
                    });
                    startActivityForResult(i, RESULT_CODE);
                }
            });

            // Show the share button
            ibtnShare.setVisibility(View.VISIBLE);
            ibtnShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View buttonView) {
                    // LOG TO ANALYTICS
                    ParseApplication.logEvent("shareEvent", Arrays.asList("status"), Arrays.asList("success"));

                    // Get the view image
                    view.setDrawingCacheEnabled(true);
                    Bitmap bitmap = view.getDrawingCache();
                    Uri uri = saveImage(bitmap);

                    // Set up the sharing intent
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, "Check out my Pitchr profile!");
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    sharingIntent.setType("image/*");
                    Intent chooser = Intent.createChooser(sharingIntent, "Share using");

                    // Grant permissions
                    List<ResolveInfo> resInfoList = getContext().getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        getContext().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivity(chooser);
                }
            });
        }
    }

    // Set up images
    public void setImages() {
        ParseFile pfpImage = user.getParseFile("pfp");
        if (pfpImage != null) {
            Glide.with(this).load(pfpImage.getUrl()).circleCrop().into(ivPfp);
            Glide.with(this).load(pfpImage.getUrl()).into(htabHeader);
        } else {
            Glide.with(this).load(R.drawable.default_pfp).circleCrop().into(ivPfp);
            Glide.with(this).load(R.drawable.default_pfp).into(htabHeader);
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

    // Returns a Uri from a Bitmap
    private Uri saveImage(Bitmap image) {
        File imagesFolder = new File(getContext().getCacheDir(), "images");
        Uri uri = null;
        try {
            imagesFolder.mkdirs();
            File file = new File(imagesFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(getContext(), "com.pitchr.fileprovider", file);

        } catch (IOException e) {
            Log.d(TAG, "IOException while trying to write file for sharing: " + e.getMessage());
        }
        return uri;
    }
}