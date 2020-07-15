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
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static androidx.core.content.FileProvider.*;

public class SettingsActivity extends AppCompatActivity {

    public static final String TAG = SettingsActivity.class.getSimpleName();
    ActivitySettingsBinding binding;

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
            }
        });
    }
}