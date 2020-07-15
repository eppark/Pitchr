package com.example.pitchr.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pitchr.R;
import com.example.pitchr.activities.MainActivity;
import com.example.pitchr.fragments.ProfileFragment;
import com.example.pitchr.models.Following;
import com.parse.CountCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    public static final String TAG = UsersAdapter.class.getSimpleName();
    Context context;
    List<ParseUser> users;

    // Pass in the context and list of users
    public UsersAdapter(Context context, List<ParseUser> users) {
        this.context = context;
        this.users = users;
    }

    // For each row, inflate the layout for the User
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    // Bind values based on the position of the element
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the data at position
        ParseUser user = users.get(position);
        // Bind the user with view holder
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    // Define a viewholder
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ParseUser user;
        ImageView ivPfp;
        TextView tvUserame;
        TextView tvFollowers;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPfp = itemView.findViewById(R.id.ivSongImage);
            tvUserame = itemView.findViewById(R.id.tvSongName);
            tvFollowers = itemView.findViewById(R.id.tvArtists);
            itemView.setOnClickListener(this);
        }

        public void bind(final ParseUser user) {
            this.user = user;
            tvUserame.setText(user.getUsername());

            // Set the images if we have them
            ParseFile pfpImage = user.getParseFile("pfp");
            if (pfpImage != null) {
                Glide.with(context).load(pfpImage.getUrl()).circleCrop().into(ivPfp);
            } else {
                Glide.with(context).load(R.drawable.default_pfp).circleCrop().into(ivPfp);
            }

            // Count the number of followers
            queryFollowersCount();
        }

        // Go to the user's profile when clicked
        @Override
        public void onClick(View view) {
            FragmentTransaction ft = ((MainActivity) context).getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.flContainer, ProfileFragment.newInstance(user), TAG);
            ft.addToBackStack(TAG);
            ft.commit();
        }

        // Count the number of followers this user has
        private void queryFollowersCount() {
            ParseQuery<Following> query = ParseQuery.getQuery(Following.class);
            query.include(Following.KEY_FOLLOWED_BY);
            query.include(Following.KEY_FOLLOWING);
            query.whereEqualTo(Following.KEY_FOLLOWING, user);
            query.countInBackground(new CountCallback() {
                @Override
                public void done(int count, ParseException e) {
                    if (count <= 0) {
                        tvFollowers.setText("0 followers");
                    } else {
                        tvFollowers.setText(String.format("%d followers", count));
                    }
                }
            });
        }
    }

    // Clean all elements of the recycler
    public void clear() {
        users.clear();
        notifyDataSetChanged();
    }

    // Add a list of items
    public void addAll(List<ParseUser> list) {
        users.addAll(list);
        notifyDataSetChanged();
    }
}