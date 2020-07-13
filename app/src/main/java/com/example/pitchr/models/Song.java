package com.example.pitchr.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;

import java.util.List;

@ParseClassName("Song")
public class Song extends ParseObject {

    public static final String KEY_NAME = "name";
    public static final String KEY_ARTISTS = "artists";
    public static final String KEY_SPOTIFY_ID = "spotifyId";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_CREATED_AT = "createdAt";
    private static final String TAG = Song.class.getSimpleName();

    public String getName() {
        return getString(KEY_NAME);
    }

    public void setName(String name) {
        put(KEY_NAME, name);
    }

    public String getSpotifyId() {
        return getString(KEY_SPOTIFY_ID);
    }

    public void setSpotifyId(String id) {
        put(KEY_SPOTIFY_ID, id);
    }

    public List<String> getArtists() {
        return getList(KEY_ARTISTS);
    }

    public void setArtists(List<String> artists) {
        getArtists().addAll(artists);
        saveInBackground();
    }

    public String getImageUrl() {
        return getString(KEY_IMAGE);
    }

    public void setImage(String image) {
        put(KEY_IMAGE, image);
    }


}
