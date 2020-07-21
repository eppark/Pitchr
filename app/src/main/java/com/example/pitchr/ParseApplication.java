package com.example.pitchr;

import android.app.Application;

import com.example.pitchr.R;
import com.example.pitchr.chat.DM;
import com.example.pitchr.chat.Message;
import com.example.pitchr.models.Comment;
import com.example.pitchr.models.FavSongs;
import com.example.pitchr.models.Following;
import com.example.pitchr.models.Match;
import com.example.pitchr.models.Post;
import com.example.pitchr.models.Song;
import com.parse.Parse;
import com.parse.ParseAnalytics;
import com.parse.ParseObject;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseApplication extends Application {

    public SpotifyAppRemote mSpotifyAppRemote;

    @Override
    public void onCreate() {
        super.onCreate();

        // Register the parse models
        ParseObject.registerSubclass(Post.class);
        ParseObject.registerSubclass(Song.class);
        ParseObject.registerSubclass(FavSongs.class);
        ParseObject.registerSubclass(Following.class);
        ParseObject.registerSubclass(Comment.class);
        ParseObject.registerSubclass(DM.class);
        ParseObject.registerSubclass(Message.class);
        ParseObject.registerSubclass(Match.class);

        // set applicationId, and server server based on the values in the back4app settings.
        // clientKey is not needed unless explicitly configured
        // any network interceptors must be added with the Configuration Builder given this syntax
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                // if defined
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build()
        );
    }

    // Log to Parse Analytics
    public static void logEvent(String eventName, List<String> dimensionName, List<String> dimensionValue) {
        Map<String, String> dimensions = new HashMap<String, String>();
        // Define ranges to bucket data points into meaningful segments
        for (int i = 0; i < dimensionName.size(); i++) {
            dimensions.put(dimensionName.get(i), dimensionValue.get(i));
        }
        // Send the dimensions to Parse along with the event
        ParseAnalytics.trackEventInBackground(eventName, dimensions);
    }
}
