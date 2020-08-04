package com.example.pitchr.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.activities.MainActivity;
import com.example.pitchr.activities.SettingsActivity;
import com.example.pitchr.adapters.ViewPagerAdapter;
import com.example.pitchr.models.Following;
import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.material.tabs.TabLayout;
import com.parse.DeleteCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

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
    ImageButton ibtnFacebook;
    ImageButton ibtnTwitter;
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

        // LOG TO ANALYTICS
        ParseApplication.logActivityEvent("profileFragment");

        // View binding
        ivPfp = view.findViewById(R.id.ivPfp);
        tvUsername = view.findViewById(R.id.tvUsername);
        btnFollow = view.findViewById(R.id.btnFollow);
        htabHeader = view.findViewById(R.id.htab_header);
        htabTabs = view.findViewById(R.id.htab_tabs);
        htabViewpager = view.findViewById(R.id.htab_viewpager);
        ibtnFacebook = view.findViewById(R.id.ibtnFacebook);
        ibtnTwitter = view.findViewById(R.id.ibtnTwitter);
        ibtnFacebook.setVisibility(View.GONE); // Hide the share buttons at first
        ibtnTwitter.setVisibility(View.GONE);

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

                                // Notify the other user that they were followed
                                String topic = String.format("/topics/%s", user.getUsername());
                                String notificationTitle = "Pitchr";
                                String notificationMessage = String.format("%s is now following you!", ParseUser.getCurrentUser().getUsername());

                                JSONObject notification = new JSONObject();
                                JSONObject notificationBody = new JSONObject();
                                try {
                                    // Set the message
                                    notificationBody.put("title", notificationTitle);
                                    notificationBody.put("message", notificationMessage);

                                    // Set the topic
                                    notification.put("to", topic);
                                    notification.put("data", notificationBody);
                                } catch (JSONException ex) {
                                    Log.e(TAG, "onCreate error!", ex);
                                }
                                // Send the notification
                                ParseApplication.sendNotification(notification, getContext().getApplicationContext());

                            }
                        });
                    }
                }
            });

            // Hide the share buttons
            ibtnFacebook.setVisibility(View.GONE);
            ibtnTwitter.setVisibility(View.GONE);
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
                            ((MainActivity) getActivity()).onStop();
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

            // Show the share buttons
            ibtnFacebook.setVisibility(View.VISIBLE);
            ibtnFacebook.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View buttonView) {
                    // LOG TO ANALYTICS
                    ParseApplication.logShareEvent("Facebook", "fav_songs");

                    // Get the view image of favorite songs
                    RecyclerView layoutView = ((FavSongsFragment) ((ViewPagerAdapter) htabViewpager.getAdapter()).getItem(0)).rvSongs;
                    if (layoutView.getAdapter().getItemCount() == 0) {
                        Toast.makeText(getContext(), "Add some favorite songs first!", Toast.LENGTH_SHORT).show();
                    } else {
                        layoutView.measure(View.MeasureSpec.makeMeasureSpec(layoutView.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        Bitmap bitmap = Bitmap.createBitmap(layoutView.getWidth(), layoutView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                        layoutView.draw(new Canvas(bitmap));

                        // Share to Facebook
                        SharePhoto photo = new SharePhoto.Builder()
                                .setBitmap(bitmap)
                                .build();
                        SharePhotoContent content = new SharePhotoContent.Builder()
                                .addPhoto(photo)
                                .setShareHashtag(new ShareHashtag.Builder()
                                        .setHashtag("#Pitchr")
                                        .build())
                                .build();
                        ShareDialog.show(ProfileFragment.this, content);
                    }
                }
            });

            ibtnTwitter.setVisibility(View.VISIBLE);
            ibtnTwitter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View buttonView) {
                    // LOG TO ANALYTICS
                    ParseApplication.logShareEvent("Twitter", "fav_songs");

                    // Get the view image of favorite songs
                    ViewPagerAdapter adapter = (ViewPagerAdapter) htabViewpager.getAdapter();
                    RecyclerView layoutView = ((FavSongsFragment) adapter.getItem(adapter.getPageIndex("Fav Songs"))).rvSongs;
                    if (layoutView.getAdapter().getItemCount() == 0) {
                        Toast.makeText(getContext(), "Add some favorite songs first!", Toast.LENGTH_SHORT).show();
                    } else {
                        layoutView.measure(View.MeasureSpec.makeMeasureSpec(layoutView.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        Bitmap bitmap = Bitmap.createBitmap(layoutView.getWidth(), layoutView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                        layoutView.draw(new Canvas(bitmap));

                        // Save and get URI
                        Uri uri = bitmapToUri(bitmap);
                        if (uri == null) {
                            Log.d(TAG, "Failed to save image!");
                            Toast.makeText(getContext(), "Failed to generate share image!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Authorize Twitter
                        TwitterConfig config = new TwitterConfig.Builder(getContext())
                                .logger(new DefaultLogger(Log.DEBUG))
                                .twitterAuthConfig(new TwitterAuthConfig(getString(R.string.twitter_consumer_key), getString(R.string.twitter_consumer_secret)))
                                .debug(true)
                                .build();
                        Twitter.initialize(config);

                        // Share to Twitter
                        TweetComposer.Builder builder = new TweetComposer.Builder(getActivity())
                                .text("Check out my favorite songs on #Pitchr !")
                                .image(uri);
                        builder.show();
                    }
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
            btnFollow.setText(R.string.following);
        } else {
            btnFollow.setSelected(false);
            btnFollow.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            btnFollow.setText(R.string.follow);
        }
    }

    // Returns a Uri from a Bitmap
    private Uri bitmapToUri(Bitmap image) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            String path = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), image, "share_image", null);
            return Uri.parse(path);
        } catch (Exception e) {
            Log.e(TAG, "Exception while trying to write file for sharing!", e);
            return null;
        }
    }
}