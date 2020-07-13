package com.example.pitchr;

import android.app.Application;

import com.example.pitchr.R;
import com.parse.Parse;
import com.parse.ParseObject;

public class ParseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Register the parse models
        //ParseObject.registerSubclass(Post.class);
        //ParseObject.registerSubclass(Song.class);

        // set applicationId, and server server based on the values in the Heroku settings.
        // clientKey is not needed unless explicitly configured
        // any network interceptors must be added with the Configuration Builder given this syntax
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.parse_application_id)) // should correspond to APP_ID env variable
                .clientKey(getString(R.string.parse_master_key))  // set explicitly unless clientKey is explicitly configured on Parse server
                .server("https://" + getString(R.string.parse_application_id) + ".herokuapp.com/parse/").build());
    }
}
