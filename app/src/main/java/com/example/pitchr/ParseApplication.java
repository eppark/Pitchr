package com.example.pitchr;

import android.app.Application;

import com.example.pitchr.R;
import com.example.pitchr.chat.DM;
import com.example.pitchr.chat.Message;
import com.example.pitchr.models.Comment;
import com.example.pitchr.models.FavSongs;
import com.example.pitchr.models.Following;
import com.example.pitchr.models.Post;
import com.example.pitchr.models.Song;
import com.parse.Parse;
import com.parse.ParseObject;

public class ParseApplication extends Application {

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
}
