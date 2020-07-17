package com.example.pitchr.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

@ParseClassName("Match")
public class Match extends ParseObject {

    public static final String KEY_TO = "to";
    public static final String KEY_FROM = "from";
    public static final String KEY_PERCENT = "percent";
    public static final String KEY_CREATED_AT = "createdAt";
    private static final String TAG = Match.class.getSimpleName();

    public ParseUser getTo() {
        return getParseUser(KEY_TO);
    }

    public ParseUser getFrom() {
        return getParseUser(KEY_FROM);
    }

    public double getPercent() {return getDouble(KEY_PERCENT); }
}
