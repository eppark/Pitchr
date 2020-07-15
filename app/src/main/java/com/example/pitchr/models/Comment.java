package com.example.pitchr.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

@ParseClassName("Comment")
public class Comment extends ParseObject {
    public static final String KEY_CAPTION = "caption";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_OPOST = "opost";
    public static final String KEY_CREATED_AT = "createdAt";

    public String getCaption() {
        return getString(KEY_CAPTION);
    }

    public void setCaption(String description) {
        put(KEY_CAPTION, description);
    }

    public Post getOriginalPost() {
        return (Post) getParseObject(KEY_OPOST);
    }

    public void setOriginalPost(Post post) {
        put(KEY_OPOST, post);
    }

    public ParseUser getAuthor() {
        return getParseUser(KEY_AUTHOR);
    }

    public void setAuthor(ParseUser parseUser) {
        put(KEY_AUTHOR, parseUser);
    }
}
