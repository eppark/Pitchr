package com.example.pitchr.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseRelation;
import com.parse.ParseUser;

@ParseClassName("Post")
public class Post extends ParseObject {

    public static final String KEY_CAPTION = "caption";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_SONG = "song";
    public static final String KEY_LIKES = "likes";
    public static final String KEY_CREATED_AT = "createdAt";
    private static final String TAG = Post.class.getSimpleName();

    public String getCaption() {
        return getString(KEY_CAPTION);
    }

    public void setCaption(String caption) {
        put(KEY_CAPTION, caption);
    }

    public ParseUser getUser() {
        return getParseUser(KEY_AUTHOR);
    }

    public void setUser(ParseUser parseUser) {
        put(KEY_AUTHOR, parseUser);
    }

    public Song getSong() {
        return (Song) getParseObject(KEY_SONG);
    }

    public void setSong(Song song) {
        put(KEY_SONG, song);
    }

    public ParseRelation<ParseUser> getLikes() {
        return getRelation(KEY_LIKES);
    }

    public void addLike() {
        getLikes().add(ParseUser.getCurrentUser());
        saveInBackground();
    }

    public void removeLike() {
        getLikes().remove(ParseUser.getCurrentUser());
        saveInBackground();
    }
}
