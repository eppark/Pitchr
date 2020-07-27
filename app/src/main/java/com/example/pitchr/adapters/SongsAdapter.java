package com.example.pitchr.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pitchr.R;
import com.example.pitchr.models.Song;

import java.util.ArrayList;
import java.util.List;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.ViewHolder> {

    private List<Song> mSongs;
    private Context mContext;
    public int currentPosition = -1;
    public static int TYPE_SONG = 1;
    public static int TYPE_REC = 2;
    private int type;

    // Define listener member variable
    private OnItemClickListener listener;

    // Define the listener interface
    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    // Define the method that allows the parent activity or fragment to define the listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // View lookup cache
    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivSongImage;
        public TextView tvSongName;
        public TextView tvArtists;
        public RelativeLayout rlSong;

        public ViewHolder(final View itemView, final OnItemClickListener clickListener) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            ivSongImage = (ImageView) itemView.findViewById(R.id.ivSongImage);
            tvSongName = (TextView) itemView.findViewById(R.id.tvSongName);
            tvArtists = (TextView) itemView.findViewById(R.id.tvArtists);
            rlSong = (RelativeLayout) itemView.findViewById(R.id.rlSong);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onItemClick(itemView, getAdapterPosition());
                }
            });
        }
    }

    public SongsAdapter(Context context, ArrayList<Song> aSongs, int type) {
        mSongs = aSongs;
        mContext = context;
        this.type = type;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public SongsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View songView;
        if (type == TYPE_SONG) {
            // Inflate the custom layout
            songView = inflater.inflate(R.layout.item_song, parent, false);
        } else {
            songView = inflater.inflate(R.layout.item_recsong, parent, false);
        }

        // Return a new holder instance
        SongsAdapter.ViewHolder viewHolder = new SongsAdapter.ViewHolder(songView, listener);
        return viewHolder;
    }


    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(SongsAdapter.ViewHolder viewHolder, int position) {
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

        // Change the background if we need to
        if (currentPosition != -1 && currentPosition == position) {
            viewHolder.rlSong.setBackgroundColor(getContext().getResources().getColor(R.color.spotifyGreen));
        } else {
            viewHolder.rlSong.setBackgroundResource(0);
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
}
