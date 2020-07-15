package com.example.pitchr.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.example.pitchr.R;
import com.example.pitchr.adapters.CommentsAdapter;
import com.example.pitchr.adapters.PostsAdapter;
import com.example.pitchr.databinding.ActivityMainBinding;
import com.example.pitchr.fragments.CommentDialogFragment;
import com.example.pitchr.fragments.DetailsFragment;
import com.example.pitchr.fragments.PostsFragment;
import com.example.pitchr.fragments.ProfileFragment;
import com.example.pitchr.models.Comment;
import com.example.pitchr.models.FavSongs;
import com.example.pitchr.models.Song;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.boltsinternal.Task;

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

public class MainActivity extends AppCompatActivity implements CommentDialogFragment.CommentDialogFragmentListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    public final FragmentManager fragmentManager = getSupportFragmentManager();
    public ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        // layout of activity is stored in a special property called root
        View view = binding.getRoot();
        setContentView(view);
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextAppearance(this, R.style.PitchrTextAppearance);
        getSupportActionBar().setTitle(" ");

        // Set the bottom navigation view
        binding.bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Fragment fragment;
                switch (menuItem.getItemId()) {
                    case R.id.action_home:
                        fragment = new PostsFragment();
                        break;
                    case R.id.action_match:
                        //fragment = new ComposeFragment();
                        fragment = new PostsFragment();
                        break;
                    case R.id.action_profile:
                    default:
                        fragment = ProfileFragment.newInstance(ParseUser.getCurrentUser());
                        break;
                }
                fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).commit();
                return true;
            }
        });

        // Set default selection
        binding.bottomNavigationView.setSelectedItemId(R.id.action_home);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onFinishCommentDialog(Comment comment) {
        ((DetailsFragment) fragmentManager.findFragmentByTag(PostsAdapter.TAG)).allComments.add(0, comment);
        ((DetailsFragment) fragmentManager.findFragmentByTag(PostsAdapter.TAG)).adapter.notifyDataSetChanged();
    }
}