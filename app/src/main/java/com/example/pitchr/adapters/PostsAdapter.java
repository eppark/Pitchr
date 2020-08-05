package com.example.pitchr.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.activities.MainActivity;
import com.example.pitchr.fragments.DetailsFragment;
import com.example.pitchr.fragments.ProfileFragment;
import com.example.pitchr.helpers.TimeFormatter;
import com.example.pitchr.models.Comment;
import com.example.pitchr.models.Post;
import com.example.pitchr.models.PostItem;
import com.example.pitchr.models.Song;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static com.example.pitchr.models.PostItem.TYPE_AD;
import static com.example.pitchr.models.PostItem.TYPE_POST;
import static com.example.pitchr.models.PostItem.TYPE_REC;

public class PostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = PostsAdapter.class.getSimpleName();
    private Context context;
    private List<PostItem> posts;
    int currentPosition;

    public PostsAdapter(Context context, List<PostItem> posts) {
        this.context = context;
        this.posts = posts;
        currentPosition = -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_POST) {
            view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
            return new PostViewHolder(view);
        } else if (viewType == TYPE_REC) {
            view = LayoutInflater.from(context).inflate(R.layout.item_recview, parent, false);
            return new RecViewHolder(view);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_ad, parent, false);
            return new AdViewHolder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return posts.get(position).type;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PostItem post = posts.get(position);

        if (getItemViewType(position) == TYPE_POST) {
            // Set play and paused correctly
            if (position == currentPosition) {
                if (((PostViewHolder) holder).paused) {
                    ((PostViewHolder) holder).ibtnPlay.setImageResource(R.drawable.ic_music_play);
                    ((PostViewHolder) holder).tvSongName.setSelected(false);
                    ((PostViewHolder) holder).tvArtists.setSelected(false);
                } else {
                    ((PostViewHolder) holder).ibtnPlay.setImageResource(R.drawable.ic_music_pause);
                    ((PostViewHolder) holder).tvSongName.setSelected(true);
                    ((PostViewHolder) holder).tvArtists.setSelected(true);
                }
            } else {
                ((PostViewHolder) holder).ibtnPlay.setImageResource(R.drawable.ic_music_play);
                ((PostViewHolder) holder).paused = false;
                ((PostViewHolder) holder).tvSongName.setSelected(false);
                ((PostViewHolder) holder).tvArtists.setSelected(false);
            }

            ((PostViewHolder) holder).bind(post);
        } else if (getItemViewType(position) == TYPE_REC) {
            ((RecViewHolder) holder).bind(post);
        } else {
            ((AdViewHolder) holder).pbLoading.setVisibility(View.VISIBLE);
            ((AdViewHolder) holder).bind(post.ad);
            ((AdViewHolder) holder).pbLoading.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    // Ad view holder
    class AdViewHolder extends RecyclerView.ViewHolder {

        UnifiedNativeAdView adView;
        ProgressBar pbLoading;

        public AdViewHolder(@NonNull View itemView) {
            super(itemView);
            pbLoading = (ProgressBar) itemView.findViewById(R.id.pbLoading);
            adView = (UnifiedNativeAdView) itemView.findViewById(R.id.ad_view);

            // Register the view used for each individual asset.
            adView.setHeadlineView(adView.findViewById(R.id.primary));
            adView.setBodyView(adView.findViewById(R.id.secondary));
            adView.setCallToActionView(adView.findViewById(R.id.cta));
            adView.setMediaView(adView.findViewById(R.id.media));
            adView.setStarRatingView(adView.findViewById(R.id.rating_bar));
        }

        private void bind(UnifiedNativeAd nativeAd) {
            // Some assets are guaranteed to be in every UnifiedNativeAd.
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());

            // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
            // check before trying to display them.
            if (nativeAd.getMediaContent() == null) {
                adView.getIconView().setVisibility(View.INVISIBLE);
            } else {
                adView.getMediaView().setMediaContent(nativeAd.getMediaContent());
                adView.getMediaView().setImageScaleType(ImageView.ScaleType.CENTER_CROP);
                adView.getMediaView().setVisibility(View.VISIBLE);
            }

            if (nativeAd.getStarRating() == null) {
                adView.getStarRatingView().setVisibility(View.GONE);
            } else {
                ((RatingBar) adView.getStarRatingView()).setRating(nativeAd.getStarRating().floatValue());
                adView.getStarRatingView().setVisibility(View.VISIBLE);
            }

            // Assign native ad object to the native view.
            adView.setNativeAd(nativeAd);
        }
    }

    // Review view holder
    class RecViewHolder extends RecyclerView.ViewHolder {

        ArrayList<Song> allRecSongs;
        SongsAdapter adapter;
        RecyclerView rvRecSongs;

        public RecViewHolder(@NonNull View itemView) {
            super(itemView);
            rvRecSongs = (RecyclerView) itemView.findViewById(R.id.rvRecSongs);
            itemView.setBackgroundColor(context.getResources().getColor(R.color.lightOrange));
        }

        public void bind(PostItem post) {
            // Set up recommended songs view
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            allRecSongs = post.recSongs;
            rvRecSongs.setLayoutManager(layoutManager);
            adapter = new SongsAdapter(context, allRecSongs, SongsAdapter.TYPE_REC);
            adapter.setOnItemClickListener(new SongsAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View itemView, int position) {
                    if (((ParseApplication) (context.getApplicationContext())).spotifyExists) {
                        // LOG TO ANALYTICS
                        ParseApplication.logEvent("recommendationClickEvent", Arrays.asList("status"), Arrays.asList("success"));

                        // Make sure the position is valid
                        if (position != RecyclerView.NO_POSITION) {
                            // Check if we're playing, pausing, or resuming
                            if (adapter.currentPosition == position) {
                                ((ParseApplication) context.getApplicationContext()).mSpotifyAppRemote.getPlayerApi().pause();
                                adapter.currentPosition = -1;
                            } else {
                                ((ParseApplication) context.getApplicationContext()).mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + allRecSongs.get(position).getSpotifyId());
                                adapter.currentPosition = position;
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            });
            rvRecSongs.setAdapter(adapter);
        }
    }

    // Post view holder
    class PostViewHolder extends RecyclerView.ViewHolder {

        Post currentPost;
        private TextView tvUsername;
        private ImageView ivPfp;
        private ImageView ivSongImage;
        private TextView tvSongName;
        private TextView tvArtists;
        private TextView tvCaption;
        private TextView tvLikes;
        private TextView tvComments;
        private ImageButton ibtnLike;
        private ImageButton ibtnComment;
        private TextView tvTime;
        private boolean liked;
        private ImageButton ibtnPlay;
        private boolean paused;
        int likes;
        View divider;

        public PostViewHolder(View itemView) {
            super(itemView);
            tvUsername = (TextView) itemView.findViewById(R.id.tvUsername);
            ivPfp = (ImageView) itemView.findViewById(R.id.ivPfp);
            ivSongImage = (ImageView) itemView.findViewById(R.id.ivSongImage);
            tvCaption = (TextView) itemView.findViewById(R.id.tvCaption);
            ibtnLike = (ImageButton) itemView.findViewById(R.id.ibtnLike);
            ibtnComment = (ImageButton) itemView.findViewById(R.id.ibtnComment);
            tvTime = (TextView) itemView.findViewById(R.id.tvTime);
            tvSongName = (TextView) itemView.findViewById(R.id.tvSongName);
            tvArtists = (TextView) itemView.findViewById(R.id.tvArtists);
            tvLikes = (TextView) itemView.findViewById(R.id.tvLikes);
            tvComments = (TextView) itemView.findViewById(R.id.tvComments);
            ibtnPlay = (ImageButton) itemView.findViewById(R.id.ibtnPlay);
            divider = (View) itemView.findViewById(R.id.divider);
            likes = 0;

            // Set background color
            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));

            // When the user clicks on text or a profile picture, take them to the profile page for that user
            View.OnClickListener profileListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FragmentTransaction ft = ((MainActivity) context).getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.replace(R.id.flContainer, ProfileFragment.newInstance(currentPost.getUser()), TAG);
                    ft.addToBackStack(TAG);
                    ft.commit();

                }
            };
            tvUsername.setOnClickListener(profileListener);
            ivPfp.setOnClickListener(profileListener);

            // When the user clicks on the post, take them to the details page for that post
            // When the user double taps a post, like the post
            View.OnTouchListener touchListener = new View.OnTouchListener() {
                private GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        FragmentTransaction ft = ((MainActivity) context).getSupportFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
                        ft.replace(R.id.flContainer, DetailsFragment.newInstance(currentPost), TAG);
                        ft.addToBackStack(TAG);
                        ft.commit();
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        setLike();
                        return true;
                    }
                });

                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    view.performClick();
                    return gestureDetector.onTouchEvent(motionEvent);
                }
            };
            tvCaption.setOnTouchListener(touchListener);
            tvTime.setOnTouchListener(touchListener);
            ibtnComment.setOnTouchListener(touchListener);
            itemView.setOnTouchListener(touchListener);

            // When the user clicks the heart, change accordingly
            ibtnLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setLike();
                }
            });

            // When a user clicks the play button, play the song
            ibtnPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (((ParseApplication) (context.getApplicationContext())).spotifyExists) {
                        // Check if we're playing, pausing, or resuming
                        if (currentPosition == getAdapterPosition()) {
                            if (paused) {
                                ((ParseApplication) context.getApplicationContext()).mSpotifyAppRemote.getPlayerApi().resume();
                                paused = false;
                            } else {
                                ((ParseApplication) context.getApplicationContext()).mSpotifyAppRemote.getPlayerApi().pause();
                                paused = true;
                            }
                        } else {
                            ((ParseApplication) context.getApplicationContext()).mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + currentPost.getSong().getSpotifyId());
                            paused = false;
                        }
                        currentPosition = getAdapterPosition();
                        notifyDataSetChanged();
                    }
                }
            });
        }

        public void bind(PostItem post) {
            currentPost = post.post;

            // Set Views
            tvCaption.setText(currentPost.getCaption());
            tvUsername.setText(currentPost.getUser().getUsername());

            // Set the time to the correct format
            tvTime.setText(TimeFormatter.getTimeDifference(currentPost.getCreatedAt().toString()));

            // Set the pfp image if we have it
            ParseFile pfpImage = currentPost.getUser().getParseFile("pfp");
            if (pfpImage != null) {
                Glide.with(context).load(pfpImage.getUrl()).circleCrop().into(ivPfp);
            } else {
                Glide.with(context).load(R.drawable.default_pfp).circleCrop().into(ivPfp);
            }

            // Show if the user likes the post and the comment count
            queryLiked();
            setCommentCount();

            // Set song views
            tvSongName.setText(currentPost.getSong().getName());
            tvArtists.setText(TextUtils.join(", ", currentPost.getSong().getArtists()));
            String image = currentPost.getSong().getImageUrl();
            if (image != null) {
                Glide.with(context).load(image).into(ivSongImage);
            } else {
                ivSongImage.setImageDrawable(context.getDrawable(R.drawable.music_placeholder));
            }
        }

        // Query if the post is liked from database
        protected void queryLiked() {
            currentPost.getLikes().getQuery().findInBackground(new FindCallback<ParseUser>() {
                @Override
                public void done(List<ParseUser> users, ParseException e) {
                    if (e != null) {
                        Log.e(TAG, "Issue with getting likes", e);
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
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

        // Set likes
        private void setLike() {
            if (!liked) {
                // LOG TO ANALYTICS
                ParseApplication.logEvent("likeEvent", Arrays.asList("type"), Arrays.asList("like"));

                currentPost.addLike();
                likes++;
                addLike();

                // Notify the other user that their post was liked
                String topic = String.format("/topics/%s", currentPost.getUser().getUsername());
                String notificationTitle = "Pitchr";
                String notificationMessage = String.format("%s liked your post about %s!", ParseUser.getCurrentUser().getUsername(), currentPost.getSong().getName());
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
                        notificationBody.put("icon", context.getString(R.string.default_app_icon_url));
                    }

                    // Set the topic
                    notification.put("to", topic);
                    notification.put("data", notificationBody);
                } catch (JSONException ex) {
                    Log.e(TAG, "onCreate error!", ex);
                }
                // Send the notification
                ParseApplication.sendNotification(notification, context.getApplicationContext());
            } else {
                // LOG TO ANALYTICS
                ParseApplication.logEvent("likeEvent", Arrays.asList("type"), Arrays.asList("unlike"));

                currentPost.removeLike();
                likes--;
                removeLike();
            }
            if (likes > 0) {
                tvLikes.setText(format(likes));
            } else {
                tvLikes.setText("");
            }
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

        // Set the count number of comments
        public void setCommentCount() {
            // Count the number of comments this post has
            ParseQuery<Comment> query = ParseQuery.getQuery(Comment.class);
            query.include(Comment.KEY_OPOST);
            query.whereEqualTo(Comment.KEY_OPOST, currentPost);
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

    // Clean all elements of the recycler
    public void clear() {
        posts.clear();
        notifyDataSetChanged();
    }

    // Add a list of items
    public void addAll(List<PostItem> list) {
        posts.addAll(list);
        notifyDataSetChanged();
    }
}
