package com.example.pitchr.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.pitchr.R;
import com.example.pitchr.databinding.ActivitySettingsBinding;
import com.example.pitchr.fragments.ProfileFragment;
import com.example.pitchr.models.FavSongs;
import com.example.pitchr.models.Song;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.spotify.sdk.android.auth.AuthorizationClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static androidx.core.content.FileProvider.*;

public class SettingsActivity extends AppCompatActivity {

    public static final String TAG = SettingsActivity.class.getSimpleName();
    ActivitySettingsBinding binding;
    public static final int FAV_SONG_LIMIT = 10;
    public SpotifyApi spotifyApi;

    // Camera variables
    public static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 42;
    public final static int PICK_PHOTO_CODE = 46;
    private static final int PERMISSION_REQUEST_CODE = 40;
    private File photoFile;
    private String photoFileName = "pfp.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set ViewBinding
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());

        // layout of activity is stored in a special property called root
        View view = binding.getRoot();
        setContentView(view);
        setSupportActionBar(binding.toolbar);
        view.setBackgroundColor(getResources().getColor(R.color.gray3));
        getSupportActionBar().setTitle("Settings");
        binding.toolbar.setTitleTextAppearance(this, R.style.PitchrTextAppearance);

        // Create a File reference for future access
        photoFile = getPhotoFileUri(photoFileName);
        binding.ivPFP.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Set views
        binding.tvUsername.setText(ParseUser.getCurrentUser().getUsername());

        // Set the images if we have them
        ParseFile pfpImage = ParseUser.getCurrentUser().getParseFile("pfp");
        if (pfpImage != null) {
            Glide.with(this).load(pfpImage.getUrl()).circleCrop().into(binding.ivPFP);
        } else {
            Glide.with(this).load(R.drawable.default_pfp).circleCrop().into(binding.ivPFP);
        }

        // Set logout button
        binding.btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParseUser.logOut();
                AuthorizationClient.clearCookies(SettingsActivity.this); // Clear Spotify cookies
                Intent i = new Intent(SettingsActivity.this, LoginActivity.class);
                ((ResultReceiver) getIntent().getParcelableExtra("finisher")).send(ProfileFragment.RESULT_CODE, new Bundle());
                startActivity(i);
                finish();
            }
        });

        // Set camera and gallery buttons
        binding.btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchCamera();
            }
        });
        binding.btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPickPhoto();
            }
        });

        // Set refreshing songs
        binding.btnRefreshFavSongs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Set the Spotify API
                spotifyApi = new SpotifyApi();
                spotifyApi.setAccessToken(ParseUser.getCurrentUser().getString("token"));
                queryFavSongs();
            }
        });

        // Set adding songs manually
        binding.btnAddFavSongs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to the activity
                Intent i = new Intent(SettingsActivity.this, FavSongsListActivity.class);
                startActivity(i);
            }
        });
    }

    // Launch the camera
    private void launchCamera() {
        if (!checkPermission()) {
            requestPermission();
        }

        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create a File reference for future access
        photoFile = getPhotoFileUri(photoFileName);

        // wrap File object into a content provider
        // required for API >= 24
        // See https://guides.codepath.com/android/Sharing-Content-with-Intents#sharing-files-with-api-24-or-higher
        Uri fileProvider = getUriForFile(SettingsActivity.this, "com.pitchr.fileprovider", photoFile);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, fileProvider);
        // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
        // So as long as the result is not null, it's safe to use the intent.
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    // Trigger gallery selection for a photo
    public void onPickPhoto() {
        // Create intent for picking a photo from the gallery
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_PHOTO_CODE);
    }

    // Check if we have permissions for photos
    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }

    // Request permissions if we don't have it
    private void requestPermission() {
        ActivityCompat.requestPermissions(SettingsActivity.this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(SettingsActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                } else {
                    Toast.makeText(SettingsActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
        }
    }

    // Show dialog options
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(SettingsActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    // Load a Bitmap from a URI
    public Bitmap loadFromUri(Uri photoUri) {
        Bitmap image = null;
        try {
            // check version of Android on device
            if (Build.VERSION.SDK_INT > 27) {
                // on newer versions of Android, use the new decodeBitmap method
                ImageDecoder.Source source = ImageDecoder.createSource(SettingsActivity.this.getContentResolver(), photoUri);
                image = ImageDecoder.decodeBitmap(source);
            } else {
                // support older versions of Android by using getBitmap
                image = MediaStore.Images.Media.getBitmap(SettingsActivity.this.getContentResolver(), photoUri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    // Check whether we returned from a camera activity or gallery activity and act accordingly
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // by this point we have the camera photo on disk
                Bitmap takenImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                // Load the taken image into the image view
                Glide.with(SettingsActivity.this).load(takenImage).circleCrop().into(binding.ivPFP);
                photoFile = getPhotoFileUri(photoFileName);
                savePFP(photoFile);
            } else { // Result was a failure
                Toast.makeText(SettingsActivity.this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        } else if ((data != null) && requestCode == PICK_PHOTO_CODE) {
            if (resultCode == RESULT_OK) {
                Uri photoUri = data.getData();

                // Load the image located at photoUri into selectedImage
                Bitmap selectedImage = loadFromUri(photoUri);

                // Save as a file
                FileOutputStream outStream = null;
                try {
                    outStream = new FileOutputStream(photoFile.getAbsolutePath());
                    selectedImage.compress(Bitmap.CompressFormat.PNG, 50, outStream);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Gallery image save error" + e);
                }
                // Load the selected image into a preview
                Glide.with(SettingsActivity.this).load(photoUri).circleCrop().into(binding.ivPFP);
                savePFP(photoFile);
            } else { // Result was a failure
                Toast.makeText(SettingsActivity.this, "Picture wasn't selected!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Returns the File for a photo stored on disk given the fileName
    public File getPhotoFileUri(String fileName) {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        File mediaStorageDir = new File(SettingsActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "failed to create directory");
        }

        // Return the file target for the photo based on filename
        return new File(mediaStorageDir.getPath() + File.separator + fileName);
    }

    // Save profile picture to database
    private void savePFP(File photoFile) {
        ParseUser.getCurrentUser().put("pfp", new ParseFile(photoFile));
        ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error while saving", e);
                    Toast.makeText(SettingsActivity.this, "Error while saving PFP!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i(TAG, "PFP save success!");
                Toast.makeText(SettingsActivity.this, "Successfully changed profile picture!", Toast.LENGTH_SHORT).show();
            }
        });
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

                // Now that we don't have any favorite songs, we should query them from Spotify
                Map<String, Object> options = new HashMap<>();
                options.put(SpotifyService.LIMIT, FAV_SONG_LIMIT);
                options.put(SpotifyService.TIME_RANGE, "long_term");

                spotifyApi.getService().getTopTracks(options, new Callback<Pager<Track>>() {
                    @Override
                    public void success(Pager<Track> trackPager, Response response) {
                        Log.d(TAG, "Get fav tracks success!");
                        for (Song song : Song.songsFromTracksList(trackPager.items)) {
                            // For each Track, turn it into a Song for the database
                            addToFavSong(song);
                        }
                        Toast.makeText(SettingsActivity.this, "Successfully added songs from Spotify!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, "Get fav tracks failed", error);
                    }
                });
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

    @Override
    public void onBackPressed() {
        ((ResultReceiver) getIntent().getParcelableExtra("updater")).send(ProfileFragment.RESULT_CODE, new Bundle());
        super.onBackPressed();
    }
}