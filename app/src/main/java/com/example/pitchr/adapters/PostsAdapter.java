package com.example.pitchr.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pitchr.R;
import com.example.pitchr.activities.MainActivity;
import com.example.pitchr.fragments.DetailsFragment;
import com.example.pitchr.fragments.ProfileFragment;
import com.example.pitchr.helpers.TimeFormatter;
import com.example.pitchr.models.Comment;
import com.example.pitchr.models.Post;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {

    public static final String TAG = PostsAdapter.class.getSimpleName();
    private Context context;
    private List<Post> posts;

    public PostsAdapter(Context context, List<Post> posts) {
        this.context = context;
        this.posts = posts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

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
        int likes;

        public ViewHolder(View itemView) {
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
            likes = 0;

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
            View.OnClickListener detailsListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FragmentTransaction ft = ((MainActivity) context).getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.replace(R.id.flContainer, DetailsFragment.newInstance(currentPost), TAG);
                    ft.addToBackStack(TAG);
                    ft.commit();
                }
            };
            tvCaption.setOnClickListener(detailsListener);
            tvTime.setOnClickListener(detailsListener);
            ibtnComment.setOnClickListener(detailsListener);
            itemView.setOnClickListener(detailsListener);

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
                    ((MainActivity) context).mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + currentPost.getSong().getSpotifyId());
                }
            });
        }

        public void bind(Post post) {
            currentPost = post;
            tvUsername.setText(post.getUser().getUsername());
            tvCaption.setText(post.getCaption());
            tvSongName.setText(post.getSong().getName());
            tvArtists.setText(TextUtils.join(", ", post.getSong().getArtists()));

            // Set the time to the correct format
            tvTime.setText(TimeFormatter.getTimeDifference(post.getCreatedAt().toString()));

            // Set the images if we have them
            String image = post.getSong().getImageUrl();
            if (image != null) {
                Glide.with(context).load(image).into(ivSongImage);
            } else {
                ivSongImage.setImageDrawable(context.getDrawable(R.drawable.music_placeholder));
            }
            ParseFile pfpImage = post.getUser().getParseFile("pfp");
            if (pfpImage != null) {
                Glide.with(context).load(pfpImage.getUrl()).circleCrop().into(ivPfp);
            } else {
                Glide.with(context).load(R.drawable.default_pfp).circleCrop().into(ivPfp);
            }

            // Show if the user likes the post and the comment count
            queryLiked();
            setCommentCount();
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
                    for(int i = 0; i < users.size(); i++) {
                        if(users.get(i).getObjectId().equals(ParseUser.getCurrentUser().getObjectId())) {
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
                currentPost.addLike();
                likes++;
                addLike();
            } else {
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
    public void addAll(List<Post> list) {
        posts.addAll(list);
        notifyDataSetChanged();
    }
}
