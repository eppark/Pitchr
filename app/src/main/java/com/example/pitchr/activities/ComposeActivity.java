package com.example.pitchr.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.pitchr.R;
import com.example.pitchr.databinding.ActivityComposeBinding;
import com.example.pitchr.helpers.TimeFormatter;
import com.example.pitchr.models.Post;
import com.example.pitchr.models.Song;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.parceler.Parcels;

public class ComposeActivity extends AppCompatActivity {

    private static final String TAG = ComposeActivity.class.getSimpleName();
    Song song;
    ActivityComposeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set ViewBinding
        binding = ActivityComposeBinding.inflate(getLayoutInflater());

        // layout of activity is stored in a special property called root
        View view = binding.getRoot();
        setContentView(view);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Compose");
        binding.toolbar.setTitleTextAppearance(this, R.style.PitchrTextAppearance);

        // Get the song
        song = Parcels.unwrap(getIntent().getParcelableExtra(Song.class.getSimpleName()));

        // Set views
        binding.tvSongName.setText(song.getName());
        binding.tvArtists.setText(TextUtils.join(", ", song.getArtists()));

        // Set the images if we have them
        String image = song.getImageUrl();
        if (image != null) {
            Glide.with(this).load(image).into(binding.ivSongImage);
        } else {
            binding.ivSongImage.setImageDrawable(getDrawable(R.drawable.music_placeholder));
        }
        ParseFile pfpImage = ParseUser.getCurrentUser().getParseFile("pfp");
        if (pfpImage != null) {
            Glide.with(this).load(pfpImage.getUrl()).circleCrop().into(binding.ivPFP);
        } else {
            Glide.with(this).load(R.drawable.default_pfp).circleCrop().into(binding.ivPFP);
        }
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_compose, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_post:
                String caption = binding.etCaption.getText().toString();
                // Make sure we have a caption
                if (caption == null || caption.isEmpty()) {
                    Toast.makeText(this, "Caption can't be empty!", Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    Post post = new Post();
                    post.put(Post.KEY_AUTHOR, ParseUser.getCurrentUser());
                    post.put(Post.KEY_SONG, song);
                    post.put(Post.KEY_CAPTION, caption);
                    post.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                Log.e(TAG, "Error while posting!", e);
                                Toast.makeText(getApplicationContext(), "Error while saving!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Log.d(TAG, "Posting success!");
                            Toast.makeText(getApplicationContext(), "Shared post successfully", Toast.LENGTH_SHORT).show();

                            // Finish the parent activity as well
                            setResult(SearchActivity.RESULT_CODE);
                            finish();
                        }
                    });
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}