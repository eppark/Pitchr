package com.example.pitchr.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;

import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {

    private Context context;
    private List<Comment> allComments;

    public CommentsAdapter(Context context, List<Comment> comments) {
        this.context = context;
        this.allComments = comments;
    }

    @NonNull
    @Override
    public CommentsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = allComments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return allComments.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        public final String TAG = ViewHolder.class.getSimpleName();
        Comment currentComment;
        ImageView ivPfp;
        TextView tvUsername;
        TextView tvTime;

        public ViewHolder(View itemView) {
            super(itemView);
            ivPfp = (ImageView) itemView.findViewById(R.id.ivPfp);
            tvUsername = (TextView) itemView.findViewById(R.id.tvUsername);
            tvTime = (TextView) itemView.findViewById(R.id.tvTime);
            itemView.setOnLongClickListener(this);
        }

        public void bind(final Comment comment) {
            currentComment = comment;

            // Set the username bold and description not bold
            SpannableString str = new SpannableString(comment.getAuthor().getUsername());
            str.setSpan(new StyleSpan(Typeface.BOLD), 0, str.length(), 0);
            tvUsername.setText(str);
            tvUsername.append("  " + comment.getCaption());

            // Set the time to the correct format
            if (comment.getCreatedAt() != null) {
                tvTime.setText(TimeFormatter.getTimeDifference(comment.getCreatedAt().toString()));
            } else {
                tvTime.setText("Just now");
            }

            // Put in the image
            ParseFile pfpImage = comment.getAuthor().getParseFile("pfp");
            if (pfpImage != null) {
                Glide.with(context).load(pfpImage.getUrl()).circleCrop().into(ivPfp);
            } else {
                Glide.with(context).load(R.drawable.default_pfp).circleCrop().into(ivPfp);
            }

            // When the user clicks on text or a profile picture, take them to the profile page for that user
            View.OnClickListener profileListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FragmentTransaction ft = ((MainActivity) context).getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.replace(R.id.flContainer, ProfileFragment.newInstance(comment.getAuthor()), TAG);
                    ft.addToBackStack(TAG);
                    ft.commit();
                }
            };
            ivPfp.setOnClickListener(profileListener);
        }

        @Override
        public boolean onLongClick(View view) {
            // Get the comment we want to delete
            ParseQuery<Comment> query = ParseQuery.getQuery(Comment.class);
            query.whereEqualTo(Comment.KEY_OBJECT_ID, currentComment.getObjectId());
            query.findInBackground(new FindCallback<Comment>() {
                @Override
                public void done(List<Comment> comments, ParseException e) {
                    if (e != null) {
                        Log.e(TAG, "Issue with getting comment to delete", e);
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // If we don't get an error, delete from Parse database
                    Log.d(TAG, "Query comment to delete success!");
                    int position = getAdapterPosition();
                    for (Comment comment : comments) {
                        comment.deleteInBackground();
                        comment.saveInBackground();

                        // Notify the adapter
                        allComments.remove(position);
                        notifyItemRemoved(position);
                        ((DetailsFragment) ((MainActivity) context).getSupportFragmentManager().findFragmentByTag(PostsAdapter.TAG)).setCommentCount();
                    }
                }
            });
            return true;
        }
    }

    // Clean all elements of the recycler
    public void clear() {
        allComments.clear();
        notifyDataSetChanged();
    }

    // Add a list of items
    public void addAll(List<Comment> list) {
        allComments.addAll(list);
        notifyDataSetChanged();
    }
}
