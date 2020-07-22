package com.example.pitchr.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.activities.MainActivity;
import com.example.pitchr.fragments.ProfileFragment;
import com.example.pitchr.models.FavSongs;
import com.example.pitchr.models.MatchItem;
import com.example.pitchr.models.Song;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.ViewHolder> {

    public static final String TAG = MatchesAdapter.class.getSimpleName();
    Context context;
    List<MatchItem> users;

    // Pass in the context and list of users
    public MatchesAdapter(Context context, List<MatchItem> users) {
        this.context = context;
        this.users = users;
    }

    // For each row, inflate the layout for the User
    @NonNull
    @Override
    public MatchesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_match, parent, false);
        return new MatchesAdapter.ViewHolder(view);
    }

    // Bind values based on the position of the element
    @Override
    public void onBindViewHolder(@NonNull MatchesAdapter.ViewHolder holder, int position) {
        // Get the data at position
        MatchItem user = users.get(position);
        // Bind the user with view holder
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    // Define a viewholder
    public class ViewHolder extends RecyclerView.ViewHolder {

        ParseUser user;
        ImageView ivPfp;
        TextView tvUserame;
        TextView tvFavSongs;
        Button btnSeeProfile;
        RecyclerView rvFavSongs;
        SongsAdapter adapter;
        TextView tvPercent;
        ArrayList<Song> allSongs;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPfp = itemView.findViewById(R.id.ivPfp);
            tvUserame = itemView.findViewById(R.id.tvUsername);
            tvFavSongs = itemView.findViewById(R.id.tvFavSongs);
            btnSeeProfile = itemView.findViewById(R.id.btnSeeProfile);
            rvFavSongs = itemView.findViewById(R.id.rvFavSongs);
            tvPercent = itemView.findViewById(R.id.tvPercent);
        }

        public void bind(final MatchItem match) {
            this.user = match.user;
            tvUserame.setText(user.getUsername());
            tvFavSongs.setText(String.format("%s's favorite songs", user.getUsername()));
            tvPercent.setText(String.format("%d%% match", Math.round(match.percent * 100)));

            // Set the images if we have them
            ParseFile pfpImage = user.getParseFile("pfp");
            if (pfpImage != null) {
                Glide.with(context).load(pfpImage.getUrl()).circleCrop().into(ivPfp);
            } else {
                Glide.with(context).load(R.drawable.default_pfp).circleCrop().into(ivPfp);
            }

            btnSeeProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FragmentTransaction ft = ((MainActivity) context).getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.replace(R.id.flContainer, ProfileFragment.newInstance(user), TAG);
                    ft.addToBackStack(TAG);
                    ft.commit();
                }
            });

            // Set up favorite songs view
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
            allSongs = new ArrayList<>();
            rvFavSongs.setLayoutManager(layoutManager);
            adapter = new SongsAdapter(context, allSongs);
            adapter.setOnItemClickListener(new SongsAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View itemView, int position) {
                    if (((ParseApplication) (context.getApplicationContext())).spotifyExists) {
                        // Make sure the position is valid
                        if (position != RecyclerView.NO_POSITION) {
                            // Check if we're playing, pausing, or resuming
                            if (adapter.currentPosition == position) {
                                ((ParseApplication) context.getApplicationContext()).mSpotifyAppRemote.getPlayerApi().pause();
                                adapter.currentPosition = -1;
                            } else {
                                ((ParseApplication) context.getApplicationContext()).mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + allSongs.get(position).getSpotifyId());
                                adapter.currentPosition = position;
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            });
            rvFavSongs.setAdapter(adapter);
            querySongs();
        }

        // Query a list of top songs
        private void querySongs() {
            ParseQuery<FavSongs> query = ParseQuery.getQuery(FavSongs.class);
            query.include(FavSongs.KEY_USER);
            query.include(FavSongs.KEY_SONG);
            query.whereEqualTo(FavSongs.KEY_USER, user);
            query.setLimit(20); // Only show 20 songs at a time
            query.addDescendingOrder(FavSongs.KEY_CREATED_AT);
            query.findInBackground(new FindCallback<FavSongs>() {
                @Override
                public void done(List<FavSongs> songsList, ParseException e) {
                    if (e != null) {
                        Log.e(TAG, "Issue with getting fav songs", e);
                        return;
                    }
                    Log.d(TAG, "Query fav songs success!");
                    for (FavSongs songItem : songsList) {
                        allSongs.add(songItem.getSong());
                    }
                    adapter.notifyDataSetChanged();
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
    public void addAll(List<MatchItem> list) {
        users.addAll(list);
        notifyDataSetChanged();
    }
}
