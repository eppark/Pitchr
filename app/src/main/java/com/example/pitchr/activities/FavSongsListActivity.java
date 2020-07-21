package com.example.pitchr.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.adapters.FavSongsListAdapter;
import com.example.pitchr.databinding.ActivityFavSongsListBinding;
import com.example.pitchr.models.FavSongs;
import com.example.pitchr.models.Song;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FavSongsListActivity extends AppCompatActivity {

    public static final String TAG = FavSongsListActivity.class.getSimpleName();
    public static final int REQUEST_CODE = 12345;
    FavSongsListAdapter adapter;
    ArrayList<Song> allSongs;
    public ActivityFavSongsListBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // LOG TO ANALYTICS
        ParseApplication.logEvent("favSongsListActivity", Arrays.asList("status"), Arrays.asList("success"));

        // Set ViewBinding
        binding = ActivityFavSongsListBinding.inflate(getLayoutInflater());

        // layout of activity is stored in a special property called root
        View view = binding.getRoot();
        setContentView(view);
        setSupportActionBar(binding.toolbar);
        view.setBackgroundColor(getResources().getColor(R.color.gray3));
        binding.toolbar.setTitleTextAppearance(this, R.style.PitchrTextAppearance);
        getSupportActionBar().setTitle("Favorite Songs");

        binding.tvNoSongs.setVisibility(View.GONE);

        allSongs = new ArrayList<>();
        // Recycler view setup: layout manager and the adapter
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.rvItems.setLayoutManager(layoutManager);
        adapter = new FavSongsListAdapter(this, allSongs);
        binding.rvItems.setAdapter(adapter);

        // Allow the user to swipe to delete
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(adapter));
        itemTouchHelper.attachToRecyclerView(binding.rvItems);

        // Populate the feed
        queryInitial();
    }

    private void queryInitial() {
        adapter.clear();
        querySongs(0);
    }

    // Query a list of top songs
    private void querySongs(int page) {
        ParseQuery<FavSongs> query = ParseQuery.getQuery(FavSongs.class);
        query.include(FavSongs.KEY_USER);
        query.include(FavSongs.KEY_SONG);
        query.whereEqualTo(FavSongs.KEY_USER, ParseUser.getCurrentUser());
        query.setSkip(20 * page);
        query.setLimit(20); // Only show 20 songs at a time
        query.addDescendingOrder(FavSongs.KEY_CREATED_AT);
        query.findInBackground(new FindCallback<FavSongs>() {
            @Override
            public void done(List<FavSongs> songsList, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting fav songs", e);
                    Toast.makeText(FavSongsListActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Query fav songs success!");
                for (FavSongs songItem : songsList) {
                    allSongs.add(songItem.getSong());
                }
                if (allSongs.size() == 0) {
                    // If there are no fav songs for the user, show the message
                    binding.tvNoSongs.setVisibility(View.VISIBLE);
                } else {
                    binding.tvNoSongs.setVisibility(View.GONE);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_fav_songs, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.miAdd:
                Intent i = new Intent(FavSongsListActivity.this, SearchActivity.class);
                i.putExtra("add", true);
                startActivityForResult(i, REQUEST_CODE);
                return true;
            case R.id.miSave:
                queryFavSongs();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            // Extract the data and add the Song to the view
            Song song = (Song) Parcels.unwrap(data.getExtras().getParcelable(Song.class.getSimpleName()));
            allSongs.add(song);
            adapter.notifyDataSetChanged();
            binding.rvItems.smoothScrollToPosition(allSongs.size() - 1);
            Toast.makeText(this, "Song added!", Toast.LENGTH_SHORT).show();
            binding.tvNoSongs.setVisibility(View.GONE);
        }
    }

    // Create a list of favorite songs from Spotify for the user
    private void queryFavSongs() {
        ParseQuery<FavSongs> query = ParseQuery.getQuery(FavSongs.class);
        query.include(FavSongs.KEY_USER);
        query.include(FavSongs.KEY_SONG);
        query.whereEqualTo(FavSongs.KEY_USER, ParseUser.getCurrentUser());
        query.findInBackground(new FindCallback<FavSongs>() {
            @Override
            public void done(List<FavSongs> objects, ParseException e) {
                // Delete all the favorite songs if we currently have them
                if (objects.size() > 0) {
                    for (FavSongs favSong : objects) {
                        favSong.deleteInBackground(new DeleteCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e != null) {
                                    Log.e(TAG, "Error deleting fav song object from database!", e);
                                }
                                Log.d(TAG, "Successfully deleted fav song object from database!");
                            }
                        });
                    }
                }

                // Now that we don't have any favorite songs, we should add the ones in our list to the database
                for (Song song : allSongs) {
                    addToFavSong(song);
                }
                Toast.makeText(getApplicationContext(), "Successfully updated favorite songs!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    // Create new FavSong objects for the database
    private void addToFavSong(Song song) {
        // Query in the database if the Song already exists
        ParseQuery<Song> songQuery = new ParseQuery<>(Song.class);
        songQuery.include(Song.KEY_SPOTIFY_ID);
        songQuery.whereEqualTo(Song.KEY_SPOTIFY_ID, song.getSpotifyId());
        songQuery.findInBackground(new FindCallback<Song>() {
            @Override
            public void done(List<Song> objects, ParseException e) {
                // Create a new FavSongs row
                FavSongs favSongsObject = new FavSongs();
                favSongsObject.put(FavSongs.KEY_USER, ParseUser.getCurrentUser());

                if (objects.size() > 0) {
                    // If the song is already in the database, we don't want to create a new one
                    favSongsObject.put(FavSongs.KEY_SONG, objects.get(0));
                    favSongsObject.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                Log.e(TAG, "Failed to add fav song to database!", e);
                                return;
                            }
                            Log.d(TAG, "Successfully added fav song object to database!");
                        }
                    });

                } else {
                    // If the Song isn't already in the database, we need to save it
                    song.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            favSongsObject.put(FavSongs.KEY_SONG, song);
                            favSongsObject.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e != null) {
                                        Log.e(TAG, "Failed to add fav song to database!", e);
                                        return;
                                    }
                                    Log.d(TAG, "Successfully added fav song object to database!");
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    // Implement the user swiping to delete
    public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
        private FavSongsListAdapter mAdapter;
        private Drawable icon;
        private final ColorDrawable background;

        public SwipeToDeleteCallback(FavSongsListAdapter adapter) {
            super(0,ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            mAdapter = adapter;
            icon = ContextCompat.getDrawable(mAdapter.mContext, R.drawable.ic_trash);
            background = new ColorDrawable(getResources().getColor(R.color.red));
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            mAdapter.deleteItem(position);
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            View itemView = viewHolder.itemView;
            int backgroundCornerOffset = 20; //so background is behind the rounded corners of itemView

            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + icon.getIntrinsicHeight();

            if (dX > 0) { // Swiping to the right
                int iconLeft = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
                int iconRight = itemView.getLeft() + iconMargin;
                icon.setBounds(iconRight, iconTop, iconLeft, iconBottom);
                background.setBounds(itemView.getLeft(), itemView.getTop(),
                        itemView.getLeft() + ((int) dX) + backgroundCornerOffset, itemView.getBottom());
            } else if (dX < 0) { // Swiping to the left
                int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                int iconRight = itemView.getRight() - iconMargin;
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                background.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                        itemView.getTop(), itemView.getRight(), itemView.getBottom());
            } else { // view is unSwiped
                background.setBounds(0, 0, 0, 0);
                icon.setBounds(0, 0, 0, 0);
            }

            background.draw(c);
            icon.draw(c);
        }
    }
}