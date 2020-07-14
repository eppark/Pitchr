package com.example.pitchr.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

@ParseClassName("Following")
public class Following extends ParseObject {

    public static final String KEY_FOLLOWING = "following";
    public static final String KEY_FOLLOWED_BY = "followedBy";
    public static final String KEY_CREATED_AT = "createdAt";
    private static final String TAG = Following.class.getSimpleName();

    public ParseUser getFollowing() {
        return getParseUser(KEY_FOLLOWING);
    }

    public void setFollowing(ParseUser user) {
        put(KEY_FOLLOWING, user);
    }

    public ParseUser getFollowedBy() {
        return getParseUser(KEY_FOLLOWED_BY);
    }

    public void setFollowedBy(ParseUser user) {
        put(KEY_FOLLOWED_BY, user);
    }
}
