package com.example.pitchr;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.pitchr.chat.DM;
import com.example.pitchr.chat.Message;
import com.example.pitchr.models.Comment;
import com.example.pitchr.models.FavSongs;
import com.example.pitchr.models.Following;
import com.example.pitchr.models.Match;
import com.example.pitchr.models.Match2;
import com.example.pitchr.models.Post;
import com.example.pitchr.models.Song;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.parse.Parse;
import com.parse.ParseObject;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseApplication extends Application {

    public static final String TAG = ParseApplication.class.getSimpleName();
    public SpotifyAppRemote mSpotifyAppRemote;
    public boolean spotifyExists;
    public int version = 2;
    public static FirebaseAnalytics mFirebaseAnalytics;

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
        ParseObject.registerSubclass(Match2.class);

        // set applicationId, and server server based on the values in the back4app settings.
        // clientKey is not needed unless explicitly configured
        // any network interceptors must be added with the Configuration Builder given this syntax
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.parse_app_id))
                // if defined
                .clientKey(getString(R.string.parse_client_key))
                .server(getString(R.string.parse_server_url))
                .build()
        );

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Push notifs
        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);
    }

    // Log login event
    public static void logLoginEvent(String method) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, method);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);
    }

    // Log signup event
    public static void logSignupEvent(String method) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, method);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle);
    }

    // Log share event
    public static void logShareEvent(String method, String id) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, method);
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle);
    }

    // Log search event
    public static void logSearchEvent(String searchQuery) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, searchQuery);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle);
    }

    // Log activity event
    public static void logActivityEvent(String activity) {
        Bundle bundle = new Bundle();
        bundle.putString("event", activity);
        mFirebaseAnalytics.logEvent("activity_event", bundle);
    }

    // Log generic event
    public static void logEvent(String eventName, List<String> dimensionName, List<String> dimensionValue) {
        Bundle bundle = new Bundle();
        for (int i = 0; i < dimensionName.size(); i++) {
            bundle.putString(dimensionName.get(i), dimensionValue.get(i));
        }
        mFirebaseAnalytics.logEvent(eventName, bundle);
    }

    // Send a notification
    public static void sendNotification(JSONObject notification, Context context) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(context.getString(R.string.fcm_api_url), notification,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: " + response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "onErrorResponse: ", error);
                    }
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", String.format("key=%s", context.getString(R.string.fcm_server_key)));
                params.put("Content-Type", "application/json");
                return params;
            }
        };
        PitchrSingleton.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }
}
