package com.example.pitchr.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.activities.MainActivity;
import com.example.pitchr.adapters.CommentsAdapter;
import com.example.pitchr.helpers.EndlessRecyclerViewScrollListener;
import com.example.pitchr.helpers.TimeFormatter;
import com.example.pitchr.models.Comment;
import com.example.pitchr.models.Post;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DetailsFragment extends Fragment {

    public static final String TAG = DetailsFragment.class.getSimpleName();
    public RecyclerView rvComments;
    public CommentsAdapter adapter;
    public List<Comment> allComments;
    protected Post post;
    TextView tvUsername;
    ImageView ivPfp;
    TextView tvTime;
    TextView tvCaption;
    ImageButton ibtnLike;
    ImageButton ibtnComment;
    TextView tvLikes;
    TextView tvComments;
    ImageView ivSongImage;
    TextView tvSongName;
    TextView tvArtists;
    ImageButton ibtnPlay;
    int likes;
    boolean liked;
    int paused;

    // Swipe to refresh and scroll to load more comments endlessly
    private SwipeRefreshLayout swipeContainer;
    private EndlessRecyclerViewScrollListener scrollListener;

    public DetailsFragment() {
        // Required empty public constructor
    }

    public static DetailsFragment newInstance(Post post) {
        DetailsFragment fragment = new DetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(Post.class.getSimpleName(), Parcels.wrap(post));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_details, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // LOG TO ANALYTICS
        ParseApplication.logActivityEvent("detailsFragment");

        // View binding
        rvComments = (RecyclerView) view.findViewById(R.id.rvComments);
        tvUsername = (TextView) view.findViewById(R.id.tvUsername);
        ivPfp = (ImageView) view.findViewById(R.id.ivPfp);
        tvTime = (TextView) view.findViewById(R.id.tvTime);
        tvCaption = (TextView) view.findViewById(R.id.tvCaption);
        tvSongName = (TextView) view.findViewById(R.id.tvSongName);
        tvArtists = (TextView) view.findViewById(R.id.tvArtists);
        ivSongImage = (ImageView) view.findViewById(R.id.ivSongImage);
        ibtnComment = (ImageButton) view.findViewById(R.id.ibtnComment);
        ibtnLike = (ImageButton) view.findViewById(R.id.ibtnLike);
        tvLikes = (TextView) view.findViewById(R.id.tvLikes);
        tvComments = (TextView) view.findViewById(R.id.tvComments);
        ibtnPlay = (ImageButton) view.findViewById(R.id.ibtnPlay);
        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        paused = -1;

        // Get the post details
        post = Parcels.unwrap(getArguments().getParcelable(Post.class.getSimpleName()));
        ParseUser op = post.getUser();

        // Set views
        tvUsername.setText(op.getUsername());
        tvCaption.setText(post.getCaption());
        tvSongName.setText(post.getSong().getName());
        tvArtists.setText(TextUtils.join(", ", post.getSong().getArtists()));

        // Set the time to the correct format
        tvTime.setText(TimeFormatter.getTimeStamp(post.getCreatedAt().toString()));

        // Set the images if we have them
        String image = post.getSong().getImageUrl();
        if (image != null) {
            Glide.with(this).load(image).into(ivSongImage);
        } else {
            ivSongImage.setImageDrawable(getResources().getDrawable(R.drawable.music_placeholder));
        }
        ParseFile pfpImage = post.getUser().getParseFile("pfp");
        if (pfpImage != null) {
            Glide.with(this).load(pfpImage.getUrl()).circleCrop().into(ivPfp);
        } else {
            Glide.with(this).load(R.drawable.default_pfp).circleCrop().into(ivPfp);
        }

        // Set comments, adapter, and layout
        allComments = new ArrayList<>();
        adapter = new CommentsAdapter(getContext(), allComments);
        rvComments.setAdapter(adapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        rvComments.setLayoutManager(linearLayoutManager);

        // When the user clicks on text or a profile picture, take them to the profile page for that user
        View.OnClickListener profileListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTransaction ft = ((MainActivity) getContext()).getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.replace(R.id.flContainer, ProfileFragment.newInstance(op), TAG);
                ft.addToBackStack(TAG);
                ft.commit();
            }
        };
        tvUsername.setOnClickListener(profileListener);
        ivPfp.setOnClickListener(profileListener);

        // Set the refresher
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                adapter.clear();
                Log.d(TAG, "Querying for refresh ");
                queryComments(0);
                swipeContainer.setRefreshing(false);
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(R.color.spotifyGreen);

        // Retain an instance for fresh searches
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                Log.d(TAG, "Querying for load more ");
                queryComments(page);
            }
        };
        // Adds the scroll listener to RecyclerView
        rvComments.addOnScrollListener(scrollListener);

        // Get comments
        Log.d(TAG, "Querying for initial retrieval ");
        queryComments(0);

        // See if the user liked the post
        liked = false;
        queryLiked();

        // When the user clicks the heart, change accordingly
        ibtnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!liked) {
                    // LOG TO ANALYTICS
                    ParseApplication.logEvent("likeEvent", Arrays.asList("type"), Arrays.asList("like"));

                    post.addLike();
                    likes++;
                    addLike();

                    // Notify the other user that their post was liked
                    String topic = String.format("/topics/%s", post.getUser().getUsername());
                    String notificationTitle = "Pitchr";
                    String notificationMessage = String.format("%s liked your post about %s!", ParseUser.getCurrentUser().getUsername(), post.getSong().getName());
                    String icon = ((ParseFile) ParseUser.getCurrentUser().get("pfp")) != null ? ((ParseFile) ParseUser.getCurrentUser().get("pfp")).getUrl() : "";

                    JSONObject notification = new JSONObject();
                    JSONObject notificationBody = new JSONObject();
                    try {
                        // Set the message
                        notificationBody.put("title", notificationTitle);
                        notificationBody.put("message", notificationMessage);
                        if (!icon.isEmpty()) {
                            notificationBody.put("icon", icon);
                        } else {
                            notificationBody.put("icon", getString(R.string.default_app_icon_url));
                        }

                        // Set the topic
                        notification.put("to", topic);
                        notification.put("data", notificationBody);
                    } catch (JSONException ex) {
                        Log.e(TAG, "onCreate error!", ex);
                    }
                    // Send the notification
                    ParseApplication.sendNotification(notification, getContext().getApplicationContext());
                } else {
                    // LOG TO ANALYTICS
                    ParseApplication.logEvent("likeEvent", Arrays.asList("type"), Arrays.asList("unlike"));

                    post.removeLike();
                    likes--;
                    removeLike();
                }
                if (likes > 0) {
                    tvLikes.setText(format(likes));
                } else {
                    tvLikes.setText("");
                }
            }
        });

        // Set comment listener
        ibtnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommentDialogFragment commentDialogFragment = CommentDialogFragment.newInstance(post);
                commentDialogFragment.show(((MainActivity) view.getContext()).fragmentManager, "fragment_comment_dialog");
            }
        });

        // Set play button
        ibtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((ParseApplication) (getContext().getApplicationContext())).spotifyExists) {
                    // Check if we're playing, pausing, or resuming
                    if (paused != -1) {
                        if (paused == 1) {
                            ibtnPlay.setImageResource(R.drawable.ic_music_pause);
                            ((ParseApplication) getContext().getApplicationContext()).mSpotifyAppRemote.getPlayerApi().resume();
                            paused = 0;
                            tvSongName.setSelected(true);
                            tvArtists.setSelected(true);
                        } else {
                            ibtnPlay.setImageResource(R.drawable.ic_music_play);
                            ((ParseApplication) getContext().getApplicationContext()).mSpotifyAppRemote.getPlayerApi().pause();
                            paused = 1;
                            tvSongName.setSelected(false);
                            tvArtists.setSelected(false);
                        }
                    } else {
                        ibtnPlay.setImageResource(R.drawable.ic_music_pause);
                        ((ParseApplication) getContext().getApplicationContext()).mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + post.getSong().getSpotifyId());
                        paused = 0;
                        tvSongName.setSelected(true);
                        tvArtists.setSelected(true);
                    }
                }
            }
        });
    }

    // Query comments from database
    protected void queryComments(int page) {
        ParseQuery<Comment> query = ParseQuery.getQuery(Comment.class);
        query.include(Comment.KEY_AUTHOR);
        query.setSkip(20 * page);
        query.whereEqualTo(Comment.KEY_OPOST, post);
        query.setLimit(20); // Only show 20 comments
        query.addDescendingOrder(Comment.KEY_CREATED_AT);
        query.findInBackground(new FindCallback<Comment>() {
            @Override
            public void done(List<Comment> comments, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting comments", e);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Query comments success!");
                allComments.addAll(comments);
                adapter.notifyDataSetChanged();
                setCommentCount();
            }
        });
    }

    // Set the count number of comments
    public void setCommentCount() {
        // Count the number of comments this post has
        ParseQuery<Comment> query = ParseQuery.getQuery(Comment.class);
        query.include(Comment.KEY_OPOST);
        query.whereEqualTo(Comment.KEY_OPOST, post);
        query.countInBackground(new CountCallback() {
            @Override
            public void done(int count, ParseException e) {
                if (count > 0) {
                    tvComments.setText(format(count));
                } else {
                    tvComments.setText("");
                }
            }
        });
    }

    // Query if the post is liked from database
    protected void queryLiked() {
        post.getLikes().getQuery().findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> users, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting likes", e);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Query likes success!");
                // If the user liked the post, show that. Otherwise, show the post is not liked
                removeLike();
                likes = users.size();
                if (likes > 0) {
                    tvLikes.setText(format(likes));
                } else {
                    tvLikes.setText("");
                }
                for (int i = 0; i < users.size(); i++) {
                    if (users.get(i).getObjectId().equals(ParseUser.getCurrentUser().getObjectId())) {
                        addLike();
                        break;
                    }
                }
            }
        });
    }

    private void addLike() {
        liked = true;
        ibtnLike.setSelected(true);
        ibtnLike.setImageResource(R.drawable.ic_heart_filled);
    }

    private void removeLike() {
        liked = false;
        ibtnLike.setSelected(false);
        ibtnLike.setImageResource(R.drawable.ic_heart);
    }

    // Truncate counts in a readable format
    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();

    static {
        suffixes.put(1_000L, "k");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "G");
        suffixes.put(1_000_000_000_000L, "T");
        suffixes.put(1_000_000_000_000_000L, "P");
        suffixes.put(1_000_000_000_000_000_000L, "E");
    }

    public static String format(long value) {
        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return Long.toString(value);

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10);
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }
}