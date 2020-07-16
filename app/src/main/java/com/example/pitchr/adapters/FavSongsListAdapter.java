package com.example.pitchr.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pitchr.R;
import com.example.pitchr.activities.FavSongsListActivity;
import com.example.pitchr.models.FavSongs;
import com.example.pitchr.models.Song;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class FavSongsListAdapter extends RecyclerView.Adapter<FavSongsListAdapter.ViewHolder> {

    private List<Song> mSongs;
    public Context mContext;
    Song deletedSong;
    int deletedPosition;

    // View lookup cache
    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivSongImage;
        public TextView tvSongName;
        public TextView tvArtists;

        public ViewHolder(final View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            ivSongImage = (ImageView)itemView.findViewById(R.id.ivSongImage);
            tvSongName = (TextView)itemView.findViewById(R.id.tvSongName);
            tvArtists = (TextView)itemView.findViewById(R.id.tvArtists);
        }
    }

    public FavSongsListAdapter(Context context, ArrayList<Song> aSongs) {
        mSongs = aSongs;
        mContext = context;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public FavSongsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View songView = inflater.inflate(R.layout.item_song, parent, false);

        // Return a new holder instance
        FavSongsListAdapter.ViewHolder viewHolder = new FavSongsListAdapter.ViewHolder(songView);
        return viewHolder;
    }


    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(FavSongsListAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        Song song = mSongs.get(position);

        // Populate data into the template view using the data object
        viewHolder.tvSongName.setText(song.getName());
        viewHolder.tvArtists.setText(TextUtils.join(", ", song.getArtists()));

        // Set the images if we have them
        String image = song.getImageUrl();
        if (image != null) {
            Glide.with(getContext()).load(image).into(viewHolder.ivSongImage);
        } else {
            viewHolder.ivSongImage.setImageDrawable(getContext().getResources().getDrawable(R.drawable.music_placeholder));
        }
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mSongs.size();
    }

    // Easy access to the context object in the recyclerview
    private Context getContext() {
        return mContext;
    }

    // Clean all elements of the recycler
    public void clear() {
        mSongs.clear();
        notifyDataSetChanged();
    }

    // Add a list of items
    public void addAll(List<Song> list) {
        mSongs.addAll(list);
        notifyDataSetChanged();
    }

    // Delete an item
    public void deleteItem(int position) {
        deletedSong = mSongs.get(position);
        deletedPosition = position;
        mSongs.remove(position);
        notifyItemRemoved(position);
        showUndoSnackbar();
    }

    // Give the user the option to put back the Song we removed
    private void showUndoSnackbar() {
        Snackbar snackbar = Snackbar.make(((FavSongsListActivity) mContext).binding.rlLayout, R.string.snack_bar_text, Snackbar.LENGTH_LONG);
        snackbar.getView().setBackgroundColor(ContextCompat.getColor(mContext, R.color.spotifyGreen));
        snackbar.setActionTextColor(ContextCompat.getColor(mContext, R.color.gray3));
        snackbar.setAction(R.string.snack_bar_undo, v -> undoDelete());
        snackbar.show();
    }

    // Put back the Song we removed
    private void undoDelete() {
        mSongs.add(deletedPosition, deletedSong);
        notifyItemInserted(deletedPosition);
        ((FavSongsListActivity) mContext).binding.rvItems.smoothScrollToPosition(deletedPosition);
    }
}
