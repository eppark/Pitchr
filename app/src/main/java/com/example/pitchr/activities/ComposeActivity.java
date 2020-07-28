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
import com.example.pitchr.ParseApplication;
import com.example.pitchr.R;
import com.example.pitchr.databinding.ActivityComposeBinding;
import com.example.pitchr.models.Post;
import com.example.pitchr.models.Song;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.models.AudioFeaturesTrack;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ComposeActivity extends AppCompatActivity {

    private static final String TAG = ComposeActivity.class.getSimpleName();
    Song song;
    ActivityComposeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // LOG TO ANALYTICS
        ParseApplication.logEvent("composeActivity", Arrays.asList("status"), Arrays.asList("success"));

        // Set ViewBinding
        binding = ActivityComposeBinding.inflate(getLayoutInflater());

        // layout of activity is stored in a special property called root
        View view = binding.getRoot();
        setContentView(view);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("Compose");
        binding.toolbar.setTitleTextAppearance(this, R.style.PitchrTextAppearance);
        binding.pbLoading.setVisibility(View.GONE);

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

    // Show the slide animation
    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    // Show the post option item
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_post:
                String caption = binding.etCaption.getText().toString();
                // Make sure we have a caption
                if (caption.isEmpty()) {
                    Toast.makeText(this, "Caption can't be empty!", Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    // Show loading bar
                    binding.pbLoading.setVisibility(View.VISIBLE);

                    // Make a new Post
                    Post post = new Post();
                    post.put(Post.KEY_AUTHOR, ParseUser.getCurrentUser());
                    post.put(Post.KEY_CAPTION, caption);

                    // First check if the Song is already in the database
                    ParseQuery<Song> songQuery = new ParseQuery<>(Song.class);
                    songQuery.include(Song.KEY_SPOTIFY_ID);
                    songQuery.whereEqualTo(Song.KEY_SPOTIFY_ID, song.getSpotifyId());
                    songQuery.findInBackground(new FindCallback<Song>() {
                        @Override
                        public void done(List<Song> objects, ParseException e) {
                            if (objects.size() > 0) {
                                // If it is, we can just save this song
                                post.put(Post.KEY_SONG, objects.get(0));
                                savePost(post);
                            } else {
                                // If it isn't, we need to save this Song object
                                SpotifyApi spotifyApi = new SpotifyApi();
                                spotifyApi.setAccessToken(ParseUser.getCurrentUser().getString("token"));
                                spotifyApi.getService().getTrackAudioFeatures(song.getSpotifyId(), new Callback<AudioFeaturesTrack>() {
                                    @Override
                                    public void success(AudioFeaturesTrack audioFeaturesTrack, Response response) {
                                        // Get the audio features into a list
                                        List<Float> audioFeatures = new ArrayList<>();
                                        audioFeatures.add(audioFeaturesTrack.acousticness);
                                        audioFeatures.add(audioFeaturesTrack.danceability);
                                        audioFeatures.add(audioFeaturesTrack.energy);
                                        audioFeatures.add(audioFeaturesTrack.instrumentalness);
                                        audioFeatures.add(audioFeaturesTrack.liveness);
                                        audioFeatures.add(audioFeaturesTrack.loudness);
                                        audioFeatures.add(audioFeaturesTrack.speechiness);
                                        audioFeatures.add(audioFeaturesTrack.valence);
                                        audioFeatures.add(audioFeaturesTrack.tempo);

                                        // Save
                                        song.setAudioFeatures(audioFeatures);
                                        song.saveInBackground(new SaveCallback() {
                                            @Override
                                            public void done(ParseException e) {
                                                post.put(Post.KEY_SONG, song);
                                                savePost(post);
                                            }
                                        });
                                    }

                                    @Override
                                    public void failure(RetrofitError error) {
                                        Log.e(TAG, "Failed to get audio features", error);
                                    }
                                });
                            }
                        }
                    });

                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Save the post to the database
    private void savePost(Post post) {
        post.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error while posting!", e);
                    Toast.makeText(getApplicationContext(), "Error while saving!", Toast.LENGTH_SHORT).show();

                    // LOG TO ANALYTICS
                    ParseApplication.logEvent("postEvent", Arrays.asList("status"), Arrays.asList("failure"));
                    return;
                }
                Log.d(TAG, "Posting success!");
                Toast.makeText(getApplicationContext(), "Shared post successfully", Toast.LENGTH_SHORT).show();

                // LOG TO ANALYTICS
                ParseApplication.logEvent("postEvent", Arrays.asList("status"), Arrays.asList("success"));

                // Hide loading bar
                binding.pbLoading.setVisibility(View.GONE);

                // Finish the parent activity as well
                setResult(SearchSongsActivity.RESULT_CODE);
                finish();
            }
        });
    }
}