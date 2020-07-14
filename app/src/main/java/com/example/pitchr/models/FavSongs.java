package com.example.pitchr.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

@ParseClassName("FavSongs")
public class FavSongs extends ParseObject {

    public static final String KEY_SONG = "song";
    public static final String KEY_USER = "user";
    public static final String KEY_CREATED_AT = "createdAt";
    private static final String TAG = FavSongs.class.getSimpleName();

    public Song getSong() {
        return (Song) getParseObject(KEY_SONG);
    }

    public void setSong(Song song) {
        put(KEY_SONG, song);
    }

    public ParseUser getUser() {
        return getParseUser(KEY_USER);
    }

    public void setUser(ParseUser parseUser) {
        put(KEY_USER, parseUser);
    }
}
